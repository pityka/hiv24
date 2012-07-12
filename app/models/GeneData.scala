package models

import play.api.Play.current
import scala.io.Source
import hiv24._
import java.io.File
import mybiotools._

object GeneData {

  private val nameFile = Source.fromURL(getClass.getResource( current.configuration.getString( "hiv24.geneNamesFile" ).get ))

  private val expressionsFile = Source.fromURL(getClass.getResource( current.configuration.getString( "hiv24.geneExpressionsFile" ).get ))

  private val clustersFile = Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.clustersFile" ).get ))

  private val enrichmentTestsFile = Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.enrichmentTestsFile" ).get ))

  private val geneSetFiles = play.api.Configuration.unapply( current.configuration ).get.getStringList( "hiv24.predefinedGeneSets" ).toArray.toList.asInstanceOf[List[String]].map{ x => 
    play.api.Logger.info("Reading "+x)
     x -> Source.fromURL(getClass.getResource( x) ) 
    } 

  private val clusterNameFile = Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.clusterNameFile" ).get ))

  val genes = readNameExpressionClusterFiles( nameFile, expressionsFile, clustersFile, clusterNameFile )

  val predefinedGeneSets: Map[String, GeneSet] = geneSetFiles.map { x =>
    val geneSetFile = x._2
    val dbname = new File( x._1 ).getName
    readGeneSets( geneSetFile, genes, dbname )
  }.flatten.groupBy( _.name ).mapValues( _.head )

  val enrichmentTests = readEnrichmentFile( enrichmentTestsFile )

  val clusterNames = readTableAsMap[Int]( Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.clusterNameFile" ).get )), key = 'Cluster_ID, sep = "\\t+" )( _.toInt).mapValues(x => x('Cluster_Name))


}