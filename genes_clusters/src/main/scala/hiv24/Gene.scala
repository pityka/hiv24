package hiv24

case class Gene(
pejId: Int,
ensembleId: String,
name: String,
expressionMock:Map[Int,Double],
expressionHIV:Map[Int,Double],
cluster: Cluster,
revtr: Double,
intgr: Double,
late: Double )

case class Cluster(id:Int)

case class GeneSet(name:String, dataBase:String, set: Set[Gene]) 

case class EnrichmentResult(logP:Double,
qVal:Double,
countInBackground:Double,
expectedCount:Double,
countInCluster:Double,
sourceURL:String)

