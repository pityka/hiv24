import scala.io.Source
import mybiotools.readTableAsMap
import mybiotools.readTable

package object hiv24 {


  def readNameExpressionClusterFiles(
    name: Source,
    expr: Source,
    clusterFile: Source,
    clusterNameFile:Source): Tuple2[List[Gene],Seq[Tuple2[Cluster,Set[Cluster]]]] = {
    val names = readTableAsMap[String]( name, key = 'Pej_ID, sep = "\\t+" )
    val expressions = readTableAsMap[String]( expr, key = 'Pej_ID, sep = "\\t+" )

    val clusterFileContent = readTableAsMap[String]( clusterFile, key = 'Pej_ID, sep = "\\t+" )

    val clusterNameFileContent = readTableAsMap[Int]( clusterNameFile, key = 'Cluster_ID, sep = "\\t+" )( _.toInt )

    val metaclusters : Seq[Tuple2[Cluster,Set[Cluster]]] = {
      clusterNameFileContent.values.map{ cl =>
        val mainCluster = Cluster(cl('Cluster_ID).toInt,cl('Cluster_Name))
        val subClusters = if (cl('Meta) == "-") Set[Cluster](mainCluster) else cl('Meta).split(":").map( id => Cluster(id.toInt,clusterNameFileContent(id.toInt)('Cluster_Name))).toSet
        (mainCluster,subClusters)
      }.toSeq
    }

    (names.map { geneName =>
      val id = geneName._1

      val ensemble = new String(geneName._2( 'ESN_ID ))
      val sym = new String(geneName._2( 'Symbol ))
      val expression: Map[Symbol, String] = expressions( id )

      val exprMapHIV = Map[Int, Double](
        2 -> expression( Symbol( "2H" ) ).toDouble,
        4 -> expression( Symbol( "4H" ) ).toDouble,
        6 -> expression( Symbol( "6H" ) ).toDouble,
        8 -> expression( Symbol( "8H" ) ).toDouble,
        10 -> expression( Symbol( "10H" ) ).toDouble,
        12 -> expression( Symbol( "12H" ) ).toDouble,
        14 -> expression( Symbol( "14H" ) ).toDouble,
        16 -> expression( Symbol( "16H" ) ).toDouble,
        18 -> expression( Symbol( "18H" ) ).toDouble,
        20 -> expression( Symbol( "20H" ) ).toDouble,
        22 -> expression( Symbol( "22H" ) ).toDouble,
        24 -> expression( Symbol( "24H" ) ).toDouble )

      val exprMapMock = Map[Int, Double](
        4 -> expression( Symbol( "4M" ) ).toDouble,
        6 -> expression( Symbol( "6M" ) ).toDouble,
        10 -> expression( Symbol( "10M" ) ).toDouble,
        12 -> expression( Symbol( "12M" ) ).toDouble,
        16 -> expression( Symbol( "16M" ) ).toDouble,
        18 -> expression( Symbol( "18M" ) ).toDouble,
        20 -> expression( Symbol( "20M" ) ).toDouble,
        22 -> expression( Symbol( "22M" ) ).toDouble )

      val cluster = clusterFileContent.get( id ) match {
        case Some(x) => Cluster( x.apply( 'Cluster_ID ).toInt, clusterNameFileContent(x.apply( 'Cluster_ID ).toInt)('Cluster_Name))
        case None => Cluster(220,"NonAssociatedWithProgression")
      } 

      val revTr = clusterFileContent.get( id.toString ).map( _.apply( Symbol( "RevTr." ) ).toDouble )
      val intgr = clusterFileContent.get( id.toString ).map( _.apply( Symbol( "Intgr." ) ).toDouble )
      val late = clusterFileContent.get( id.toString ).map( _.apply( Symbol( "Late" ) ).toDouble )

      Gene( id.toInt, ensemble, sym, exprMapMock, exprMapHIV, cluster, revTr, intgr, late )
    }.toList,metaclusters)
  }

  def readGeneSets( geneSetFile: Source, genes: Traversable[Gene], dbName: String ): List[GeneSet] = {
    val genesMap = genes.groupBy( _.pejId ).mapValues( _.head )
    geneSetFile.getLines.map { line =>
      val spl = mybiotools.fastSplitSeparator( line, '\t' )
      val name = new String(spl.head)//.replaceAll("\""," ").trim
      val set = spl.tail.map { pejid =>
        val pejidInt = pejid.toInt
        genesMap.get( pejidInt )
      }.filter( _.isDefined ).map( _.get ).toSet
      GeneSet( name, dbName, set )
    }.toList
  }

  def readEnrichmentFile( enrichmentFile: Source, clusterNames: Map[Int,String] ): Map[Tuple2[Cluster, String], EnrichmentResult] = {
    Map( readTable( enrichmentFile, header = true, sep = "\\t+" ).map { line =>
      val enrich = EnrichmentResult( logP = line( Symbol( "log10(Pval)" ) ) match {
        case x if x == "-Inf" => Double.MinValue
        case x if x == "Inf" => Double.MaxValue
        case x => x.toDouble
      },
        qVal = line( 'Qval ).toDouble,
        countInBackground = line( 'CountinBackground ).toDouble,
        expectedCount = line( 'ExpectedCount ).toDouble,
        countInCluster = line( 'CountinCluster ).toDouble,
        sourceURL = line( 'SourceUrl ) )
      ( Cluster(line( 'ClusterID ).toInt,clusterNames(line('ClusterID).toInt)), line( 'SetName ) ) -> enrich
    }.toSeq: _* )

  }

  import de.erichseifert.gral.data.DataSeries;
  import de.erichseifert.gral.data.DataTable;
  import de.erichseifert.gral.data.filters.Convolution;
  import de.erichseifert.gral.data.filters.Filter;
  import de.erichseifert.gral.data.filters.Kernel;
  import de.erichseifert.gral.data.filters.KernelUtils;
  import de.erichseifert.gral.data.filters.Median;
  import de.erichseifert.gral.plots.Plot;
  import de.erichseifert.gral.plots.XYPlot;
  import de.erichseifert.gral.plots.legends.Legend;
  import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
  import de.erichseifert.gral.ui.InteractivePanel;
  import de.erichseifert.gral.util.GraphicsUtils;
  import de.erichseifert.gral.util.Insets2D;
  import de.erichseifert.gral.util.Orientation;
  import de.erichseifert.gral.plots.points._
  import de.erichseifert.gral.plots.lines._
  import de.erichseifert.gral.plots.axes._
  import de.erichseifert.gral.plots.XYPlot.XYPlotArea2D
  import de.erichseifert.gral.graphics.AbstractDrawable
  import java.awt._;
  import scala.runtime.RichInt
  import scala.runtime.RichDouble
  import de.erichseifert.gral.graphics.DrawableContainer
  import de.erichseifert.gral.graphics.TableLayout

  def createTimeLinePlot( genes: Traversable[Gene], title: String = "" ): AbstractDrawable = {

    def createPlot( dat: Traversable[Map[Int, Double]], maxY: Double, minY: Double, title: String ): XYPlot = {

      val seriesHIV = dat.map { gene =>
        val ser = new DataTable( classOf[scala.runtime.RichInt], classOf[scala.runtime.RichDouble] )
        gene.toSeq.sortBy( _._1 ).foreach( tuple => ser.add( new RichInt( tuple._1 ), new RichDouble( tuple._2 ) ) )

        ser
      }

      val maxX = dat.map( x => x.map( _._1 ).toList ).flatten.max
      val minX = dat.map( x => x.map( _._1 ).toList ).flatten.min

      val avgHIV = dat.head.map( _._1 ).map { i =>
        val sum = dat.foldLeft( 0.0 )( ( x, y ) => x + y( i ) )
        i -> sum / dat.size.toDouble
      }

      val avgserHIV = new DataTable( classOf[scala.runtime.RichInt], classOf[scala.runtime.RichDouble] )
      avgHIV.toSeq.sortBy( _._1 ).foreach( tup => avgserHIV.add( new RichInt( tup._1 ), new RichDouble( tup._2 ) ) )

      val upperDashedLine = new DataTable( classOf[scala.runtime.RichInt], classOf[scala.runtime.RichDouble] )
      upperDashedLine.add( new RichInt( minX ), new RichDouble( 1.0 ) )
      upperDashedLine.add( new RichInt( maxX ), new RichDouble( 1.0 ) )
      val lowerDashedLine = new DataTable( classOf[scala.runtime.RichInt], classOf[scala.runtime.RichDouble] )
      lowerDashedLine.add( new RichInt( minX ), new RichDouble( -1.0 ) )
      lowerDashedLine.add( new RichInt( maxX ), new RichDouble( -1.0 ) )

      val color = new Color( 1.0f, 0.3f, 0.0f, 0.5f );

      val pointedLineRenderer = new DefaultLineRenderer2D();
      val dashedStroke = new BasicStroke( 1.5f, BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER, 10.0f, Array( 14f, 4f ), 0.0f );
      val pointedStroke = new BasicStroke( 1.0f, BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER, 10.0f, Array( 1.4f, 1.4f ), 0.0f );
      pointedLineRenderer.setSetting( LineRenderer.STROKE, pointedStroke );
      pointedLineRenderer.setSetting( LineRenderer.COLOR, color );

      val avgStroke = new BasicStroke( 5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f );
      val avgLineRenderer = new DefaultLineRenderer2D();
      val avgColor = new Color( 1.0f, 0.0f, 0.0f )
      avgLineRenderer.setSetting( LineRenderer.STROKE, avgStroke )
      avgLineRenderer.setSetting( LineRenderer.COLOR, avgColor )

      val dashedLineRenderer = new DefaultLineRenderer2D();
      val dashedColor = new Color( 0.3f, 0.3f, 1.0f )
      dashedLineRenderer.setSetting( LineRenderer.COLOR, dashedColor )
      dashedLineRenderer.setSetting( LineRenderer.STROKE, dashedStroke )

      val plotHIV = new XYPlot( ( seriesHIV.toSeq :+ avgserHIV :+ lowerDashedLine :+ upperDashedLine ): _* )
      seriesHIV.foreach { s =>
        plotHIV.setLineRenderer( s, pointedLineRenderer );
        plotHIV.setPointRenderer( s, null );

      }
      plotHIV.setLineRenderer( avgserHIV, avgLineRenderer )
      plotHIV.setPointRenderer( avgserHIV, null );

      plotHIV.setLineRenderer( upperDashedLine, dashedLineRenderer )
      plotHIV.setLineRenderer( lowerDashedLine, dashedLineRenderer )
      plotHIV.setPointRenderer( upperDashedLine, null );
      plotHIV.setPointRenderer( lowerDashedLine, null );

      val plotarea = plotHIV.getPlotArea
      plotarea.setSetting( XYPlotArea2D.GRID_MAJOR_X, true )
      plotarea.setSetting( XYPlotArea2D.GRID_MAJOR_Y, true )
      plotarea.setSetting( XYPlotArea2D.GRID_MINOR_X, false )
      plotarea.setSetting( XYPlotArea2D.GRID_MINOR_Y, false )

      val rendererY = plotHIV.getAxisRenderer( XYPlot.AXIS_Y );
      rendererY.setSetting( AxisRenderer.INTERSECTION, -Double.MaxValue );
      val rendererX = plotHIV.getAxisRenderer( XYPlot.AXIS_X );
      rendererX.setSetting( AxisRenderer.INTERSECTION, -Double.MaxValue );
      val xAxis = plotHIV.getAxis( XYPlot.AXIS_X )
      // xAxis.setRange( 0, 26 )
      val yAxis = plotHIV.getAxis( XYPlot.AXIS_Y )
      yAxis.setRange( -5, +5 )

      rendererX.setSetting( AxisRenderer.LABEL, "Time (hours)" )
      rendererY.setSetting( AxisRenderer.LABEL, "Fold change (log2)" )

      rendererX.setSetting( AxisRenderer.TICKS_MINOR, false )

      rendererY.setSetting( AxisRenderer.TICKS_MINOR, false )

      rendererX.setSetting( AxisRenderer.TICKS_SPACING, 2 )
      rendererY.setSetting( AxisRenderer.TICKS_SPACING, 2 )
      // rendererX.setSetting( AxisRenderer.TICK_LABELS_FORMAT, choiceFormat );
      // rendererX.setSetting(AxisRenderer.LABEL_DISTANCE,32)

      val titlefont = new Font( null, Font.PLAIN, 12 )

      plotHIV.setSetting( Plot.TITLE, title.grouped( 30 ).mkString( "\n" )+" : Gene expression pattern" );
      plotHIV.setSetting( Plot.TITLE_FONT, titlefont )

      plotHIV
    }

    val maxY = genes.map( x => x.expressionHIV.map( _._2 ).toList ::: x.expressionMock.map( _._2 ).toList ).flatten.max
    val minY = genes.map( x => x.expressionHIV.map( _._2 ).toList ::: x.expressionMock.map( _._2 ).toList ).flatten.min

    val hivplot = createPlot( genes.map( _.expressionHIV ), maxY, minY, "HIV" )
    val mockplot = createPlot( genes.map( _.expressionMock ), maxY, minY, "Mock" )
    mockplot.setAxis( XYPlot.AXIS_Y, hivplot.getAxis( XYPlot.AXIS_Y ) )

    val rendererYHIV = hivplot.getAxisRenderer( XYPlot.AXIS_Y )
    rendererYHIV.setSetting( AxisRenderer.TICK_LABELS, false )
    rendererYHIV.setSetting( AxisRenderer.SHAPE_VISIBLE, false )
    rendererYHIV.setSetting( AxisRenderer.TICKS, false )
    rendererYHIV.setSetting( AxisRenderer.LABEL, "" )
    rendererYHIV.setSetting( AxisRenderer.LABEL_DISTANCE, 0 )

    val insetsTop = 20.0
    val insetsLeft = 60.0
    val insetsBottom = 60.0
    val insetsRight = 20.0
    hivplot.setInsets( new Insets2D.Double(
      insetsTop, 5, insetsBottom, insetsRight ) );
    mockplot.setInsets( new Insets2D.Double(
      insetsTop, insetsLeft, insetsBottom, 0 ) );

    val boxplot = createBoxPlot( genes, title )

    val container = new DrawableContainer( new TableLayout( 3 ) );
    container.add( boxplot )
    container.add( mockplot );
    container.add( hivplot );

    container

  }

  import de.erichseifert.gral.plots.BoxPlot;
  import de.erichseifert.gral.plots.BoxPlot.BoxWhiskerRenderer;
  import de.erichseifert.gral.plots.XYPlot.XYNavigationDirection;
  import de.erichseifert.gral.plots.colors.LinearGradient;
  import de.erichseifert.gral.plots.colors.ScaledContinuousColorMapper;
  import de.erichseifert.gral.util.DataUtils;

  def createBoxPlot( genes: Traversable[Gene], title: String ): BoxPlot = {

    val limits = Array[Double]( 1.0, 2.0, 3.0 );
    val choices = Array( "RevTr.",
      "Intgr.",
      "Late" )
    val choiceFormat = new java.text.ChoiceFormat( limits, choices );

    val data = new DataTable( classOf[RichDouble], classOf[RichDouble], classOf[RichDouble] );
    genes.filter( _.revtr.isDefined ).foreach { gene =>

      data.add( new RichDouble( gene.revtr.get ), new RichDouble( gene.intgr.get ), new RichDouble( gene.late.get ) );
    }

    // Create new box-and-whisker plot
    val boxData = BoxPlot.createBoxData( data );
    val plot = new BoxPlot( boxData );

    // Format plot
    plot.setInsets( new Insets2D.Double( 20.0, 100.0, 60.0, 20.0 ) );

      val yAxis = plot.getAxis( XYPlot.AXIS_Y )
      yAxis.setRange( -1, +1 )

    // Format axes
    val rendererX = plot.getAxisRenderer( XYPlot.AXIS_X )
    rendererX.setSetting( AxisRenderer.TICK_LABELS_FORMAT, choiceFormat );
    val tickfont = new Font( null, Font.PLAIN, 12 )
    rendererX.setSetting( AxisRenderer.TICKS_FONT, tickfont )
    plot.setSetting( Plot.TITLE_FONT, tickfont )

    val rendererY = plot.getAxisRenderer( XYPlot.AXIS_Y );
    rendererY.setSetting( AxisRenderer.TICKS_FONT, tickfont )
    rendererY.setSetting( AxisRenderer.LABEL_FONT, tickfont )

    rendererY.setSetting( AxisRenderer.TICKS_MINOR, false )

    rendererY.setSetting( AxisRenderer.LABEL, "Relevance level" )

    // Format boxes
    val stroke = new BasicStroke( 1f );

    plot.getPointRenderer( boxData ).setSetting( BoxWhiskerRenderer.WHISKER_STROKE, stroke );
    plot.getPointRenderer( boxData ).setSetting( BoxWhiskerRenderer.BOX_BORDER, stroke );
    plot.getPointRenderer( boxData ).setSetting( BoxWhiskerRenderer.BOX_BACKGROUND, Color.white );
    plot.getPointRenderer( boxData ).setSetting( BoxWhiskerRenderer.BOX_COLOR, Color.blue );
    plot.getPointRenderer( boxData ).setSetting( BoxWhiskerRenderer.WHISKER_COLOR, Color.blue );
    plot.getPointRenderer( boxData ).setSetting( BoxWhiskerRenderer.BAR_CENTER_COLOR, Color.red );

    plot.getNavigator().setDirection( XYNavigationDirection.VERTICAL );

    plot.setSetting( Plot.TITLE, title.grouped( 30 ).mkString( "\n" )+" : "+genes.size+" genes" );

    plot
  }

}