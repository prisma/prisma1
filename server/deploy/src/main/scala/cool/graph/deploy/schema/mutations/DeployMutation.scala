package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import cool.graph.deploy.migration._
import cool.graph.deploy.migration.inference.{InvalidGCValue, MigrationStepsInferrer, RelationDirectiveNeeded, SchemaInferrer}
import cool.graph.deploy.migration.migrator.Migrator
import cool.graph.deploy.migration.validation.{SchemaError, SchemaSyntaxValidator}
import cool.graph.graphql.GraphQlClient
import cool.graph.shared.models.{Function, Migration, MigrationStep, Project, Schema, ServerSideSubscriptionFunction, WebhookDelivery}
import org.scalactic.{Bad, Good, Or}
import play.api.libs.json.{JsString, Json}
import sangria.parser.QueryParser

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

case class DeployMutation(
    args: DeployMutationInput,
    project: Project,
    schemaInferrer: SchemaInferrer,
    migrationStepsInferrer: MigrationStepsInferrer,
    schemaMapper: SchemaMapper,
    migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
    migrator: Migrator,
    graphQlClient: GraphQlClient
)(
    implicit ec: ExecutionContext
) extends Mutation[DeployMutationPayload] {

  val graphQlSdl   = QueryParser.parse(args.types).get
  val validator    = SchemaSyntaxValidator(args.types)
  val schemaErrors = validator.validate()

  override def execute: Future[MutationResult[DeployMutationPayload]] = {
    if (schemaErrors.nonEmpty) {
      Future.successful {
        MutationSuccess(
          DeployMutationPayload(
            clientMutationId = args.clientMutationId,
            migration = None,
            errors = schemaErrors
          ))
      }
    } else {
      performDeployment
    }
  }

  private def performDeployment: Future[MutationSuccess[DeployMutationPayload]] = {
    val schemaMapping = schemaMapper.createMapping(graphQlSdl)

    schemaInferrer.infer(project.schema, schemaMapping, graphQlSdl) match {
      case Good(inferredNextSchema) =>
        val steps = migrationStepsInferrer.infer(project.schema, inferredNextSchema, schemaMapping)
        for {
          _         <- handleProjectUpdate()
          functions <- callThis(args.functions)
          migration <- functions match {
                        case Bad(_)  => Future.successful(Some(Migration.empty(project.id)))
                        case Good(_) => handleMigration(inferredNextSchema, steps, functionsForInput)
                      }
        } yield {
          val functionErrors = functions.swap.getOrElse(Vector.empty)
          MutationSuccess {
            DeployMutationPayload(args.clientMutationId, migration = migration, errors = schemaErrors ++ functionErrors)
          }
        }

      case Bad(err) =>
        Future.successful {
          MutationSuccess(
            DeployMutationPayload(
              clientMutationId = args.clientMutationId,
              migration = None,
              errors = List(err match {
                case RelationDirectiveNeeded(t1, t1Fields, t2, t2Fields) => SchemaError.global(s"Relation directive required for types $t1 and $t2.")
                case InvalidGCValue(err)                                 => SchemaError.global(s"Invalid value '${err.value}' for type ${err.typeIdentifier}.")
              })
            ))
        }
    }
  }

//  val url           = ""
//  val graphQlClient = GraphQlClient(url, Map("Authorization" -> s"Bearer ${project.secrets.head}"))

  def callThis(fns: Vector[FunctionInput]): Future[Vector[Function] Or Vector[SchemaError]] = {
    foo(fns).map { errors =>
      if (errors.nonEmpty) {
        Bad(errors)
      } else {
        Good(functionsForInput)
      }
    }
  }

  def foo(fns: Vector[FunctionInput]): Future[Vector[SchemaError]] = Future.sequence(fns.map(bar)).map(_.flatten)

  def bar(fn: FunctionInput): Future[Vector[SchemaError]] = {
    graphQlClient
      .sendQuery(
        s"""{
       |  validateSubscriptionQuery(query: ${JsString(fn.query).toString()}){
       |    errors
       |  }
       |}""".stripMargin
      )
      .map { response =>
        response.bodyAs[Vector[String]]("data.validateSubscriptionQuery.errors").get
      }
      .map { errorMessages =>
        errorMessages.map(error => SchemaError(`type` = "Subscription", field = fn.name, description = error))
      }
  }

  val functionsForInput: Vector[Function] = {
    args.functions.map { fnInput =>
      ServerSideSubscriptionFunction(
        name = fnInput.name,
        isActive = true,
        delivery = WebhookDelivery(
          url = fnInput.url,
          headers = fnInput.headers.map(header => header.name -> header.value)
        ),
        query = fnInput.query
      )
    }
  }

  private def handleProjectUpdate(): Future[_] = {
    if (project.secrets != args.secrets && !args.dryRun.getOrElse(false)) {
      projectPersistence.update(project.copy(secrets = args.secrets))
    } else {
      Future.unit
    }
  }

  private def handleMigration(nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Future[Option[Migration]] = {
    val migrationNeeded = steps.nonEmpty || functions.nonEmpty
    val isNotDryRun     = !args.dryRun.getOrElse(false)
    if (migrationNeeded && isNotDryRun) {
      migrator.schedule(project.id, nextSchema, steps, functions).map(Some(_))
    } else {
      Future.successful(None)
    }
  }
}

case class DeployMutationInput(
    clientMutationId: Option[String],
    projectId: String,
    types: String,
    dryRun: Option[Boolean],
    secrets: Vector[String],
    functions: Vector[FunctionInput]
) extends sangria.relay.Mutation

case class FunctionInput(
    name: String,
    query: String,
    url: String,
    headers: Vector[HeaderInput]
)

case class HeaderInput(
    name: String,
    value: String
)

case class DeployMutationPayload(
    clientMutationId: Option[String],
    migration: Option[Migration],
    errors: Seq[SchemaError]
) extends sangria.relay.Mutation
