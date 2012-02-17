package controllers

import play.api._
import play.api.mvc._
import models.GeneData
import hiv24._
import de.erichseifert.gral.io.plots.DrawableWriterFactory
import java.io.ByteArrayOutputStream
import play.api.libs.concurrent.{ Promise, Akka }
import javax.xml.bind.DatatypeConverter
import play.api.Play.current
import play.api.data.Forms._
import play.api.data._

object Application extends Controller {

  
  val clusterSelectForm = Form(
    mapping(
      "clusterID" -> number )(
        ( id ) => Cluster( id ) )(
          ( cl: Cluster ) => Some( cl.id ) ) )

  val geneInputForm = Form(
    tuple(
      "idList" -> text,
      "format" -> optional( text ) ) )

  val geneSetQueryForm = Form(
    "keywords" -> optional( text ) )

  val clusters = GeneData.genes.map( _.cluster ).distinct.sortBy( _.id )

  def about = Action{
    Ok(views.html.about())
  }

  def index = Action {
    Ok( views.html.index( clusters, clusterSelectForm, geneInputForm ) )
  }

  // GET /geneset
  def showAllGeneSets = Action {
    Ok( views.html.geneSetList( GeneData.predefinedGeneSets, geneSetQueryForm ) )
  }

  // GET /listgenesets/:text
  def listGeneSets( text: String ) = Action {
    val gs = geneSetsFromText( text )
    Ok( views.html.geneSetList( gs, geneSetQueryForm ) )
  }

  // POST /listgenesets/
  def queryGeneSets = Action { implicit request =>
    geneSetQueryForm.bindFromRequest.fold(
      errors => BadRequest,
      keywordstextOption => {
        keywordstextOption match {
          case None => Redirect( routes.Application.showAllGeneSets )
          case Some( keywordstext ) => {
            val keywords = mybiotools.fastSplitSetSeparator( keywordstext, SeparatorCharacters ).distinct.map( _.toUpperCase )

            val selectedGeneSets = GeneData.predefinedGeneSets.filter( x => keywords.exists( y => x._1.indexOf( y ) != -1 ) )

            Ok( views.html.geneSetList( selectedGeneSets, geneSetQueryForm.bindFromRequest ) )
          }
        }

      } )
  }

  // POST /cluster/
  def showClusterFromForm = Action { implicit request =>
    clusterSelectForm.bindFromRequest.fold(
      errors => BadRequest,
      cluster => {
        showClusterHelper( cluster )
      } )
  }

  // GET /cluster/:ID
  def showCluster( id: Int ) = Action {
    val cluster = Cluster( id )
    showClusterHelper( cluster )
  }

  // POST /genes
  def listGenesFromForm = Action { implicit request =>
    geneInputForm.bindFromRequest.fold(
      errors => BadRequest,
      tuple => {
        if ( tuple._2 != Some( "csv" ) ) showGenesHelper( geneSetFromString( tuple._1 ) )
        else serveCSV( geneSetFromString( tuple._1 ) )
      } )
  }

  // GET /genes/:list
  def listGenes( list: String ) = Action { implicit request =>
    val genes = geneSetFromString( list )
    showGenesHelper( genes )
  }
  def showGeneSet( name: String ) = Action {
    val geneSetOp = GeneData.predefinedGeneSets.get( name )
    geneSetOp match {
      case None => NotFound( views.html.emptyPage() )
      case Some( geneSet ) => {
        val genes = geneSet.set
        val promiseOfImage: Promise[String] = play.api.cache.Cache.get( "predef"+name ) match {
          case Some( x ) => Promise.pure( x.asInstanceOf[String] )
          case None => getImagePromise( genes, name.toString )
        }

        val enrichmentResults: Traversable[Tuple2[Cluster, EnrichmentResult]] = GeneData.enrichmentTests.filter( tup => tup._1._2 == geneSet.name ).map { tup =>
          val cl = clusters.find( _.id == tup._1._1 )
          cl.map( x => x -> tup._2 )
        }.filter( _.isDefined ).map( _.get )

        Async {
          promiseOfImage.orTimeout( "Oops", 120000 ).map { either =>
            either.fold(
              image => {
                play.api.cache.Cache.set( "predef"+name, image, CacheExpiryTime )
                Ok( views.html.showGenesPage( genes, image, Nil, List( ( geneSet, enrichmentResults ) ), bindGenesToForm( genes ) ) )
              },
              timeout => InternalServerError( "timeout" ) )
          }
        }
      }
    }
  }

