package cool.graph

import sangria.execution._
import sangria.schema._
import spray.json.DefaultJsonProtocol._
import spray.json._
import com.typesafe.scalalogging.LazyLogging
import cool.graph.shared.logging.{LogData, LogKey}

import scala.collection.concurrent.TrieMap

class FieldMetricsMiddleware
    extends Middleware[RequestContextTrait]
    with MiddlewareAfterField[RequestContextTrait]
    with MiddlewareErrorField[RequestContextTrait]
    with LazyLogging {

  type QueryVal = TrieMap[String, List[Int]]
  type FieldVal = Long

  def beforeQuery(context: MiddlewareQueryContext[RequestContextTrait, _, _]) =
    TrieMap()
  def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[RequestContextTrait, _, _]) = {

    import TimingProtocol._

    val total  = queryVal.foldLeft(0)(_ + _._2.sum)
    val sumMap = queryVal.toMap.mapValues(_.sum) + ("__total" -> total)
//    logger.info(
//      LogData(
//        key = LogKey.RequestMetricsFields,
//        requestId = context.ctx.requestId,
//        clientId = Some(context.ctx.clientId),
//        projectId = context.ctx.projectId,
//        payload = Some(sumMap)
//      ).json)
  }

  def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[RequestContextTrait, _, _], ctx: Context[RequestContextTrait, _]) =
    continue(System.currentTimeMillis())

  def afterField(queryVal: QueryVal,
                 fieldVal: FieldVal,
                 value: Any,
                 mctx: MiddlewareQueryContext[RequestContextTrait, _, _],
                 ctx: Context[RequestContextTrait, _]) = {
    val key  = ctx.parentType.name + "." + ctx.field.name
    val list = queryVal.getOrElse(key, Nil)

    queryVal.update(key, list :+ (System.currentTimeMillis() - fieldVal).toInt)
    None
  }

  def fieldError(queryVal: QueryVal,
                 fieldVal: FieldVal,
                 error: Throwable,
                 mctx: MiddlewareQueryContext[RequestContextTrait, _, _],
                 ctx: Context[RequestContextTrait, _]) = {
    val key    = ctx.parentType.name + "." + ctx.field.name
    val list   = queryVal.getOrElse(key, Nil)
    val errors = queryVal.getOrElse("ERROR", Nil)

    queryVal.update(key, list :+ (System.currentTimeMillis() - fieldVal).toInt)
    queryVal.update("ERROR", errors :+ 1)
  }
}
