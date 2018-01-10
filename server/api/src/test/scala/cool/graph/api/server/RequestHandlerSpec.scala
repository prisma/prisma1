package cool.graph.api.server

import akka.http.scaladsl.model.StatusCodes
import cool.graph.api.{ApiBaseSpec, GraphQLResponseAssertions}
import cool.graph.api.project.ProjectFetcher
import cool.graph.api.schema.APIErrors.InvalidToken
import cool.graph.api.schema.{ApiUserContext, SchemaBuilder}
import cool.graph.client.server.GraphQlRequestHandler
import cool.graph.deploy.specutils.TestProject
import cool.graph.shared.models.{Project, ProjectWithClientId}
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}
import sangria.schema.{ObjectType, Schema, SchemaValidationRule}
import spray.json.JsObject

import scala.concurrent.Future

class RequestHandlerSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils with GraphQLResponseAssertions {
  import testDependencies.bugSnagger
  import system.dispatcher

  "a request without token" should "result in an InvalidToken exception if a project has secrets" in {
    val (_, result) = handler(projectWithSecret).handleRawRequest(projectWithSecret.id, request("header")).await

    result.pathAsLong("errors.[0].code") should equal(3015)
    result.pathAsString("errors.[0].message") should include("Your token is invalid")
  }

  "request with a proper token" should "result in a successful query" in {
    val properHeader     = Jwt.encode("{}", projectWithSecret.secrets.head, JwtAlgorithm.HS256)
    val (status, result) = handler(projectWithSecret).handleRawRequest(projectWithSecret.id, request(properHeader)).await
    println(result)
    result.assertSuccessfulResponse("")
  }

  val projectWithSecret = TestProject().copy(secrets = Vector("secret"))

  def request(authHeader: String) =
    RawRequest(id = "request-id", json = JsObject(), ip = "0.0.0.0", sourceHeader = null, authorizationHeader = Some(authHeader))

  def handler(project: Project) = {
    RequestHandler(
      projectFetcher = ProjectFetcherStub(project),
      schemaBuilder = EmptySchemaBuilder,
      graphQlRequestHandler = SucceedingGraphQlRequesthandler,
      auth = AuthImpl,
      log = println
    )
  }
}

object SucceedingGraphQlRequesthandler extends GraphQlRequestHandler {
  override def handle(graphQlRequest: GraphQlRequest) = Future.successful {
    StatusCodes.ImATeapot -> JsObject()
  }

  override def healthCheck = Future.unit
}

object EmptySchemaBuilder extends SchemaBuilder {
  override def apply(project: Project): Schema[ApiUserContext, Unit] = {
    Schema(
      query = ObjectType("Query", List.empty),
      validationRules = SchemaValidationRule.empty
    )
  }
}

case class ProjectFetcherStub(project: Project) extends ProjectFetcher {
  override def fetch(projectIdOrAlias: String) = Future.successful {
    Some(ProjectWithClientId(project, project.ownerId))
  }
}
