package mt.gamify

import java.util.concurrent.{CompletableFuture, Future => JavaFuture}
import scala.concurrent.{Future => ScalaFuture}
import scala.jdk.FutureConverters._

package object kafka {

  implicit class FutureOps[T](future: JavaFuture[T]) extends AnyRef {

    def toScala: ScalaFuture[T] = {
      val completableFuture: CompletableFuture[T] = FutureUtils.makeCompletableFuture(future)
      completableFuture.asScala
    }
  }
}
