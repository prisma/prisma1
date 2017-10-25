package cool.graph.javascriptEngine

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.jse.Engine.JsExecutionResult
import cool.graph.cuid.Cuid
import cool.graph.javascriptEngine.lib.{Engine, Trireme}

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object JavascriptExecutor {
  implicit val system  = ActorSystem("jse-system")
  implicit val timeout = Timeout(5.seconds)

  def execute(program: String): Future[Result] = {

    // note: probably not the way to do this ...
    val engine = system.actorOf(Trireme.props(), s"engine-${Cuid.createCuid()}")

    (engine ? Engine.ExecuteJs(program, immutable.Seq(), timeout.duration))
      .mapTo[JsExecutionResult]
      .map(res => Result(result = res.output.utf8String, error = res.error.utf8String))
  }

  def executeFunction(program: String): Future[Map[String, Any]] = {
    import spray.json._
    import DefaultJsonProtocol._

    // todo: copied from shared.Utils. Extract to own module
    implicit object AnyJsonFormat extends JsonFormat[Any] {
      def write(x: Any) = x match {
        case m: Map[_, _] =>
          JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
        case l: List[Any] => JsArray(l.map(write).toVector)
        case n: Int       => JsNumber(n)
        case n: Long      => JsNumber(n)
        case s: String    => JsString(s)
        case true         => JsTrue
        case false        => JsFalse
        case v: JsValue   => v
        case null         => JsNull
        case r            => JsString(r.toString)
      }

      def read(x: JsValue): Any = {
        x match {
          case l: JsArray   => l.elements.map(read).toList
          case m: JsObject  => m.fields.mapValues(write)
          case s: JsString  => s.value
          case n: JsNumber  => n.value
          case b: JsBoolean => b.value
          case JsNull       => null
          case _            => sys.error("implement all scalar types!")
        }
      }
    }

    execute(program).map(res => {

      if (!res.error.trim.isEmpty) {
        throw new JsExecutionError(res.error)
      }

      res.result.parseJson.asJsObject.convertTo[Map[String, Any]]
    })
  }

}

case class Result(result: String, error: String)

class JsExecutionError(message: String) extends Error
