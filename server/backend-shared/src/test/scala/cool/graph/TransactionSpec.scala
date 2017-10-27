package cool.graph

import cool.graph.client.database.DataResolver
import cool.graph.shared.database.Databases
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.util.{Failure, Random, Success, Try}

class TransactionSpec extends FlatSpec with Matchers {
  import cool.graph.util.AwaitUtils._

  import scala.language.reflectiveCalls

  val dataResolver: DataResolver = null // we don't need it for those tests

  "Transaction.verify" should "return a success if it contains no Mutactions at all" in {
    val transaction = Transaction(List.empty, dataResolver)
    val result      = await(transaction.verify())
    result should be(Success(MutactionVerificationSuccess()))
  }

  "Transaction.verify" should "return a success if all Mutactions succeed" in {
    val mutactions  = List(successfulMutaction, successfulMutaction, successfulMutaction)
    val transaction = Transaction(mutactions, dataResolver)
    val result      = await(transaction.verify())
    result should be(Success(MutactionVerificationSuccess()))
  }

  "Transaction.verify" should "return the failure of the first failed Mutaction" in {
    for (i <- 1 to 10) {
      val failedMutactions =
        Random.shuffle(List(failedMutaction("error 1"), failedMutaction("error 2"), failedMutaction("error 3")))
      val mutactions  = List(successfulMutaction) ++ failedMutactions
      val transaction = Transaction(mutactions, dataResolver)
      val result      = await(transaction.verify())
      result.isFailure should be(true)
      result.failed.get.getMessage should be(failedMutactions.head.errorMessage)
    }
  }

  def failedMutaction(errorMsg: String) = {
    new ClientSqlMutaction {
      val errorMessage = errorMsg

      override def execute = ???

      override def verify(): Future[Try[MutactionVerificationSuccess]] = {
        Future.successful(Failure(new Exception(errorMessage)))
      }
    }
  }

  def successfulMutaction = {
    new ClientSqlMutaction {
      override def execute = ???

      override def verify(): Future[Try[MutactionVerificationSuccess]] = {
        Future.successful(Success(MutactionVerificationSuccess()))
      }
    }
  }
}
