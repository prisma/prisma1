package cool.graph.javascriptEngine

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class JavascriptExecutorSpec extends FlatSpec with Matchers {
  "engine" should "execute simple script" in {

    val before = System.currentTimeMillis()

    JavascriptExecutor.execute("""
        |console.log(42)
        |
        |console.log(43 + 2 + "lalala")
      """.stripMargin).futureValue(Timeout(Duration.Inf)) should be(Result("42\n45lalala\n", ""))

    println("1 (initial): " + (System.currentTimeMillis() - before))

    val before2 = System.currentTimeMillis()

    JavascriptExecutor.execute("""
                                 |console.log(42)
                                 |
                                 |console.log(43 + 2 + "lalala")
                               """.stripMargin).futureValue(Timeout(Duration.Inf)) should be(Result("42\n45lalala\n", ""))

    println("1 (warm): " + (System.currentTimeMillis() - before2))

    val before3 = System.currentTimeMillis()

    (1 to 10).foreach(_ => JavascriptExecutor.execute("""
                                                        |console.log(42)
                                                        |
                                                        |console.log(43 + 2 + "lalala")
                                                      """.stripMargin).futureValue(Timeout(Duration.Inf)) should be(Result("42\n45lalala\n", "")))

    println("10 (seq): " + (System.currentTimeMillis() - before3))

    val before4 = System.currentTimeMillis()

    Future.sequence((1 to 10).map(_ => JavascriptExecutor.execute("""
                                     |console.log(42)
                                     |
                                     |console.log(43 + 2 + "lalala")
                                   """.stripMargin))).futureValue(Timeout(Duration.Inf))

    println("10 (par): " + (System.currentTimeMillis() - before4))

    val before5 = System.currentTimeMillis()

    Future.sequence((1 to 100).map(_ => JavascriptExecutor.execute("""
                                                                    |console.log(42)
                                                                    |
                                                                    |console.log(43 + 2 + "lalala")
                                                                  """.stripMargin))).futureValue(Timeout(Duration.Inf))

    println("100 (par): " + (System.currentTimeMillis() - before5))

    val before6 = System.currentTimeMillis()

    Future
      .sequence((1 to 1000).map(_ => JavascriptExecutor.execute("""
                                                                     |console.log(42)
                                                                     |
                                                                     |console.log(43 + 2 + "lalala")
                                                                   """.stripMargin)))
      .futureValue(Timeout(Duration.Inf))

    println("1000 (par): " + (System.currentTimeMillis() - before6))

  }
}
