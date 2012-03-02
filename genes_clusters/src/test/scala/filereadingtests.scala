import org.scalatest.FunSuite



import hiv24._

import scala.io.Source

class FileReadingTestSuite extends FunSuite {

  test( "simple" ) {

    val clusterstxt = """|Pej_ID	Cluster_ID	RevTr.	Intgr.	Late
					|1	8	5.7031	1.648	7.773
					|2	6	-1.3568	1.6485	2.3129""".stripMargin
    val exprtxt = """|Pej_ID		4M	6M	10M	12M	16M	18M	20M	22M	2H	4H	6H	8H	10H	12H	14H	16H	18H	20H	22H	24H
					|1	0	0	0	0	0	0	0	0	10.239	5.7814	7.8534	8.561	5.3124	6.8683	6.9917	1.2749	7.8892	10.018	10.737	8.1218
					|2	0	0	0	0	0	0	0	0	2.5042	1	0	0	0	0	1.36	1.801	2.6093	2.5543	2.3204	2.9642
""".stripMargin

    val namestxt = """|Pej_ID	ESN_ID	Symbol
|1	5LTR	5LTR
|2	ASP	ASP
""".stripMargin

    val genesettxt = """|set1	1	2	3
|set2	2	3	1""".stripMargin

    val genes = readNameExpressionClusterFiles(
      name = Source.fromString( namestxt ),
      expr = Source.fromString( exprtxt ),
      clusterFile = Source.fromString( clusterstxt ) )

    val geneSets = readGeneSets(Source.fromString(genesettxt),genes,"dbname")

      val expected = List(
	      Gene(1,"5LTR","5LTR",Map(
              4 -> 0,
              6 -> 0,
              10 -> 0,
              12 -> 0,
              16 -> 0,
              18 -> 0,
              20 -> 0,
              22 -> 0),
          Map(
        2 ->    10.239,
        4 ->    5.7814,
        6 ->    7.8534,
        8 ->    8.561,
        10 ->   5.3124,
        12 ->   6.8683,
        14 ->   6.9917,
        16 ->   1.2749,
        18 ->   7.8892,
        20 ->   10.018,
        22 ->   10.737,
        24 -> 8.1218),Some(Cluster(8)),Some(5.7031),Some(1.648),Some(7.773)),
	      Gene(2,"ASP","ASP",Map(
              4 -> 0,
              6 -> 0,
              10 -> 0,
              12 -> 0,
              16 -> 0,
              18 -> 0,
              20 -> 0,
              22 -> 0),Map(
        2 ->    2.5042,
        4 ->    1,
        6 ->    0,
        8 ->    0,
        10 ->   0,
        12 ->   0,
        14 ->   1.36,
        16 ->   1.801,
        18 ->   2.6093,
        20 ->   2.5543,
        22 ->   2.3204,
        24 -> 2.9642),Some(Cluster(6)),Some(-1.3568),Some(1.6485),Some(2.3129)))

        val expectedGeneSets = List(GeneSet("set1","dbname",expected.toSet),
        GeneSet("set2","dbname",expected.toSet))

        expect(expected)(genes)

        expect(expectedGeneSets)(geneSets)

  }

  test("read enrichment file") {
      val txt = "|DataBase\tClusterID\tSetName\tlog10(Pval)\tQval\tCountinBackground\tExpectedCount\tCountinCluster\tSourceUrl\nJagerlist.gmt GeneSets\t3\tJagersList\t-2.8\t0.00164\t381\t77.19\t100\tJagersList\nJagerlist.gmt GeneSets\t12\tJagersList\t-2.1\t0.00720\t381\t15.56\t25\tJagersList"

    val r = readEnrichmentFile(Source.fromString(txt))

    val expected = Map(
        (3,"JagersList") -> EnrichmentResult(-2.8,0.00164,381,77.19,100,"JagersList"),
        (12,"JagersList") -> EnrichmentResult(-2.1,0.00720,381,15.56,25,"JagersList"))
    
    expect(expected)(r)
  }

}