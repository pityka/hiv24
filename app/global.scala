import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import com.newrelic.api.agent.NewRelic
import scala.collection.JavaConversions._

trait GlobalCommon extends GlobalSettings {

  override def onStart( app: Application ) {
    Logger.info( "Application has started" )
    Logger.info( "Genes loaded: "+models.GeneData.genes.size )
    Logger.info( "Predefined genesets loaded: "+models.GeneData.predefinedGeneSets.size )
  }

  override def onStop( app: Application ) {
    Logger.info( "Application shutdown..." )
  }

}

object GlobalDev extends GlobalCommon

object GlobalProd extends GlobalCommon {

  override def onError( request: RequestHeader, ex: Throwable ) = {
    NewRelic.noticeError(ex, request.queryString.mapValues(_.toString))
    InternalServerError
  }

  override def onHandlerNotFound( request: RequestHeader ) = {
    NotFound
  }

  override def onBadRequest( request: RequestHeader, error: String ) = {
    BadRequest( "Don't try to hack the URI!" )
  }

}