import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.concurrent.Execution.Implicits._

import com.newrelic.api.agent.NewRelic
import scala.collection.JavaConversions._

trait GlobalCommon extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    Logger.info("Genes loaded: " + models.GeneData.genes.size)
    Logger.info("Predefined genesets loaded: " + models.GeneData.predefinedGeneSets.size)
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}

object GlobalDev extends GlobalCommon

object GlobalProd extends GlobalCommon {

  override def onError(request: RequestHeader, ex: Throwable) = scala.concurrent.Future {
    NewRelic.noticeError(ex, request.queryString.mapValues(_.toString))
    InternalServerError
  }

  override def onHandlerNotFound(request: RequestHeader) = scala.concurrent.Future {
    NotFound
  }

  override def onBadRequest(request: RequestHeader, error: String) = scala.concurrent.Future {
    BadRequest("")
  }

}