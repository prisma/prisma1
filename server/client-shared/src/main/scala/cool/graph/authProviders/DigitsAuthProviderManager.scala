package cool.graph.authProviders

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import cool.graph.ArgumentSchema
import cool.graph.shared.errors.UserAPIErrors.{CannotSignInCredentialsInvalid, CannotSignUpUserWithCredentialsExist, UniqueConstraintViolation}
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.database.DeferredResolverProvider
import cool.graph.client.mutations.Create
import cool.graph.client.schema.simple.SimpleArgumentSchema
import cool.graph.client.UserContext
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.models.IntegrationName._
import cool.graph.shared.models.{AuthProviderMetaInformation, IntegrationName, TypeIdentifier}
import cool.graph.util.coolSangria.Sangria
import org.apache.http.auth.InvalidCredentialsException
import sangria.schema.Context
import scaldi.Injector
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DigitsResponse(id: Int, phoneNumber: String, access: DigitsResponseAccess)
case class DigitsResponseAccess(token: String, secret: String)

case class JwtDigitsAuthData(digitsToken: String, digitsSecret: String)

object DigitsAuthJsonProtocol extends DefaultJsonProtocol {
  implicit val responseAccessFormat: RootJsonFormat[DigitsResponseAccess] = jsonFormat(DigitsResponseAccess, "token", "secret")
  implicit val responseFormat: RootJsonFormat[DigitsResponse]             = jsonFormat(DigitsResponse, "id", "phone_number", "access_token")
  implicit val authDataFormat: RootJsonFormat[JwtDigitsAuthData]          = jsonFormat2(JwtDigitsAuthData)
}

class DigitsAuthProviderManager(implicit inj: Injector) extends AuthProviderManager[Unit]()(inj) {

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")
  val clientAuth                               = inject[ClientAuth]

  val digitsIdField = ManagedField(defaultName = "digitsId", typeIdentifier = TypeIdentifier.String, isUnique = true)

  val apiUrlField      = ManagedField(defaultName = "apiUrl", TypeIdentifier.String)
  val credentialsField = ManagedField(defaultName = "credentials", TypeIdentifier.String)

  override val managedFields: List[ManagedField] = List(digitsIdField)
  override val signupFields: List[ManagedField]  = List(apiUrlField, credentialsField)
  override val signinFields: List[ManagedField]  = List(apiUrlField, credentialsField)

  override val integrationName: IntegrationName = IntegrationName.AuthProviderDigits

  override val name = "digits"

  override def getmetaInformation: Option[AuthProviderMetaInformation] = None

  import DigitsAuthJsonProtocol._

  def resolveSignin(ctx: Context[UserContext, Unit], args: Map[String, Any]): Future[Option[AuthData]] = {

    sendRequestToDigits(args)
      .recover {
        case e => throw CannotSignInCredentialsInvalid()
      }
      // TODO validate oauth payload against DIGITS_CONSUMER_KEY
      .map(
        resp =>
          ctx.ctx.dataResolver
            .resolveByUnique(ctx.ctx.project.getModelByName_!("User"), "digitsId", resp.id)
            .map(_ map (user => (user, JwtDigitsAuthData(digitsToken = resp.access.token, digitsSecret = resp.access.secret)))))
      .flatMap(identity)
      .flatMap {
        case Some((user, authData)) =>
          clientAuth
            .loginUser(ctx.ctx.project, user, Some(authData))
            .map(token => Some(AuthData(token = token, user = user)))
        case None => Future.successful(None)
      }
  }

  def sendRequestToDigits(args: Map[String, Any]): Future[DigitsResponse] = {
    val apiUrlArgument      = args("apiUrl").asInstanceOf[String]
    val credentialsArgument = args("credentials").asInstanceOf[String]

    Http()
      .singleRequest(
        HttpRequest(method = HttpMethods.GET,
                    uri = apiUrlArgument,
                    headers =
                      Authorization(GenericHttpCredentials(scheme = "", token = credentialsArgument)) :: Nil))
      .flatMap {
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          entity.dataBytes.runFold(ByteString(""))(_ ++ _)
        case _ => throw new InvalidCredentialsException()
      }
      .map(_.decodeString("UTF-8"))
      .map(_.parseJson.convertTo[DigitsResponse])
  }

  override def resolveSignup[T, A](ctx: Context[UserContext, Unit],
                                   customArgs: Map[String, Any],
                                   providerArgs: Map[String, Any],
                                   modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[T],
                                   argumentSchema: ArgumentSchema,
                                   deferredResolverProvider: DeferredResolverProvider[_, UserContext]): Future[Option[AuthData]] = {

    val userModel =
      ctx.ctx.dataResolver.project.models.find(_.name == "User").get

    sendRequestToDigits(providerArgs)
      .recover {
        case e => throw CannotSignUpUserWithCredentialsExist()
      }
      // TODO validate oauth payload against DIGITS_CONSUMER_KEY
      .map(resp => {
        val createArgs =
          Sangria.rawArgs(raw = customArgs + ("digitsId" -> resp.id))
        new Create(
          model = userModel,
          project = ctx.ctx.project,
          args = createArgs,
          dataResolver = ctx.ctx.dataResolver,
          argumentSchema = SimpleArgumentSchema,
          allowSettingManagedFields = true
        ).run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .recover {
            case e: UniqueConstraintViolation => throw CannotSignUpUserWithCredentialsExist()
          }
          .map(user => {

            val authData = JwtDigitsAuthData(digitsToken = resp.access.token, digitsSecret = resp.access.secret)

            clientAuth
              .loginUser(ctx.ctx.project, user, Some(authData))
              .map(token => Some(AuthData(token = token, user = user)))
          })
      })
      .flatMap(identity)
      .flatMap(identity)
  }
}
