package cool.graph.system.mutactions.internal.validations

import cool.graph.MutactionVerificationSuccess

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait MutactionVerificationUtil {
  type VerificationFuture = Future[Try[MutactionVerificationSuccess]]

  private val initialResult = Future.successful(Success(MutactionVerificationSuccess()))

  /**
    * Executes the verification functions in serial until:
    * a. all of them result in a success
    * OR
    * b. the first verification fails
    *
    * The return value is the result of the last verification function.
    */
  def serializeVerifications(verificationFns: List[() => VerificationFuture])(implicit ec: ExecutionContext): VerificationFuture = {
    serializeVerifications(verificationFns, initialResult)
  }

  private def serializeVerifications(verificationFns: List[() => VerificationFuture], lastResult: VerificationFuture)(
      implicit ec: ExecutionContext): VerificationFuture = {
    verificationFns match {
      case Nil =>
        lastResult
      case firstVerificationFn :: remainingVerifications =>
        firstVerificationFn().flatMap {
          case result @ Success(_) =>
            serializeVerifications(remainingVerifications, Future.successful(result))
          case result @ Failure(_) =>
            Future.successful(result)
        }
    }
  }
}
