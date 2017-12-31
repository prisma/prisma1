package cool.graph.api.database.mutactions.mutactions

import java.sql.SQLException

import cool.graph.api.database._
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.NodeSelector
import cool.graph.api.schema.APIErrors
import cool.graph.gc_values.{NullGCValue, _}
import cool.graph.shared.models.Project

import scala.concurrent.Future

case class VerifyConnection(project: Project, relationTableName: String, outerWhere: NodeSelector, innerWhere: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.connectionFailureTrigger(project, relationTableName, outerWhere, innerWhere)))
  }

//  override def handleErrors = {Some({ case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodesNotConnectedError(outerWhere, innerWhere)})}
//
////  def causedByThisMutaction(cause: String) = {
////    val parameterString = where.fieldValue match {
////      case StringGCValue(x) => s"parameters ['$x',"
////      case IntGCValue(x) => s"parameters [$x,"
////      case FloatGCValue(x) => s"parameters [$x,"
////      case BooleanGCValue(false) => s"parameters [0,"
////      case BooleanGCValue(true) => s"parameters [1,"
////      case GraphQLIdGCValue(x) => s"parameters ['$x',"
////      case EnumGCValue(x) => s"parameters ['$x',"
////      case DateTimeGCValue(x) => s"parameters ['${dateTimeFromISO8601(x)}',"
////      case JsonGCValue(x) => s"parameters ['$x',"
////      case ListGCValue(_) => sys.error("Not an acceptable Where")
////      case RootGCValue(_) => sys.error("Not an acceptable Where")
////      case NullGCValue => sys.error("Not an acceptable Where")
////    }
////
////  cause.contains(s"`${where.model.name}` where `${where.fieldName}` =") && cause.contains(parameterString)
////  }
}