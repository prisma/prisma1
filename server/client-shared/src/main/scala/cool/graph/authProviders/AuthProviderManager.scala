package cool.graph.authProviders

import cool.graph._
import cool.graph.client.UserContext
import cool.graph.client.database.DeferredResolverProvider
import cool.graph.client.mutations.Create
import cool.graph.client.mutations.definitions.CreateDefinition
import cool.graph.client.schema.simple.SimpleArgumentSchema
import cool.graph.client.schema.{InputTypesBuilder, SchemaModelObjectTypesBuilder}
import cool.graph.relay.schema.RelayArgumentSchema
import cool.graph.shared.errors.UserAPIErrors.InvalidAuthProviderData
import cool.graph.shared.models.IntegrationName.IntegrationName
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models._
import sangria.schema.InputObjectType.DefaultInput
import sangria.schema.{Argument, Context, InputField, InputObjectType, InputValue, ObjectType, OptionInputType, OptionType, UpdateCtx}
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class IntegrationSigninData(token: String, user: DataItem)

abstract class AuthProviderManager[MetaInfoType](implicit inj: Injector) extends Injectable {

  case class ManagedField(defaultName: String,
                          typeIdentifier: TypeIdentifier,
                          description: Option[String] = None,
                          isUnique: Boolean = false,
                          isReadonly: Boolean = true)

  val managedFields: List[ManagedField]
  val signupFields: List[ManagedField]
  val signinFields: List[ManagedField]
  val integrationName: IntegrationName
  val name: String
  def getmetaInformation: Option[AuthProviderMetaInformation]

  protected def resolveSignin(ctx: Context[UserContext, Unit], args: Map[String, Any]): Future[Option[AuthData]]

  protected def resolveSignup[T, A](ctx: Context[UserContext, Unit],
                                    customArgs: Map[String, Any],
                                    providerArgs: Map[String, Any],
                                    modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[T],
                                    argumentSchema: ArgumentSchema,
                                    deferredResolverProvider: DeferredResolverProvider[_, UserContext]): Future[Option[AuthData]]

  private def getSigninArgumentType = {
    val inputFields: List[InputField[Any]] =
      signinFields.map(f =>
        sangria.schema.InputField(f.defaultName, TypeIdentifier.toSangriaScalarType(f.typeIdentifier), description = f.description.getOrElse("")))

    OptionInputType(
      InputObjectType(
        name = integrationName.toString,
        fields = inputFields
      ))
  }

  private def getSignupArgumentType = {
    val inputFields: List[InputField[Any]] =
      signupFields.map(f =>
        sangria.schema.InputField(f.defaultName, TypeIdentifier.toSangriaScalarType(f.typeIdentifier), description = f.description.getOrElse("")))

    OptionInputType(
      InputObjectType(
        name = integrationName.toString,
        fields = inputFields
      ))
  }
}

case class AuthData(token: String, user: DataItem, clientMutationId: Option[String] = None)

object AuthProviderManager {

