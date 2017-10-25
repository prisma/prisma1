package cool.graph.client.authorization

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.Types
import cool.graph.client.authorization.queryPermissions.QueryPermissionValidator
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models._
import sangria.ast.Document
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class PermissionQueryArg(name: String, value: Any, typeIdentifier: TypeIdentifier)

class PermissionValidator(project: Project)(implicit inj: Injector) extends Injectable {

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")

  val validator = new QueryPermissionValidator(project)

  def checkModelQueryPermissions(
      project: Project,
      permissions: List[ModelPermission],
      authenticatedRequest: Option[AuthenticatedRequest],
      nodeId: Types.Id,
      permissionQueryArgs: Seq[PermissionQueryArg],
      alwaysQueryMasterDatabase: Boolean
  )(implicit inj: Injector, system: ActorSystem, materializer: ActorMaterializer): Future[Boolean] = {
    if (project.hasGlobalStarPermission) {
      return Future.successful(true)
    }

    val predefinedVars = Map(
      "$userId"  -> (authenticatedRequest.map(_.id).getOrElse(""), "ID"),
      "$user_id" -> (authenticatedRequest.map(_.id).getOrElse(""), "ID"),
      "$nodeId"  -> (nodeId, "ID"),
      "$node_id" -> (nodeId, "ID")
    ) ++ permissionQueryArgs
      .filter(_.name != "$node_id")
      .map(x =>
        x.name -> (x.value, x.typeIdentifier match {
          case TypeIdentifier.GraphQLID => "ID"
          case x                        => x.toString
        }))

    val queries = permissions
      .filter(_.rule == CustomRule.Graph)
      .filter(_.userType == UserType.Everyone || authenticatedRequest.isDefined)
      .map(_.ruleGraphQuery.getOrElse(""))

    Future
      .sequence(
        queries
          .map(p => checkQueryPermission(authenticatedRequest, p, predefinedVars, alwaysQueryMasterDatabase)))
      .map(_.exists(b => b))
  }

  def checkRelationQueryPermissions(
      project: Project,
      permissions: List[RelationPermission],
      authenticatedRequest: Option[AuthenticatedRequest],
      permissionQueryArgs: Map[String, (Any, String)],
      alwaysQueryMasterDatabase: Boolean
  ): Future[Boolean] = {
    if (project.hasGlobalStarPermission) {
      return Future.successful(true)
    }

    val queries = permissions.filter(_.rule == CustomRule.Graph).map(_.ruleGraphQuery.getOrElse(""))

    Future
      .sequence(
        queries
          .map(p => checkQueryPermission(authenticatedRequest, p, permissionQueryArgs, alwaysQueryMasterDatabase)))
      .map(_.exists(b => b))
  }

  private def checkQueryPermission(
      authenticatedRequest: Option[AuthenticatedRequest],
      permission: String,
      permissionQueryArgs: Map[String, (Any, String)],
      alwaysQueryMasterDatabase: Boolean
  ): Future[Boolean] = {

    val (injectedQuery, variables) = injectQueryParams(permission, permissionQueryArgs)

    validator.validate(injectedQuery, variables, authenticatedRequest, alwaysQueryMasterDatabase)
  }

  //this generates a query to validate by prepending the provided arguments and their types in front of it/ the prepending should not happen for the correctly formatted queries
  private def injectQueryParams(query: String, permissionQueryArgs: Map[String, (Any, String)]): (String, Map[String, Any]) = {

    def isQueryValidGraphQL(query: String): Option[Document] = sangria.parser.QueryParser.parse(query).toOption

    def prependQueryWithHeader(query: String) = {
      val usedVars    = permissionQueryArgs.filter(field => query.contains(field._1))
      val vars        = usedVars.map(field => s"${field._1}: ${field._2._2}").mkString(", ")
      val queryHeader = if (usedVars.isEmpty) "query " else s"query ($vars) "
      queryHeader + query + " "
    }

    val usedVars       = permissionQueryArgs.filter(field => query.contains(field._1))
    val outputArgs     = usedVars.map(field => (field._1.substring(1), field._2._1))
    val prependedQuery = prependQueryWithHeader(query)
    isQueryValidGraphQL(prependedQuery) match {
      case None =>
        isQueryValidGraphQL(query) match {
          case None      => ("# Could not parse the query. Please check that it is valid.\n" + query, outputArgs) // todo or throw error directly?
          case Some(doc) => (query, outputArgs)
        }
      case Some(doc) => (prependedQuery, outputArgs)
    }
  }

//  private def injectQueryParams(query: String, permissionQueryArgs: Map[String, (Any, String)]): (String, Map[String, Any]) = {
//
//    val usedVars    = permissionQueryArgs.filter(field => query.contains(field._1))
//    val vars        = usedVars.map(field => s"${field._1}: ${field._2._2}").mkString(", ")
//    val queryHeader = if (usedVars.isEmpty) "query " else s"query ($vars) "
//
//    (queryHeader + query + " ", usedVars.map(field => (field._1.substring(1), field._2._1)))
//  }

}
