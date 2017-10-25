package cool.graph.localfaas.actors

import akka.actor.Actor
import better.files.File
import cool.graph.localfaas.actors.MappingActor.{GetHandler, HandlerMap, SaveMapping}
import play.api.libs.json._

import scala.collection.mutable

object MappingActor {
  case class SaveMapping(projectId: String, functionName: String, handlerPath: String)
  case class GetHandler(projectId: String, functionName: String)

  type HandlerMap = mutable.HashMap[String, mutable.HashMap[String, String]]
}

case class MappingActor(handlerFile: File) extends Actor {
  import Conversions._

  // projectId -> functionName -> handlerPath
  val handlers = loadHandlers

  // load handlers on creation
  def loadHandlers: HandlerMap = {
    val content = handlerFile.contentAsString

    if (handlerFile.contentAsString.isEmpty) {
      new HandlerMap
    } else {
      Json.parse(content).validate[HandlerMap] match {
        case JsSuccess(result, _) => println("Using mapping from file."); result
        case JsError(_)           => println("Unable to parse handler map from file, using empty map."); new HandlerMap
      }
    }
  }

  def flush(): Unit = {
    val compactJson: String = Json.stringify(Json.toJson(handlers))
    handlerFile.overwrite(compactJson)
  }

  override def receive: Receive = {
    case GetHandler(pid, fnName) =>
      val projectHandlerMap = handlers.getOrElse(pid, new mutable.HashMap[String, String]())
      sender ! projectHandlerMap.getOrElse(fnName, "")

    case SaveMapping(pid, fnName, handlerPath) =>
      val projectHandlerMap = handlers.getOrElseUpdate(pid, new mutable.HashMap[String, String]())
      projectHandlerMap += fnName -> handlerPath
      flush()
  }
}
