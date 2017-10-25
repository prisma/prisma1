package cool.graph.client.mutations

import cool.graph._
import cool.graph.client.authorization.RelationMutationPermissions
import cool.graph.client.database.DataResolver
import cool.graph.client.mutations.definitions.UpdateOrCreateDefinition
import cool.graph.client.schema.InputTypesBuilder
import cool.graph.shared.models.{AuthenticatedRequest, Model, Project}
import cool.graph.util.coolSangria.Sangria
import sangria.schema
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateOrCreate(model: Model,
                     project: Project,
                     args: schema.Args,
                     dataResolver: DataResolver,
                     argumentSchema: ArgumentSchema,
                     allowSettingManagedFields: Boolean = false)(implicit inj: Injector)
    extends ClientMutation(model, args, dataResolver, argumentSchema)
    with Injectable {

  override val mutationDefinition = UpdateOrCreateDefinition(argumentSchema, project, InputTypesBuilder(project, argumentSchema))

  val argsPointer: Map[String, Any] = args.raw.get("input") match {
    case Some(value) => value.asInstanceOf[Map[String, Any]]
    case None        => args.raw
  }

  val updateMutation: Update = {
    val updateArgs = Sangria.rawArgs(argsPointer("update").asInstanceOf[Map[String, Any]])
    new Update(model, project, updateArgs, dataResolver, argumentSchema)
  }
  val createMutation: Create = {
    val createArgs = Sangria.rawArgs(argsPointer("create").asInstanceOf[Map[String, Any]])
    new Create(model, project, createArgs, dataResolver, argumentSchema)
  }

  var itemExists = false

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    for {
      exists <- dataResolver.existsByModelAndId(model, updateMutation.id)
      mutactionGroups <- if (exists) {
                          itemExists = true
                          updateMutation.prepareMutactions()
                        } else {
                          itemExists = false
                          createMutation.prepareMutactions()
                        }
    } yield {
      mutactionGroups
    }
  }

  override def checkPermissions(authenticatedRequest: Option[AuthenticatedRequest]): Future[Boolean] = {
    // TODO: what's the difference between Update and Create permission checking?
    if (itemExists) {
      updateMutation.checkPermissions(authenticatedRequest)
    } else {
      createMutation.checkPermissions(authenticatedRequest)
    }
  }

  override def checkPermissionsAfterPreparingMutactions(authenticatedRequest: Option[AuthenticatedRequest], mutactions: List[Mutaction]): Future[Unit] = {
    RelationMutationPermissions.checkAllPermissions(project, mutactions, authenticatedRequest)
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    if (itemExists) {
      returnValueById(model, updateMutation.id)
    } else {
      returnValueById(model, createMutation.id)
    }
  }
}