  private def geneSetFromString( text: String ): Set[Gene] = {
    val ids = mybiotools.fastSplitSetSeparator( text, SeparatorCharacters ).distinct.map( _.toUpperCase )
    ids.map( x => GeneData.genes.find( y => y.ensembleId == x || y.name == x ) ).filter( _.isDefined ).map( _.get ).toSet
  }

  private def getImagePromise( genes: Traversable[Gene], name: String ): Promise[String] = {
    Akka.future {
      val factory = DrawableWriterFactory.getInstance();
      val writer = factory.get( "image/png" );
      val plot = createTimeLinePlot( genes, name )
      val bs = new ByteArrayOutputStream()
      writer.write( plot, bs, 900, 300 );

      DatatypeConverter.printBase64Binary( bs.toByteArray )
    }
  }

  private def showGenesHelper( genes: Traversable[Gene] )( implicit request: Request[_] ): Result = {
    if ( genes.size > 0 ) {
      val promiseOfImage = getImagePromise( genes, "Custom geneset" )
      Async {
        promiseOfImage.orTimeout( "Oops", 120000 ).map { either =>
          either.fold(
            image => Ok( views.html.showGenesPage( genes, image, Nil, Nil, bindGenesToForm( genes ) ) ),
            timeout => InternalServerError( "timeout" ) )
        }
      }
    } else {
      NotFound( views.html.emptyPage() )
    }
  }

  private def serveCSV( genes: Traversable[Gene] )( implicit request: Request[_] ): Result = {
    if ( genes.size == 0 ) NotFound else {
      SimpleResult(
        header = ResponseHeader( 200, Map( "Content-Disposition" -> "attachment; filename=table.txt" ) ),
        body = play.api.libs.iteratee.Enumerator( renderCSV( genes ) ) )
    }
  }

  private def showClusterHelper( cluster: Cluster ): Result = {
    val genes = GeneData.genes.filter( _.cluster == cluster )
    if ( genes.size > 0 ) {
      val promiseOfImage: Promise[String] = play.api.cache.Cache.get( cluster.id.toString ) match {
        case Some( x ) => Promise.pure( x.asInstanceOf[String] )
        case None => getImagePromise( genes, "C"+cluster.id.toString )
      }

      val enrichmentResults: Traversable[Tuple2[GeneSet, EnrichmentResult]] = GeneData.enrichmentTests.filter( tup => tup._1._1 == cluster.id ).map { tup =>
        val gset = GeneData.predefinedGeneSets.get( tup._1._2 )
        gset.map( x => x -> tup._2 )
      }.filter( _.isDefined ).map( _.get )

      Async {
        promiseOfImage.orTimeout( "Oops", 120000 ).map { either =>
          either.fold(
            image => {
              play.api.cache.Cache.set( cluster.id.toString, image )
              Ok( views.html.showGenesPage( genes, image, List( ( cluster, enrichmentResults ) ), Nil, bindGenesToForm( genes ) ) )
            },
            timeout => InternalServerError( "timeout" ) )
        }
      }
    } else {
      NotFound( views.html.emptyPage() )
    }
  }

  private def renderCSV( genes: Traversable[Gene] ) = genes.map( g => List( g.ensembleId, g.name, g.cluster.id ).mkString( "," ) ).mkString( "\n" )

  private def bindGenesToForm( genes: Traversable[Gene] ) = geneInputForm.bind( Map( "idList" -> genes.map( _.ensembleId ).mkString( ":" ), "format" -> "csv" ) )

  private def geneSetsFromText( t: String ) = {
    val ids = mybiotools.fastSplitSetSeparator( t, SeparatorCharacters ).distinct.map( _.toUpperCase )
    GeneData.predefinedGeneSets.filter( x => ids.contains( x._1 ) )
  }

  private val SeparatorCharacters = Set( ':', ',', ';', '\n', '\r', 13.toByte.toChar, 10.toByte.toChar, ' ' )

  private val CacheExpiryTime = 24*60*60 // 1 day, in seconds


}