  def simpleMutationFields[T, A](
      project: Project,
      userModel: Model,
      userFieldType: ObjectType[UserContext, DataItem],
      modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[T],
      argumentSchema: ArgumentSchema,
      deferredResolverProvider: DeferredResolverProvider[_, UserContext])(implicit inj: Injector): List[sangria.schema.Field[UserContext, Unit]] = {
    val activeAuthProviders = project.authProviders
      .filter(_.integrationType == IntegrationType.AuthProvider)
      .filter(_.isEnabled)

    val hasExperimentalServerlessAuthProvider = project.experimentalAuthProvidersCustomMutations.nonEmpty

    def resolveSignin(ctx: Context[UserContext, Unit]): Future[Option[AuthData]] = {
      activeAuthProviders.foreach(auth => {
        val provider = AuthProviderManager.withName(auth.name)
        if (ctx.args.raw.get(provider.name).isDefined) {
          return provider.resolveSignin(ctx,
                                        ctx.args
                                          .raw(provider.name)
                                          .asInstanceOf[Option[Map[String, Any]]]
                                          .get)
        }
      })

      Future.successful(None)
    }

    def resolveCreate(ctx: Context[UserContext, Unit]): Future[Option[DataItem]] = {

//      if (!activeAuthProviders.isEmpty && ctx.ctx.user.isDefined && !ctx.ctx.user.get.isAdmin) {
//        throw new CannotCreateUserWhenSignedIn()
//      }

      activeAuthProviders.foreach(auth => {
        val customArgs: Map[String, Any] =
          ctx.args.raw.filter(x => x._1 != "authProvider")
        val provider = AuthProviderManager.withName(auth.name)
        if (extractAuthProviderField(ctx.args.raw).flatMap(_.get(provider.name)).isDefined) {
          return provider
            .resolveSignup(
              ctx,
              customArgs,
              extractAuthProviderField(ctx.args.raw)
                .get(provider.name)
                .asInstanceOf[Option[Map[String, Any]]]
                .get,
              modelObjectTypesBuilder,
              argumentSchema,
              deferredResolverProvider
            )
            .map(_.map(_.user))
        }
      })

      // fall back to normal create mutation when no auth providers

      if (!activeAuthProviders.isEmpty && !hasExperimentalServerlessAuthProvider) {
        throw new InvalidAuthProviderData("You must include at least one Auth Provider when creating user")
      }

      new Create(model = userModel, project = project, args = ctx.args, dataResolver = ctx.ctx.dataResolver, argumentSchema = argumentSchema)
        .run(ctx.ctx.authenticatedRequest, ctx.ctx)
        .map(Some(_))
    }

    val signinField = sangria.schema.Field(
      "signinUser",
      fieldType = AuthProviderManager.signinUserPayloadType(userFieldType, None, false),
      arguments = activeAuthProviders.map(auth =>
        Argument(name = AuthProviderManager.withName(auth.name).name, AuthProviderManager.withName(auth.name).getSigninArgumentType)),
      resolve = (ctx: Context[UserContext, Unit]) => resolveSignin(ctx)
    )

    val customFields =
      new CreateDefinition(SimpleArgumentSchema, project, InputTypesBuilder(project, SimpleArgumentSchema))
        .getSangriaArguments(model = userModel)
        .filter(removeEmailAndPassword(activeAuthProviders))

    def authProviderType: InputObjectType[DefaultInput] =
      InputObjectType(
        name = "AuthProviderSignupData",
        fields = activeAuthProviders.map(auth =>
          InputField(name = AuthProviderManager.withName(auth.name).name, AuthProviderManager.withName(auth.name).getSignupArgumentType))
      )

    val createArguments = (activeAuthProviders.isEmpty, hasExperimentalServerlessAuthProvider) match {
      case (true, _) => customFields
      case (false, false) => {
        customFields ++ List(sangria.schema.Argument("authProvider", authProviderType))
      }
      case (false, true) => {

        customFields ++ List(sangria.schema.Argument("authProvider", OptionInputType(authProviderType)))
      }
    }

    val createField = sangria.schema.Field(
      "createUser",
      fieldType = OptionType(userFieldType),
      arguments = createArguments,
      resolve = (ctx: Context[UserContext, Unit]) => resolveCreate(ctx)
    )

    activeAuthProviders.isEmpty match {
      case true  => List(createField)
      case false => List(signinField, createField)
    }
  }

