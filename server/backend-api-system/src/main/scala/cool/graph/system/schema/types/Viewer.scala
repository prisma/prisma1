package cool.graph.system.schema.types

import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models
import cool.graph.shared.models.{Client, ModelParser}
import cool.graph.system.SystemUserContext
import cool.graph.system.database.finder.{ProjectFinder, ProjectResolver}
import cool.graph.system.schema.types.Model.ModelContext
import cool.graph.system.schema.types.Relation.RelationContext
import cool.graph.system.schema.types._Field.FieldContext
import sangria.relay.Node
import sangria.schema._
import scaldi.Injector

import scala.concurrent.Future

case class ViewerModel(id: String) extends Node

object ViewerModel {
  val globalId = "static-viewer-id"

  def apply(): ViewerModel = new ViewerModel(ViewerModel.globalId)
}

object Viewer {
  import scala.concurrent.ExecutionContext.Implicits.global

  def getType(clientType: ObjectType[SystemUserContext, Client], projectResolver: ProjectResolver)(
      implicit inj: Injector): ObjectType[SystemUserContext, ViewerModel] = {

    val idArgument           = Argument("id", IDType)
    val projectNameArgument  = Argument("projectName", StringType)
    val modelNameArgument    = Argument("modelName", StringType)
    val relationNameArgument = Argument("relationName", StringType)
    val fieldNameArgument    = Argument("fieldName", StringType)

    def throwNotFound(item: String) = throw UserInputErrors.NotFoundException(s"$item not found")

    ObjectType(
      "Viewer",
      "This is the famous Relay viewer object",
      interfaces[SystemUserContext, ViewerModel](nodeInterface),
      idField[SystemUserContext, ViewerModel] ::
        fields[SystemUserContext, ViewerModel](
        Field("user", OptionType(clientType), resolve = ctx => {
          val client = ctx.ctx.getClient
          client
        }),
        Field(
          "project",
          OptionType(ProjectType),
          arguments = idArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val id                              = ctx.arg(idArgument)
            val project: Future[models.Project] = ProjectFinder.loadById(clientId, id)(projectResolver)
            project
          }
        ),
        Field(
          "projectByName",
          OptionType(ProjectType),
          arguments = projectNameArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val projectName                     = ctx.arg(projectNameArgument)
            val project: Future[models.Project] = ProjectFinder.loadByName(clientId, projectName)(ctx.ctx.internalDatabase, projectResolver)
            project
          }
        ),
        Field(
          "model",
          OptionType(ModelType),
          arguments = idArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val modelId                         = ctx.arg(idArgument)
            val project: Future[models.Project] = ProjectFinder.loadByModelId(clientId, modelId)(ctx.ctx.internalDatabase, projectResolver)
            project.map { project =>
              val model = project.getModelById_!(modelId)
              ModelContext(project, model)
            }
          }
        ),
        Field(
          "modelByName",
          OptionType(ModelType),
          arguments = projectNameArgument :: modelNameArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val modelName                       = ctx.arg(modelNameArgument)
            val projectName                     = ctx.arg(projectNameArgument)
            val project: Future[models.Project] = ProjectFinder.loadByName(clientId, projectName)(ctx.ctx.internalDatabase, projectResolver)
            project.map { project =>
              val model = ModelParser.modelByName(project, modelName, ctx.ctx.injector).getOrElse(throwNotFound("Model"))
              ModelContext(project, model)
            }
          }
        ),
        Field(
          "relation",
          OptionType(RelationType),
          arguments = idArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val id                              = ctx.arg(idArgument)
            val project: Future[models.Project] = ProjectFinder.loadByRelationId(clientId, id)(ctx.ctx.internalDatabase, projectResolver)
            project.map { project =>
              ModelParser
                .relation(project, id, ctx.ctx.injector)
                .map(rel => RelationContext(project, rel))
                .getOrElse(throwNotFound("Relation"))
            }
          }
        ),
        Field(
          "relationByName",
          OptionType(RelationType),
          arguments = projectNameArgument :: relationNameArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val projectName                     = ctx.arg(projectNameArgument)
            val project: Future[models.Project] = ProjectFinder.loadByName(clientId, projectName)(ctx.ctx.internalDatabase, projectResolver)

            project.map { project =>
              ModelParser
                .relationByName(project, ctx.arg(relationNameArgument), ctx.ctx.injector)
                .map(rel => RelationContext(project, rel))
                .getOrElse(throwNotFound("Relation by name"))
            }
          }
        ),
        Field(
          "field",
          OptionType(FieldType),
          arguments = idArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val fieldId                         = ctx.arg(idArgument)
            val project: Future[models.Project] = ProjectFinder.loadByFieldId(clientId, fieldId)(ctx.ctx.internalDatabase, projectResolver)
            project.map { project =>
              val field = project.getFieldById_!(fieldId)
              FieldContext(project, field)
            }
          }
        ),
        Field(
          "fieldByName",
          OptionType(FieldType),
          arguments =
            projectNameArgument :: modelNameArgument :: fieldNameArgument :: Nil,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val fieldName                       = ctx.arg(fieldNameArgument)
            val modelName                       = ctx.arg(modelNameArgument)
            val projectName                     = ctx.arg(projectNameArgument)
            val project: Future[models.Project] = ProjectFinder.loadByName(clientId, projectName)(ctx.ctx.internalDatabase, projectResolver)
            project.map { project =>
              val field =
                ModelParser.fieldByName(project, modelName, fieldName, ctx.ctx.injector).getOrElse(throwNotFound("Field by name"))
              FieldContext(project, field)
            }
          }
        )
      )
    )
  }
}
