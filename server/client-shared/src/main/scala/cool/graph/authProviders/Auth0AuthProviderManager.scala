package cool.graph.authProviders

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.shared.errors.UserAPIErrors.{CannotSignUpUserWithCredentialsExist, UniqueConstraintViolation}
import cool.graph.client.authorization.{Auth0Jwt, ClientAuth, ClientAuthImpl}
import cool.graph.client.database.{DataResolver, DeferredResolverProvider}
import cool.graph.client.mutations.Create
import cool.graph.client.schema.simple.SimpleArgumentSchema
import cool.graph.client.UserContext
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.IntegrationName._
import cool.graph.shared.models.{IntegrationName, _}
import cool.graph.util.coolSangria.Sangria
import cool.graph.{ArgumentSchema, DataItem}
import sangria.schema.Context
import scaldi.Injector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Auth0AuthProviderManager(implicit inj: Injector) extends AuthProviderManager[Unit]()(inj) {

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")
  val clientAuth                               = inject[ClientAuth]

  val auth0UserIdField = ManagedField(defaultName = "auth0UserId", typeIdentifier = TypeIdentifier.String, isUnique = true)

  val idTokenField =
    ManagedField(
      defaultName = "idToken",
      TypeIdentifier.String,
      description = Some(
        "Is returned when calling any of the Auth0 functions which invoke authentication. This includes calls to the Lock widget, to the auth0.js library, or the libraries for other languages. See https://auth0.com/docs/tokens/id_token for more detail")
    )

  override val managedFields: List[ManagedField] = List(auth0UserIdField)
  override val signupFields: List[ManagedField]  = List(idTokenField)
  override val signinFields: List[ManagedField]  = List(idTokenField)

  override val integrationName: IntegrationName = IntegrationName.AuthProviderAuth0

  override val name = "auth0"

  override def getmetaInformation: Option[AuthProviderMetaInformation] = None

  import cool.graph.client.authorization.Auth0AuthJsonProtocol._

  def resolveSignin(ctx: Context[UserContext, Unit], args: Map[String, Any]): Future[Option[AuthData]] = {
    val idToken = args(idTokenField.defaultName).asInstanceOf[String]

    Auth0Jwt.parseTokenAsAuth0AuthData(ctx.ctx.project, idToken) match {
      case Some(authData) =>
        getUser(ctx.ctx.dataResolver, authData.auth0UserId) flatMap {
          case Some(user) =>
            clientAuth
              .loginUser(ctx.ctx.project, user, Some(authData))
              .map(token => Some(AuthData(token = token, user = user)))
          case None =>
            throw UserAPIErrors.CannotSignInCredentialsInvalid()
        }
      case None =>
        throw UserAPIErrors.InvalidSigninData()
    }
  }

  override def resolveSignup[T, A](ctx: Context[UserContext, Unit],
                                   customArgs: Map[String, Any],
                                   providerArgs: Map[String, Any],
                                   modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[T],
                                   argumentSchema: ArgumentSchema,
                                   deferredResolverProvider: DeferredResolverProvider[_, UserContext]): Future[Option[AuthData]] = {

    val userModel = ctx.ctx.dataResolver.project.getModelByName_!("User")
    val idToken   = providerArgs(idTokenField.defaultName).asInstanceOf[String]

    Auth0Jwt.parseTokenAsAuth0AuthData(ctx.ctx.project, idToken) match {
      case Some(authData) =>
        val createArgs = Sangria.rawArgs(raw = customArgs + (auth0UserIdField.defaultName -> authData.auth0UserId))
        val a: Future[Future[Some[AuthData]]] =
          new Create(
            model = userModel,
            project = ctx.ctx.project,
            args = createArgs,
            dataResolver = ctx.ctx.dataResolver,
            argumentSchema = SimpleArgumentSchema,
            allowSettingManagedFields = true
          ).run(ctx.ctx.authenticatedRequest, ctx.ctx)
            .recover { case e: UniqueConstraintViolation => throw CannotSignUpUserWithCredentialsExist() }
            .map(user => {
              clientAuth
                .loginUser(ctx.ctx.project, user, Some(authData))
                .map(token => Some(AuthData(token = token, user = user)))
            })

        a.flatMap(identity)
      case None =>
        throw UserAPIErrors.Auth0IdTokenIsInvalid()
    }
  }

  private def getUser(dataResolver: DataResolver, auth0UserId: String): Future[Option[DataItem]] = {
    dataResolver.resolveByUnique(dataResolver.project.getModelByName_!("User"), auth0UserIdField.defaultName, auth0UserId)
  }
}