  def relayMutationFields[T, A](
      project: Project,
      userModel: Model,
      viewerType: ObjectType[UserContext, Unit],
      userFieldType: ObjectType[UserContext, DataItem],
      modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[T],
      argumentSchema: ArgumentSchema,
      deferredResolverProvider: DeferredResolverProvider[_, UserContext])(implicit inj: Injector): List[sangria.schema.Field[UserContext, Unit]] = {
    val activeAuthProviders = project.authProviders
      .filter(_.integrationType == IntegrationType.AuthProvider)
      .filter(_.isEnabled)

    def resolveSignin(ctx: Context[UserContext, Unit]): Future[Option[AuthData]] = {
      val clientMutationId = ctx.args
        .raw("input")
        .asInstanceOf[Map[String, Any]]("clientMutationId")
        .asInstanceOf[String]

      activeAuthProviders.foreach(auth => {
        val provider = AuthProviderManager.withName(auth.name)
        val input    = ctx.args.raw("input").asInstanceOf[Map[String, Any]]
        if (input.get(provider.name).isDefined) {
          return provider
            .resolveSignin(ctx, input(provider.name).asInstanceOf[Option[Map[String, Any]]].get)
            .map(_.map(_.copy(clientMutationId = Some(clientMutationId))))
        }
      })

      Future.successful(None)
    }

    def resolveCreate(ctx: Context[UserContext, Unit]): Future[Option[AuthData]] = {
      val clientMutationId = ctx.args
        .raw("input")
        .asInstanceOf[Map[String, Any]]("clientMutationId")
        .asInstanceOf[String]

      activeAuthProviders.foreach(auth => {
        val input = ctx.args.raw("input").asInstanceOf[Map[String, Any]]
        val customArgs: Map[String, Any] =
          input.filter(x => x._1 != "authProvider")
        val provider = AuthProviderManager.withName(auth.name)
        if (extractAuthProviderField(input)
              .flatMap(_.get(provider.name))
              .isDefined) {
          return provider
            .resolveSignup(
              ctx,
              customArgs,
              extractAuthProviderField(input)
                .get(provider.name)
                .asInstanceOf[Option[Map[String, Any]]]
                .get,
              modelObjectTypesBuilder,
              argumentSchema,
              deferredResolverProvider
            )
            .map(_.map(_.copy(clientMutationId = Some(clientMutationId))))
        }
      })

      // fall back to normal create mutation when no auth providers

      if (!activeAuthProviders.isEmpty) {
        throw new InvalidAuthProviderData("You must include at least one Auth Provider when creating user")
      }

      new Create(model = userModel, project = project, args = ctx.args, dataResolver = ctx.ctx.dataResolver, argumentSchema = argumentSchema)
        .run(ctx.ctx.authenticatedRequest, ctx.ctx)
        .map(user => Some(AuthData(token = "", user = user, clientMutationId = Some(clientMutationId))))
    }

    val signinInputFields = activeAuthProviders.map(
      auth =>
        InputField(name = AuthProviderManager.withName(auth.name).name,
                   AuthProviderManager
                     .withName(auth.name)
                     .getSigninArgumentType)) ++ List(InputField("clientMutationId", sangria.schema.StringType))

    val signinInput = InputObjectType(
      name = "SigninUserInput",
      fields = signinInputFields
    )

    val signinField = sangria.schema.Field(
      "signinUser",
      fieldType = AuthProviderManager
        .signinUserPayloadType(userFieldType, Some(viewerType), true),
      arguments = List(Argument(name = "input", argumentType = signinInput)),
      resolve = (ctx: Context[UserContext, Unit]) =>
        UpdateCtx({
          resolveSignin(ctx)
            .map(
              _.map(
                authData =>
                  authData.copy(
                    clientMutationId = ctx.args
                      .raw("input")
                      .asInstanceOf[Map[String, Any]]
                      .get("clientMutationId")
                      .map(_.asInstanceOf[String]))))
        }) { payload =>
          ctx.ctx.copy(authenticatedRequest = payload.map(_.user).map(x => AuthenticatedUser(id = x.id, typeName = "User", originalToken = "")))
      }
    )

    val customFields =
      new CreateDefinition(RelayArgumentSchema, project, InputTypesBuilder(project, RelayArgumentSchema))
        .getSangriaArguments(model = userModel)
        .find(_.name == "input")
        .get
        .argumentType
        .asInstanceOf[InputObjectType[_]]
        .fields
        .filter(removeEmailAndPassword(activeAuthProviders))

    val createArguments = (activeAuthProviders.isEmpty match {
      case true => customFields
      case false => {
        val authProviderType: InputObjectType[DefaultInput] = InputObjectType(
          name = "AuthProviderSignupData",
          fields = activeAuthProviders.map(auth =>
            InputField(name = AuthProviderManager.withName(auth.name).name, AuthProviderManager.withName(auth.name).getSignupArgumentType))
        )

        customFields ++ List(sangria.schema.InputField("authProvider", authProviderType))
      }
    })

    val createInput = InputObjectType(
      name = "SignupUserInput",
      fields = createArguments
    )

    val createField = sangria.schema.Field(
      "createUser",
      fieldType = AuthProviderManager.createUserPayloadType(userFieldType, viewerType),
      arguments = List(Argument(name = "input", argumentType = createInput)),
      resolve = (ctx: Context[UserContext, Unit]) => resolveCreate(ctx)
    )

    activeAuthProviders.isEmpty match {
      case true  => List(createField)
      case false => List(signinField, createField)
    }
  }

