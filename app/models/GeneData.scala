package models

import play.api.Play.current
import scala.io.Source
import hiv24._
import java.io.File

object GeneData {

  private val nameFile = Source.fromURL(getClass.getResource( current.configuration.getString( "hiv24.geneNamesFile" ).get ))

  private val expressionsFile = Source.fromURL(getClass.getResource( current.configuration.getString( "hiv24.geneExpressionsFile" ).get ))

  private val clustersFile = Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.clustersFile" ).get ))

  private val enrichmentTestsFile = Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.enrichmentTestsFile" ).get ))

  private val geneSetFiles = play.api.Configuration.unapply( current.configuration ).get.getStringList( "hiv24.predefinedGeneSets" ).toArray.toList.asInstanceOf[List[String]].map( x => x -> Source.fromURL(getClass.getResource( x) ) )

  val genes = readNameExpressionClusterFiles( nameFile, expressionsFile, clustersFile )

  val predefinedGeneSets: Map[String, GeneSet] = geneSetFiles.map { x =>
    val geneSetFile = x._2
    val dbname = new File( x._1 ).getName
    readGeneSets( geneSetFile, genes, dbname )
  }.flatten.groupBy( _.name ).mapValues( _.head )

  val enrichmentTests = readEnrichmentFile( enrichmentTestsFile )

}