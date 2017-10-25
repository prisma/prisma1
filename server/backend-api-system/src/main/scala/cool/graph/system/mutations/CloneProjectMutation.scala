package cool.graph.system.mutations

import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.database.client.EmptyClientDbQueries
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.mutactions.client._
import cool.graph.system.mutactions.internal._
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class CloneProjectMutation(client: Client, project: Project, args: CloneProjectInput, projectDbsFn: models.Project => InternalAndProjectDbs)(
    implicit inj: Injector)
    extends InternalProjectMutation[CloneProjectPayload]
    with Injectable {

  val projectQueries: ProjectQueries = inject[ProjectQueries](identified by "projectQueries")

  var clonedProject: models.Project = Project(
    id = Cuid.createCuid(),
    name = args.name,
    ownerId = client.id,
    projectDatabase = project.projectDatabase
  )

  override def prepareActions(): List[Mutaction] = {

    // INTERNAL DATABASE

    var modelActions: List[Mutaction]                 = List()
    var fieldActions: List[Mutaction]                 = List()
    var modelPermissionActions: List[Mutaction]       = List()
    var modelPermissionFieldActions: List[Mutaction]  = List()
    var relationActions: List[Mutaction]              = List()
    var relationFieldMirrorActions: List[Mutaction]   = List()
    var relationPermissionActions: List[Mutaction]    = List()
    var actionActions: List[Mutaction]                = List()
    var authProviderActions: List[Mutaction]          = List()
    var rootTokenActions: List[Mutaction]             = List()
    var integrationActions: List[Mutaction]           = List()
    var searchProviderAlgoliaActions: List[Mutaction] = List()
    var algoliaSyncQueriesActions: List[Mutaction]    = List()
    var seatActions: List[Mutaction]                  = List()

    val clientDbQueries = EmptyClientDbQueries

    seatActions :+= CreateSeat(
      client,
      project = clonedProject,
      seat =
        Seat(id = Cuid.createCuid(), status = SeatStatus.JOINED, isOwner = true, email = client.email, clientId = Some(client.id), name = Some(client.name)),
      internalDatabase.databaseDef
    )

    var modelIdMap       = Map.empty[String, String]
    var fieldIdMap       = Map.empty[String, String]
    var fieldRelationMap = Map.empty[String, String] // new fieldId, old relationId
    project.models.foreach(model => {
      val newId = Cuid.createCuid()
      modelIdMap += (model.id -> newId)
      var clonedModel =
        model.copy(id = newId, fields = List(), permissions = List())
      modelActions :+= CreateModelWithoutSystemFields(project = clonedProject, model = clonedModel)

      model.fields.foreach(field => {
        val newId = Cuid.createCuid()
        fieldIdMap += (field.id -> newId)
        if (field.relation.isDefined) {
          fieldRelationMap += (newId -> field.relation.get.id)
        }
        val clonedField = field.copy(id = newId, relation = None) // keep old relation so we can patch it up later
        fieldActions :+= CreateField(project = clonedProject, model = clonedModel, field = clonedField, None, clientDbQueries)

        // RelationFieldMirror validation needs this
        clonedModel = clonedModel.copy(fields = clonedModel.fields :+ clonedField)
      })

      model.permissions.foreach(permission => {
        val clonedPermission =
          permission.copy(id = Cuid.createCuid(), fieldIds = List())
        modelPermissionActions :+= CreateModelPermission(project = clonedProject, model = clonedModel, permission = clonedPermission)

        permission.fieldIds.foreach(fieldId => {
          modelPermissionFieldActions :+= CreateModelPermissionField(project = clonedProject,
                                                                     model = clonedModel,
                                                                     permission = clonedPermission,
                                                                     fieldId = fieldIdMap(fieldId))
        })
      })

      // ActionTriggerMutationModel validation needs this
      clonedProject = clonedProject.copy(models = clonedProject.models :+ clonedModel)
    })

    val enumsToCreate = project.enums.map { enum =>
      val newEnum = enum.copy(id = Cuid.createCuid())
      CreateEnum(clonedProject, newEnum)
    }

    var relationIdMap = Map.empty[String, String]
    project.relations.foreach(relation => {
      val newId = Cuid.createCuid()
      relationIdMap += (relation.id -> newId)
      val clonedRelation =
        relation.copy(
          id = newId,
          modelAId = modelIdMap(relation.modelAId),
          modelBId = modelIdMap(relation.modelBId),
          fieldMirrors = relation.fieldMirrors.map(
            fieldMirror =>
              fieldMirror.copy(
                id = Cuid.createCuid(),
                relationId = newId,
                fieldId = fieldIdMap(fieldMirror.fieldId)
            ))
        )
      relationActions :+= CreateRelation(project = clonedProject, relation = clonedRelation, clientDbQueries = clientDbQueries)

      clonedRelation.permissions.foreach(relationPermission => {
        val newId                    = Cuid.createCuid()
        val clonedRelationPermission = relationPermission.copy(id = newId)

        relationPermissionActions :+= CreateRelationPermission(project = clonedProject, relation = clonedRelation, permission = clonedRelationPermission)
      })

      // RelationFieldMirror validation needs this
      clonedProject = clonedProject.copy(relations = clonedProject.relations :+ clonedRelation)

      clonedRelation.fieldMirrors.foreach(fieldMirror => {
        relationFieldMirrorActions :+= CreateRelationFieldMirror(project = clonedProject, relationFieldMirror = fieldMirror)
      })
    })

    def findNewEnumForOldEnum(enum: Option[Enum]): Option[Enum] = {
      for {
        oldEnum       <- enum
        newEnumCreate <- enumsToCreate.find(_.enum.name == oldEnum.name)
      } yield {
        newEnumCreate.enum
      }
    }

    fieldActions = fieldActions.map {
      case x: CreateField =>
        x.copy(
          project = clonedProject,
          field = x.field match {
            case f if fieldRelationMap.get(x.field.id).isDefined =>
              f.copy(relation = Some(clonedProject.getRelationById_!(relationIdMap(fieldRelationMap(f.id)))))

            case f =>
              f.copy(enum = findNewEnumForOldEnum(f.enum))
          }
        )
    }

    clonedProject = clonedProject.copy(models = clonedProject.models.map(model =>
      model.copy(fields = model.fields.map(field => {
        val oldField = project.getModelByName_!(model.name).getFieldByName_!(field.name)

        field.copy(relation = oldField.relation.map(oldRelation => clonedProject.getRelationById_!(relationIdMap(oldRelation.id))))
      }))))

    if (args.includeMutationCallbacks) {
      // TODO: relying on ActionTriggerMutationRelation to not get used, as not clean copying it
      project.actions.foreach(action => {
        val clonedAction = action.copy(
          id = Cuid.createCuid(),
          handlerWebhook = action.handlerWebhook.map(_.copy(id = Cuid.createCuid())),
          triggerMutationModel = action.triggerMutationModel.map(_.copy(id = Cuid.createCuid(), modelId = modelIdMap(action.triggerMutationModel.get.modelId))),
          triggerMutationRelation = None
        )
        actionActions ++= CreateAction.generateAddActionMutactions(project = clonedProject, action = clonedAction)
      })
    }

    project.authProviders.foreach(authProvider => {
      // don't need to copy the metaInformation as a new Cuid is generated internally
      val clonedAuthProvider = authProvider.copy(id = Cuid.createCuid())
      authProviderActions :+= CreateAuthProvider(project = clonedProject,
                                                 name = clonedAuthProvider.name,
                                                 metaInformation = authProvider.metaInformation,
                                                 isEnabled = authProvider.isEnabled)
    })

    project.integrations.foreach {
      case searchProviderAlgolia: SearchProviderAlgolia =>
        val clonedSearchProviderAlgolia = searchProviderAlgolia.copy(
          id = Cuid.createCuid(),
          subTableId = Cuid.createCuid(),
          algoliaSyncQueries = List()
        )
        integrationActions :+= CreateIntegration(project = clonedProject, integration = clonedSearchProviderAlgolia)
        searchProviderAlgoliaActions :+= CreateSearchProviderAlgolia(project = clonedProject, searchProviderAlgolia = clonedSearchProviderAlgolia)

        searchProviderAlgolia.algoliaSyncQueries.foreach(algoliaSyncQuery => {
          val clonedAlgoliaSyncQuery =
            algoliaSyncQuery.copy(id = Cuid.createCuid(), model = clonedProject.getModelById_!(modelIdMap(algoliaSyncQuery.model.id)))
          algoliaSyncQueriesActions :+= CreateAlgoliaSyncQuery(searchProviderAlgolia = clonedSearchProviderAlgolia, algoliaSyncQuery = clonedAlgoliaSyncQuery)
        })
      case _ =>
    }

    actions :+= CreateProject(client = client, project = clonedProject, projectQueries = projectQueries, internalDatabase = internalDatabase.databaseDef)
    actions ++= seatActions
    actions ++= enumsToCreate
    actions ++= modelActions
    actions ++= relationActions
    actions ++= fieldActions
    actions ++= modelPermissionActions
    actions ++= modelPermissionFieldActions
    actions ++= relationPermissionActions
    actions ++= relationFieldMirrorActions
    actions ++= actionActions
    actions ++= authProviderActions
    actions ++= rootTokenActions
    actions ++= integrationActions
    actions ++= searchProviderAlgoliaActions
    actions ++= algoliaSyncQueriesActions

    // PROJECT DATABASE

    actions :+= CreateClientDatabaseForProject(clonedProject.id)
    actions ++= clonedProject.models.map(model => CreateModelTable(clonedProject.id, model))
    actions ++= clonedProject.models.flatMap(
      model =>
        model.scalarFields
          .filter(f => !DatabaseMutationBuilder.implicitlyCreatedColumns.contains(f.name))
          .map(field => CreateColumn(clonedProject.id, model, field)))

    actions ++= clonedProject.relations.map(relation => CreateRelationTable(clonedProject, relation))
    actions ++= clonedProject.relations.flatMap(relation =>
      relation.fieldMirrors.map(fieldMirror => CreateRelationFieldMirrorColumn(clonedProject, relation, clonedProject.getFieldById_!(fieldMirror.fieldId))))

    if (args.includeData) {
      actions ++= clonedProject.models.map(
        model =>
          CopyModelTableData(sourceProjectId = project.id,
                             sourceModel = project.getModelByName_!(model.name),
                             targetProjectId = clonedProject.id,
                             targetModel = model))

      actions ++= project.relations.map(
        oldRelation =>
          CopyRelationTableData(
            sourceProject = project,
            sourceRelation = oldRelation,
            targetProjectId = clonedProject.id,
            targetRelation = clonedProject.getRelationById_!(relationIdMap(oldRelation.id))
        ))
    }

    actions
  }

  override def getReturnValue: Option[CloneProjectPayload] = {
    // note: we don't fully reconstruct the project (as we are cloning) since we just reload it in its
    // entirety from the DB in the SchemaBuilder
    Some(CloneProjectPayload(clientMutationId = args.clientMutationId, projectId = clonedProject.id, clonedProject = clonedProject))
  }
}

case class CloneProjectPayload(clientMutationId: Option[String], projectId: String, clonedProject: Project) extends Mutation

case class CloneProjectInput(clientMutationId: Option[String], projectId: String, name: String, includeData: Boolean, includeMutationCallbacks: Boolean)