  private def withName(name: IntegrationName)(implicit inj: Injector): AuthProviderManager[Unit] = name match {
    case IntegrationName.AuthProviderEmail  => new EmailAuthProviderManager()
    case IntegrationName.AuthProviderDigits => new DigitsAuthProviderManager()
    case IntegrationName.AuthProviderAuth0  => new Auth0AuthProviderManager()
    case _                                  => throw new Exception(s"$name is not an AuthProvider")
  }

  private def extractAuthProviderField(args: Map[String, Any]): Option[Map[String, Any]] = {
    args.get("authProvider") match {
      case None => None
      case Some(x) if x.isInstanceOf[Some[_]] => {
        x.asInstanceOf[Some[Map[String, Any]]]
      }
      case Some(authProvider: Map[_, _]) => {
        Some(authProvider.asInstanceOf[Map[String, Any]])
      }
    }
  }

  private def signinUserPayloadType(userFieldType: ObjectType[UserContext, DataItem],
                                    viewerType: Option[ObjectType[UserContext, Unit]],
                                    isRelay: Boolean): ObjectType[UserContext, Option[AuthData]] = {

    val fields = sangria.schema.fields[UserContext, Option[AuthData]](
      sangria.schema.Field(name = "token", fieldType = sangria.schema.OptionType(sangria.schema.StringType), resolve = _.value.map(_.token)),
      sangria.schema.Field(name = "user", fieldType = sangria.schema.OptionType(userFieldType), resolve = _.value.map(_.user))
    ) ++ (isRelay match {
      case true =>
        sangria.schema.fields[UserContext, Option[AuthData]](sangria.schema
          .Field(name = "clientMutationId", fieldType = sangria.schema.OptionType(sangria.schema.StringType), resolve = _.value.flatMap(_.clientMutationId)))
      case false => List()
    }) ++ (viewerType.isDefined match {
      case true =>
        sangria.schema.fields[UserContext, Option[AuthData]](sangria.schema.Field(name = "viewer", fieldType = viewerType.get, resolve = _ => ()))
      case false => List()
    })

    ObjectType(
      "SigninPayload",
      description = "If authentication was successful the payload contains the user and a token. If unsuccessful this payload is null.",
      fields = fields
    )
  }

  private def createUserPayloadType(userFieldType: ObjectType[UserContext, DataItem],
                                    viewerType: ObjectType[UserContext, Unit]): ObjectType[UserContext, Option[AuthData]] = {

    val fields =
      sangria.schema.fields[UserContext, Option[AuthData]](
        sangria.schema
          .Field(name = "user", fieldType = sangria.schema.OptionType(userFieldType), resolve = _.value.map(_.user)),
        sangria.schema.Field(name = "clientMutationId",
                             fieldType = sangria.schema.OptionType(sangria.schema.StringType),
                             resolve = _.value.flatMap(_.clientMutationId)),
        sangria.schema
          .Field(name = "viewer", fieldType = viewerType, resolve = _ => ())
      )

    ObjectType(
      "CreateUserPayload",
      description = "If authentication was successful the payload contains the user and a token. If unsuccessful this payload is null.",
      fields = fields
    )
  }

  private def removeEmailAndPassword(activeAuthProviders: List[AuthProvider]) =
    (f: InputValue[_]) => {
      // old password fields are not read only, so we filter them explicitly
      activeAuthProviders.exists(_.name == IntegrationName.AuthProviderEmail) match {
        case true  => f.name != "password"
        case false => true
      }
    }
}
