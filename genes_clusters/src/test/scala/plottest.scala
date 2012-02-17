import org.scalatest.FunSuite

import hiv24._
import java.io.FileOutputStream
import scala.io.Source

class PlotTestSuite extends FunSuite {

  test( "timeplot and boxplot" ) {
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

    val genes = readNameExpressionClusterFiles(
      name = Source.fromString( namestxt ),
      expr = Source.fromString( exprtxt ),
      clusterFile = Source.fromString( clusterstxt ) )

    val outfile = getClass.getResource( "/" ).getPath+"/timeline.pdf"

    val outfile2 = getClass.getResource( "/" ).getPath+"/boxplot.pdf"

    // val im = createTimeLinePlot(genes).createBufferedImage( 914, 323 )
    // javax.imageio.ImageIO.write( im, "png", new java.io.File( outfile ) )
    import de.erichseifert.gral.io.plots._
    val factory = DrawableWriterFactory.getInstance();
    val writer = factory.get( "application/pdf" );
    val plot = createTimeLinePlot(genes,"Test set of genes")
    writer.write( plot, new FileOutputStream( outfile ),1000,300 );

    val boxplot = createBoxPlot(genes,"Test set of genes")
writer.write( boxplot, new FileOutputStream( outfile2 ),1000,1000 );
  }

}