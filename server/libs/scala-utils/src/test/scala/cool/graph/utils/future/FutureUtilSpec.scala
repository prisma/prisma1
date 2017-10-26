package cool.graph.utils.future

import org.scalatest.{Matchers, WordSpec}
import cool.graph.utils.future.FutureUtils._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Millis, Seconds, Span}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureUtilSpec extends WordSpec with Matchers {
  val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  "runSequentially" should {
    "run all given futures in sequence" in {

      val testList = List[() => Future[Long]](
        () => { Thread.sleep(500); Future.successful(System.currentTimeMillis()) },
        () => { Thread.sleep(250); Future.successful(System.currentTimeMillis()) },
        () => { Thread.sleep(100); Future.successful(System.currentTimeMillis()) }
      )

      val values: Seq[Long] = testList.runSequentially.futureValue(patienceConfig)
      (values, values.tail).zipped.forall((a, b) => a < b)
    }
  }

  "andThenFuture" should {

    "Should work correctly in error and success cases" in {
      val f1 = Future.successful(100)
      val f2 = Future.failed(new Exception("This is a test"))

      whenReady(
        f1.andThenFuture(
          handleSuccess = x => Future.successful("something"),
          handleFailure = e => Future.successful("another something")
        )) { res =>
        res should be(100)
      }

      whenReady(
        f2.andThenFuture(
            handleSuccess = (x: Int) => Future.successful("something"),
            handleFailure = e => Future.successful("another something")
          )
          .failed) { res =>
        res shouldBe a[Exception]
      }
    }
  }

}
