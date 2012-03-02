package hiv24

case class Gene(
pejId: Int,
ensembleId: String,
name: String,
expressionMock:Map[Int,Double],
expressionHIV:Map[Int,Double],
cluster: Option[Cluster],
revtr: Option[Double],
intgr: Option[Double],
late: Option[Double] )

case class Cluster(id:Int)

case class GeneSet(name:String, dataBase:String, set: Set[Gene]) 

case class EnrichmentResult(logP:Double,
qVal:Double,
countInBackground:Double,
expectedCount:Double,
countInCluster:Double,
sourceURL:String)

