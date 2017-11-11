package cool.graph.authProviders

import com.github.t3hnar.bcrypt._
import cool.graph.shared.errors.UserAPIErrors.{CannotSignUpUserWithCredentialsExist, UniqueConstraintViolation}
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.database.DeferredResolverProvider
import cool.graph.client.mutations.Create
import cool.graph.client.schema.simple.SimpleArgumentSchema
import cool.graph.client.UserContext
import cool.graph.shared.models.IntegrationName._
import cool.graph.shared.models.{AuthProviderMetaInformation, IntegrationName, TypeIdentifier}
import cool.graph.util.coolSangria.Sangria
import cool.graph.util.crypto.Crypto
import cool.graph.ArgumentSchema
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.errors.UserAPIErrors
import sangria.schema.Context
import scaldi.Injector
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class JwtEmailAuthData(email: String)

object EmailAuthJsonProtocol extends DefaultJsonProtocol {
  implicit val authDataFormat: RootJsonFormat[JwtEmailAuthData] = jsonFormat1(JwtEmailAuthData)
}

class EmailAuthProviderManager()(implicit inj: Injector) extends AuthProviderManager[Unit]()(inj) {
  val clientAuth = inject[ClientAuth]

  val emailField    = ManagedField(defaultName = "email", typeIdentifier = TypeIdentifier.String, isUnique = true, isReadonly = true)
  val passwordField = ManagedField(defaultName = "password", typeIdentifier = TypeIdentifier.String, isReadonly = true)

  override val managedFields: List[ManagedField] = List(emailField, passwordField)
  override val signupFields: List[ManagedField]  = List(emailField, passwordField)
  override val signinFields: List[ManagedField]  = List(emailField, passwordField)

  override val integrationName: IntegrationName = IntegrationName.AuthProviderEmail

  override val name = "email"

  override def getmetaInformation: Option[AuthProviderMetaInformation] = None

  import EmailAuthJsonProtocol._

  def resolveSignin(ctx: Context[UserContext, Unit], args: Map[String, Any]): Future[Option[AuthData]] = {
    val email    = args("email").asInstanceOf[String]
    val password = args("password").asInstanceOf[String]
    ctx.ctx.dataResolver.resolveByUnique(ctx.ctx.project.getModelByName_!("User"), "email", email) flatMap {
      case Some(user) if password.isBcrypted(user.get[String]("password")) =>
        clientAuth
          .loginUser(ctx.ctx.project, user, Some(JwtEmailAuthData(email = email)))
          .map(token => Some(AuthData(token = token, user = user)))
      case _ => throw UserAPIErrors.CannotSignInCredentialsInvalid()
    }
  }

  override def resolveSignup[T, A](ctx: Context[UserContext, Unit],
                                   customArgs: Map[String, Any],
                                   providerArgs: Map[String, Any],
                                   modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[T],
                                   argumentSchema: ArgumentSchema,
                                   deferredResolverProvider: DeferredResolverProvider[_, UserContext]): Future[Option[AuthData]] = {

    val userModel = ctx.ctx.dataResolver.project.getModelByName_!("User")

    val createArgs = Sangria.rawArgs(
      raw = customArgs ++ providerArgs + (passwordField.defaultName -> Crypto
        .hash(providerArgs(passwordField.defaultName).asInstanceOf[String])))

    val a = new Create(
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
      .map(user =>
        clientAuth
          .loginUser(ctx.ctx.project, user, Some(JwtEmailAuthData(email = user.get[String]("email"))))
          .map(token => Some(AuthData(token = token, user = user))))

    a.flatMap(identity)
  }
}
