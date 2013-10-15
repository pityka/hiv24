package model

import scala.concurrent._
import scala.concurrent.duration._
import play.libs.Akka
import play.api.libs.concurrent.Execution.Implicits._

object TimeoutFuture {
  def apply[A](timeout: FiniteDuration)(f: Future[A]): Future[A] = {

    val prom = promise[A]

    // timeout logic
    Akka.system.scheduler.scheduleOnce(timeout) {
      prom tryFailure new java.util.concurrent.TimeoutException
    }

    // business logic
    f.map(x => prom success x)

    prom.future
  }
}