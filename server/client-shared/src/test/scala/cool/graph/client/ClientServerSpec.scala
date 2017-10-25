package cool.graph.client

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.OK
import cool.graph.DataItem
import cool.graph.bugsnag.BugSnaggerMock
import cool.graph.client.authorization.ClientAuth
import cool.graph.client.finder.ProjectFetcher
import cool.graph.client.server._
import cool.graph.cloudwatch.CloudwatchMock
import cool.graph.shared.{ApiMatrixFactory, DefaultApiMatrix}
import cool.graph.shared.logging.RequestLogger
import cool.graph.shared.models._
import cool.graph.util.ErrorHandlerFactory
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Identifier, Injector, Module}
import spray.json._

import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

class ClientServerSpec extends FlatSpec with Matchers {

  ".handleRawRequestForPermissionSchema()" should "fail if no authentication is provided" in {
    val clientAuth: ClientAuth = succeedingClientAuthForRootToken
    val handler                = requestHandler(clientAuth)
    val result                 = await(handler.handleRawRequestForPermissionSchema("projectId", rawRequestWithoutAuth))
    println(result)
    result._2.assertError("Insufficient permissions")
  }

  ".handleRawRequestForPermissionSchema()" should "fail if authentication is provided, but ClientAuth.authenticateRequest fails" in {
    val clientAuth = failingClientAuth
    val handler    = requestHandler(clientAuth)
    val result     = await(handler.handleRawRequestForPermissionSchema("projectId", rawRequestWithAuth))
    println(result)
    result._2.assertError("Insufficient permissions")
  }

  ".handleRawRequestForPermissionSchema()" should "fail if authentication is provided and ClientAuth.authenticateRequest results in a normal User" in {
    val clientAuth = succeedingClientAuthForNormalUser
    val handler    = requestHandler(clientAuth)
    val result     = await(handler.handleRawRequestForPermissionSchema("projectId", rawRequestWithAuth))
    println(result)
    result._2.assertError("Insufficient permissions")
  }

  ".handleRawRequestForPermissionSchema()" should "succeed if authentication is provided and ClientAuth.authenticateRequest results in a Root Token" in {
    val clientAuth = succeedingClientAuthForRootToken
    val handler    = requestHandler(clientAuth)
    val result     = await(handler.handleRawRequestForPermissionSchema("projectId", rawRequestWithAuth))
    println(result)
    result._2.assertSuccess
  }

  ".handleRawRequestForPermissionSchema()" should "succeed if authentication is provided and ClientAuth.authenticateRequest results in a Customer" in {
    val clientAuth = succeedingClientAuthForCustomer
    val handler    = requestHandler(clientAuth)
    val result     = await(handler.handleRawRequestForPermissionSchema("projectId", rawRequestWithAuth))
    println(result)
    result._2.assertSuccess
  }

  val logger = new RequestLogger("", println)
  logger.begin // otherwise the ClientServer freaks out
  val rawRequestWithoutAuth = RawRequest(
    json = """ {"query": "{ foo }"} """.parseJson,
    ip = "some.ip",
    sourceHeader = None,
    authorizationHeader = None,
    logger = logger
  )
  val rawRequestWithAuth = rawRequestWithoutAuth.copy(authorizationHeader = Some("Bearer super-token"))

  val failingClientAuth                 = clientAuthStub(token => Future.failed(new Exception(s"this goes wrong for some reason. Token was: $token")))
  val succeedingClientAuthForCustomer   = clientAuthStub(token => Future.successful(AuthenticatedCustomer(id = "id", originalToken = token)))
  val succeedingClientAuthForRootToken  = clientAuthStub(token => Future.successful(AuthenticatedRootToken(id = "id", originalToken = token)))
  val succeedingClientAuthForNormalUser = clientAuthStub(token => Future.successful(AuthenticatedUser(id = "id", typeName = "User", originalToken = token)))

  def clientAuthStub(resultFn: String => Future[AuthenticatedRequest]): ClientAuth = {
    new ClientAuth {
      override def loginUser[T: JsonFormat](project: Project, user: DataItem, authData: Option[T]) = ???

      override def authenticateRequest(sessionToken: String, project: Project): Future[AuthenticatedRequest] = resultFn(sessionToken)
    }
  }

  def requestHandler(clientAuth: ClientAuth) = {

    val errorHandlerFactory = ErrorHandlerFactory(
      log = println,
      cloudwatch = CloudwatchMock,
      bugsnagger = BugSnaggerMock
    )
    val projectFetcher = new ProjectFetcher {
      override def fetch(projectIdOrAlias: String): Future[Option[ProjectWithClientId]] = Future.successful {
        val models      = List(Model("id", name = "Todo", isSystem = false))
        val testDb      = ProjectDatabase(id = "test-project-database-id", region = Region.EU_WEST_1, name = "client1", isDefaultForRegion = true)
        val testProject = Project(id = "test-project-id", ownerId = "test-client-id", name = s"Test Project", projectDatabase = testDb, models = models)
        Some(ProjectWithClientId(testProject, "id"))
      }
    }
    val ec = ExecutionContext.global

    val graphQlRequestHandler = new GraphQlRequestHandler {
      override def handle(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = Future.successful {
        OK -> """ {"message": "success"} """.parseJson
      }

      override def healthCheck = Future.successful(())
    }

    val injector = new Module {
      bind[ApiMatrixFactory] toNonLazy ApiMatrixFactory(DefaultApiMatrix(_))
    }

    RequestHandler(
      errorHandlerFactory = errorHandlerFactory,
      projectSchemaFetcher = projectFetcher,
      projectSchemaBuilder = null,
      graphQlRequestHandler = graphQlRequestHandler,
      clientAuth = clientAuth,
      log = println
    )(BugSnaggerMock, injector, ec)
  }

  implicit class ResultAssertions(json: JsValue) {
    def assertSuccess = {
      require(
        requirement = !hasError,
        message = s"The query had to result in a success but it returned an error. Here's the response: \n $json"
      )
    }

    def assertError(shouldInclude: String) = {
      require(
        requirement = hasError,
        message = s"The query had to result in an error but it returned no errors. Here's the response: \n $json"
      )
      require(
        requirement = json.toString.contains(shouldInclude),
        message = s"The query did not contain the expected fragment [$shouldInclude]. Here's the response: \n $json"
      )
    }

    private def hasError: Boolean = json.asJsObject.fields.get("error").isDefined
  }

  import scala.concurrent.duration._
  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, 5.seconds)
}
