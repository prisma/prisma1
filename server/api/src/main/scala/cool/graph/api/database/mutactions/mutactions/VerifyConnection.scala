package cool.graph.api.database.mutactions.mutactions

import java.sql.SQLException

import cool.graph.api.database._
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.NodeSelector
import cool.graph.api.schema.APIErrors
import cool.graph.gc_values.{NullGCValue, _}
import cool.graph.shared.models.{Project, Relation}

import scala.concurrent.Future

case class VerifyConnection(project: Project, relation: Relation, outerWhere: NodeSelector, innerWhere: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.connectionFailureTrigger(project, relation, outerWhere, innerWhere)))
  }

  override def handleErrors = {Some({ case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodesNotConnectedError(relation.name, outerWhere, innerWhere)})}

  private def dateTimeFromISO8601(v: Any) = {
    val string = v.toString
    //"2017-12-05T12:34:23.000Z" to "2017-12-05T12:34:23.000" which MySQL will accept
    string.replace("Z", "")
  }

  def causedByThisMutaction(cause: String) = {

    val parameterString = innerWhere.fieldValue match {
      case StringGCValue(x) => s"parameters ['$x',"
      case IntGCValue(x) => s"parameters [$x,"
      case FloatGCValue(x) => s"parameters [$x,"
      case BooleanGCValue(false) => s"parameters [0,"
      case BooleanGCValue(true) => s"parameters [1,"
      case GraphQLIdGCValue(x) => s"parameters ['$x',"
      case EnumGCValue(x) => s"parameters ['$x',"
      case DateTimeGCValue(x) => s"parameters ['${dateTimeFromISO8601(x)}',"
      case JsonGCValue(x) => s"parameters ['$x',"
      case ListGCValue(_) => sys.error("Not an acceptable Where")
      case RootGCValue(_) => sys.error("Not an acceptable Where")
      case NullGCValue => sys.error("Not an acceptable Where")
    }

  val relationString = s"`${relation.id}` where `${relation.sideOf(innerWhere.model)}` ="

  cause.contains(relationString) && cause.contains(parameterString)
  }
}