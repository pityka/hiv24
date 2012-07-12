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

  private val enrichmentTestsFiles = play.api.Configuration.unapply( current.configuration ).get.getStringList( "hiv24.enrichmentTestsFiles" ).toArray.toList.asInstanceOf[List[String]].map{ x => 
    play.api.Logger.info("Reading "+x)
    Source.fromURL(getClass.getResource( x) ) 
    } 

  private val geneSetFiles = play.api.Configuration.unapply( current.configuration ).get.getStringList( "hiv24.predefinedGeneSets" ).toArray.toList.asInstanceOf[List[String]].map{ x => 
    play.api.Logger.info("Reading "+x)
     x -> Source.fromURL(getClass.getResource( x) ) 
    } 

  private val clusterNameFile = Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.clusterNameFile" ).get ))

  private val tmptup = readNameExpressionClusterFiles( nameFile, expressionsFile, clustersFile, clusterNameFile )

  val genes = tmptup._1

  val metaClusters = tmptup._2

  val genesByCluster : Map[Cluster,Set[Gene]] = metaClusters.map{ t =>
    t._1 -> t._2.map{ cluster =>
      genes.filter(_.cluster == cluster).toSet
    }.reduce(_ ++ _)
  }.toMap

  val predefinedGeneSets: Map[String, GeneSet] = geneSetFiles.map { x =>
    val geneSetFile = x._2
    val dbname = new File( x._1 ).getName
    readGeneSets( geneSetFile, genes, dbname )
  }.flatten.groupBy( _.name ).mapValues( _.head )

  val enrichmentTests: Map[Tuple2[Cluster, String], EnrichmentResult] = enrichmentTestsFiles.map(readEnrichmentFile).reduce(_ ++ _)

  private val clusterNamesAndOrder : Map[Int,Tuple2[String,Int]] = 
    readTableAsMap[Int]( 
      Source.fromURL( getClass.getResource(current.configuration.getString( "hiv24.clusterNameFile" ).get )),
      key = 'Cluster_ID,
      sep = "\\t+" )( _.toInt)
        .mapValues(x => (x('Cluster_Name),x('DisplayOrder).toInt))

  val clusterNames = clusterNamesAndOrder.mapValues(_._1)
  val clusterDisplayOrder = clusterNamesAndOrder.mapValues(_._2)


}