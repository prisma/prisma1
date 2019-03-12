package com.prisma.deploy.connector

import com.prisma.shared.models.Manifestations.{EmbeddedRelationLink, RelationTable}
import com.prisma.shared.models._

case class MigrationStepMapperImpl(project: Project) extends MigrationStepMapper {
  def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Vector[DeployMutaction] = step match {
    case x: CreateModel =>
      val model = nextSchema.getModelByName_!(x.name)
      Vector(CreateModelTable(project, model))

    case x: DeleteModel =>
      val model                = previousSchema.getModelByName_!(x.name)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Vector(DeleteModelTable(project, model, model.dbNameOfIdField_!, scalarListFieldNames))

    case x: UpdateModel =>
      val oldModel = previousSchema.getModelByName_!(x.name)
      val newModel = nextSchema.getModelByName_!(x.newName)
      Vector(UpdateModelTable(project = project, oldModel = oldModel, newModel = newModel))

    case x: CreateField =>
      val model = nextSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)

      field match {
        case _ if ReservedFields.idFieldName == field.name => Vector.empty
        case _: RelationField                              => Vector.empty
        case f: ScalarField if field.isScalarList          => Vector(CreateScalarListTable(project, model, f))
        case f: ScalarField if field.isScalarNonList       => Vector(CreateColumn(project, model, f))
      }

    case x: DeleteField =>
      val model = previousSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)

      field match {
        case _ if field.isRelation                   => Vector.empty
        case f: ScalarField if field.isScalarList    => Vector(DeleteScalarListTable(project, model, f))
        case f: ScalarField if field.isScalarNonList => Vector(DeleteColumn(project, model, f))
      }

    case x: UpdateField =>
      val oldModel                   = previousSchema.getModelByName_!(x.model)
      val newModel                   = nextSchema.getModelByName_!(x.newModel)
      val next                       = nextSchema.getFieldByName_!(x.newModel, x.finalName)
      val previous                   = previousSchema.getFieldByName_!(x.model, x.name)
      lazy val createColumn          = CreateColumn(project, oldModel, next.asScalarField_!)
      lazy val updateColumn          = UpdateColumn(project, oldModel, previous.asScalarField_!, next.asScalarField_!)
      lazy val deleteColumn          = DeleteColumn(project, oldModel, previous.asScalarField_!)
      lazy val createScalarListTable = CreateScalarListTable(project, oldModel, next.asScalarField_!)
      lazy val deleteScalarListTable = DeleteScalarListTable(project, oldModel, previous.asScalarField_!)
      lazy val updateScalarListTable = UpdateScalarListTable(project, oldModel, newModel, previous.asScalarField_!, next.asScalarField_!)

      // TODO: replace that with a pattern match based on the subtypes of `models.Field`
      () match {
        case _ if previous.isRelation && next.isRelation                                                       => Vector.empty
        case _ if previous.isRelation && next.isScalarNonList                                                  => Vector(createColumn)
        case _ if previous.isRelation && next.isScalarList                                                     => Vector(createScalarListTable)
        case _ if previous.isScalarList && next.isScalarNonList                                                => Vector(createColumn, deleteScalarListTable)
        case _ if previous.isScalarList && next.isRelation                                                     => Vector(deleteScalarListTable)
        case _ if previous.isScalarList && next.isScalarList && previous.typeIdentifier == next.typeIdentifier => Vector(updateScalarListTable)
        case _ if previous.isScalarList && next.isScalarList                                                   => Vector(deleteScalarListTable, createScalarListTable)
        case _ if previous.isScalarNonList && next.isRelation                                                  => Vector(deleteColumn)
        case _ if previous.isScalarNonList && next.isScalarList                                                => Vector(createScalarListTable, deleteColumn)
        case _ if previous.isScalarNonList && next.isScalarNonList =>
          val common         = Vector(updateColumn)
          val isIdTypeChange = previous.asScalarField_!.isId && next.asScalarField_!.isId && previous.asScalarField_!.typeIdentifier != next.asScalarField_!.typeIdentifier

          if (isIdTypeChange) {
            val deleteRelations = previousSchema.relations.filter(_.containsTheModel(previous.model)).map(deleteRelation).toVector
            val recreateRelations = nextSchema.relations
              .filter(r => r.containsTheModel(next.model))
              .filter(r => previousSchema.relations.exists(_.name == r.name))
              .map(createRelation)
              .toVector

            deleteRelations ++ common ++ recreateRelations
          } else {
            common
          }
      }

    case x: CreateRelation =>
      val relation = nextSchema.getRelationByName_!(x.name)
      Vector(createRelation(relation))

    case x: DeleteRelation =>
      val relation = previousSchema.getRelationByName_!(x.name)
      Vector(deleteRelation(relation))

    case x: UpdateRelation =>
      val previousRelation      = previousSchema.getRelationByName_!(x.name)
      val nextRelation          = nextSchema.getRelationByName_!(x.finalName)
      val previousManifestation = previousRelation.manifestation
      val nextManifestation     = nextRelation.manifestation

      val manifestationChange = (previousManifestation, nextManifestation) match {
        case (_: EmbeddedRelationLink, _: RelationTable) =>
          Vector(
            deleteRelation(previousRelation),
            createRelation(nextRelation)
          )
        case (_: RelationTable, _: EmbeddedRelationLink) =>
          Vector(
            deleteRelation(previousRelation),
            createRelation(nextRelation)
          )

        case (previousLink: EmbeddedRelationLink, nextLink: EmbeddedRelationLink) =>
          val previousModel     = previousSchema.getModelByName_!(previousLink.inTableOfModelName)
          val nextModel         = nextSchema.getModelByName_!(nextLink.inTableOfModelName)
          val tableDidNotChange = previousModel.stableIdentifier == nextModel.stableIdentifier

          if (tableDidNotChange) {
            Vector(
              UpdateInlineRelation(project, previousRelation, nextRelation)
            )
          } else {
            Vector(
              deleteRelation(previousRelation),
              createRelation(nextRelation)
            )
          }

        case (_: RelationTable, _: RelationTable) =>
          Vector(
            UpdateRelationTable(project, previousRelation, nextRelation)
          )

        case (p, n) =>
          sys.error(s"Combination $p, $n not supported here") // this can only happen for None + Some combination that must not be possible
      }

      manifestationChange

    case x: EnumMigrationStep =>
      Vector.empty

    case x: UpdateSecrets =>
      Vector.empty
  }

  def deleteRelation(relation: Relation): DeployMutaction = {
    relation.manifestation match {
      case m: EmbeddedRelationLink =>
        val modelA              = relation.modelA
        val modelB              = relation.modelB
        val (model, references) = if (m.inTableOfModelName == modelA.name) (modelA, modelB) else (modelB, modelA)

        DeleteInlineRelation(project, relation, model, references, m.referencingColumn)
      case _ =>
        DeleteRelationTable(project, relation)
    }
  }

  def createRelation(relation: Relation): DeployMutaction = {
    relation.manifestation match {
      case m: EmbeddedRelationLink =>
        val modelA              = relation.modelA
        val modelB              = relation.modelB
        val (model, references) = if (m.inTableOfModelName == modelA.name) (modelA, modelB) else (modelB, modelA)

        CreateInlineRelation(project, relation, model, references, m.referencingColumn)
      case _ =>
        CreateRelationTable(project, relation = relation)
    }
  }
}
