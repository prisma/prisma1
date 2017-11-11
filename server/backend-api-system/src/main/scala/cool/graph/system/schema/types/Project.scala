package cool.graph.system.schema.types

import cool.graph.shared.{ApiMatrixFactory, models}
import cool.graph.shared.models.{ActionTriggerType, IntegrationType}
import cool.graph.system.migration.dataSchema.SchemaExport
import cool.graph.system.migration.project.ClientInterchange
import cool.graph.system.schema.types.Model.{ModelContext, inject}
import cool.graph.system.schema.types.Relation.RelationContext
import cool.graph.system.schema.types.SearchProviderAlgolia.SearchProviderAlgoliaContext
import cool.graph.system.schema.types._Action.ActionContext
import cool.graph.system.schema.types._Field.FieldContext
import cool.graph.system.{ActionSchemaPayload, ActionSchemaPayloadMutationModel, SystemUserContext}
import sangria.relay._
import sangria.schema.{Field, _}
import scaldi.Injectable

object Project extends Injectable {
  lazy val Type: ObjectType[SystemUserContext, models.Project] = ObjectType(
    "Project",
    "This is a project",
    interfaces[SystemUserContext, models.Project](nodeInterface),
    idField[SystemUserContext, models.Project] ::
      fields[SystemUserContext, models.Project](
      Field("name", StringType, resolve = _.value.name),
      Field("alias", OptionType(StringType), resolve = _.value.alias),
      Field("version", IntType, resolve = _.value.revision),
      Field("region", RegionType, resolve = _.value.region),
      Field("projectDatabase", ProjectDatabaseType, resolve = _.value.projectDatabase),
      Field("schema", StringType, resolve = x => {
        SchemaExport.renderSchema(x.value)
      }),
      Field("typeSchema", StringType, resolve = x => {
        SchemaExport.renderTypeSchema(x.value)

      }),
      Field("enumSchema", StringType, resolve = x => {
        SchemaExport.renderEnumSchema(x.value)

      }),
      Field("projectDefinition", StringType, resolve = x => { // todo: reenable
        val z = ClientInterchange.export(x.value)(x.ctx.injector)
        z.content

      }),
      Field("projectDefinitionWithFileContent", StringType, resolve = x => {
        ClientInterchange.render(x.value)(x.ctx.injector)

      }),
      Field("isGlobalEnumsEnabled", BooleanType, resolve = _.value.isGlobalEnumsEnabled),
      Field("webhookUrl", OptionType(StringType), resolve = _.value.webhookUrl),
      Field("seats",
            seatConnection,
            arguments = Connection.Args.All,
            resolve = ctx =>
              Connection.connectionFromSeq(ctx.value.seats
                                             .sortBy(_.id),
                                           ConnectionArgs(ctx))),
      Field(
        "integrations",
        integrationConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          val integrations: Seq[models.Integration] = ctx.value.integrations
            .filter(_.integrationType == IntegrationType.SearchProvider)
            .sortBy(_.id.toString)
            .map {
              case x: models.SearchProviderAlgolia => SearchProviderAlgoliaContext(ctx.value, x)
              case x                               => x
            }

          Connection.connectionFromSeq(
            // todo: integrations should return all integrations, but we need to find a way to make it work with fragments
            // and adjust `IntegrationsSpec`
            integrations,
            ConnectionArgs(ctx)
          )
        }
      ),
      Field(
        "authProviders",
        authProviderConnection,
        arguments = Connection.Args.All,
        resolve = ctx =>
          Connection
            .connectionFromSeq(ctx.value.authProviders
                                 .sortBy(_.name.toString),
                               ConnectionArgs(ctx))
      ),
      Field(
        "fields",
        projectFieldConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          val project      = ctx.value
          implicit val inj = ctx.ctx.injector
          val apiMatrix    = inject[ApiMatrixFactory].create(project)
          val fields = apiMatrix
            .filterFields(
              apiMatrix
                .filterModels(project.models)
                .sortBy(_.id)
                .flatMap(model => model.fields))
            .map(field => FieldContext(project, field))

          Connection
            .connectionFromSeq(fields, ConnectionArgs(ctx))
        }
      ),
      Field(
        "models",
        modelConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          implicit val inj = ctx.ctx.injector
          val apiMatrix    = inject[ApiMatrixFactory].create(ctx.value)
          Connection
            .connectionFromSeq(apiMatrix
                                 .filterModels(ctx.value.models)
                                 .sortBy(_.id)
                                 .map(model => ModelContext(ctx.value, model)),
                               ConnectionArgs(ctx))
        }
      ),
      Field("enums", enumConnection, arguments = Connection.Args.All, resolve = ctx => {
        Connection.connectionFromSeq(ctx.value.enums, ConnectionArgs(ctx))
      }),
      Field(
        "packageDefinitions",
        packageDefinitionConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          Connection
            .connectionFromSeq(ctx.value.packageDefinitions
                                 .sortBy(_.id)
                                 .map(packageDefinition => packageDefinition),
                               ConnectionArgs(ctx))
        }
      ),
      Field(
        "relations",
        relationConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          implicit val inj     = ctx.ctx.injector
          val apiMatrix        = inject[ApiMatrixFactory].create(ctx.value)
          val relations        = apiMatrix.filterRelations(ctx.value.relations).sortBy(_.id)
          val relationContexts = relations.map(rel => RelationContext(ctx.value, rel))
          Connection.connectionFromSeq(relationContexts, ConnectionArgs(ctx))
        }
      ),
      Field(
        "permanentAuthTokens",
        rootTokenConnection,
        arguments = Connection.Args.All,
        resolve = ctx =>
          Connection.connectionFromSeq(ctx.value.rootTokens
                                         .sortBy(_.id),
                                       ConnectionArgs(ctx))
      ),
      Field(
        "functions",
        functionConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          val functions: Seq[Function.FunctionInterface] =
            ctx.value.functions.sortBy(_.id).map(Function.mapToContext(ctx.value, _))
          Connection.connectionFromSeq(functions, ConnectionArgs(ctx))
        }
      ),
      Field(
        "featureToggles",
        featureToggleConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          Connection.connectionFromSeq(ctx.value.featureToggles, ConnectionArgs(ctx))
        }
      ),
      Field(
        "actions",
        actionConnection,
        arguments = Connection.Args.All,
        resolve = ctx => Connection.connectionFromSeq(ctx.value.actions.sortBy(_.id).map(a => ActionContext(ctx.value, a)), ConnectionArgs(ctx))
      ), {
        val modelIdArgument = Argument("modelId", IDType)
        val modelMutationTypeArgument =
          Argument("modelMutationType", ModelMutationTypeType)

        Field(
          "actionSchema",
          StringType,
          arguments = List(modelIdArgument, modelMutationTypeArgument),
          resolve = ctx => {
            val payload = ActionSchemaPayload(
              triggerType = ActionTriggerType.MutationModel,
              mutationModel = Some(
                ActionSchemaPayloadMutationModel(
                  modelId = ctx arg modelIdArgument,
                  mutationType =
                    ctx arg modelMutationTypeArgument
                )),
              mutationRelation = None
            )

            ctx.ctx.getActionSchema(ctx.value, payload)
          }
        )
      },
      Field("allowMutations", BooleanType, resolve = _.value.allowMutations),
      Field("availableUserRoles", ListType(StringType), resolve = _ => List()),
      Field(
        "functionRequestHistogram",
        ListType(IntType),
        arguments = List(Argument("period", HistogramPeriodType)),
        resolve = ctx => {

          ctx.ctx.logsDataResolver.calculateHistogram(ctx.value.id, ctx.arg("period"))
        }
      ),
      Field("isEjected", BooleanType, resolve = _.value.isEjected)
    )
  )
}
