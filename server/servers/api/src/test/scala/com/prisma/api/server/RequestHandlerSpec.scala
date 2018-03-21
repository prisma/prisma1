package com.prisma.api.server

import akka.http.scaladsl.model.StatusCodes
import com.prisma.api.project.ProjectFetcher
import com.prisma.api.schema.APIErrors.InvalidToken
import com.prisma.api.schema.{ApiUserContext, SchemaBuilder}
import com.prisma.api.{ApiBaseSpec, GraphQLResponseAssertions}
import com.prisma.auth.AuthImpl
import com.prisma.client.server.GraphQlRequestHandler
import com.prisma.shared.models.{Project, ProjectWithClientId}
import com.prisma.shared.schema_dsl.TestProject
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import pdi.jwt.{Jwt, JwtAlgorithm}
import sangria.schema.{ObjectType, Schema, SchemaValidationRule}
import play.api.libs.json._
import spray.json.{JsObject => SprayJsObject}

import scala.concurrent.Future

class RequestHandlerSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils with GraphQLResponseAssertions {
  import system.dispatcher
  import testDependencies.reporter

  "a request without token" should "result in an InvalidToken error" in {
    val error = handler(projectWithSecret).handleRawRequestForPublicApi(projectWithSecret.id, request("header")).failed.await
    error shouldBe an[InvalidToken]
  }

  "request with a proper token" should "result in a successful query" in {
    val properHeader = Jwt.encode("{}", projectWithSecret.secrets.head, JwtAlgorithm.HS256)
    val (_, result)  = handler(projectWithSecret).handleRawRequestForPublicApi(projectWithSecret.id, request(properHeader)).await
    result.assertSuccessfulResponse("")
  }

  val projectWithSecret = TestProject().copy(secrets = Vector("secret"))

  def request(authHeader: String) =
    RawRequest(id = "req-id", json = Json.obj("query" -> "{users}"), ip = "0.0.0.0", sourceHeader = null, authorizationHeader = Some(authHeader))

  def handler(project: Project) = {
    RequestHandler(
      projectFetcher = ProjectFetcherStub(project),
      schemaBuilder = EmptySchemaBuilder,
      graphQlRequestHandler = SucceedingGraphQlRequestHandler,
      auth = AuthImpl,
      log = println
    )
  }
}

object SucceedingGraphQlRequestHandler extends GraphQlRequestHandler {
  override def handle(graphQlRequest: GraphQlRequest) = Future.successful {
    StatusCodes.ImATeapot -> SprayJsObject.empty
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
