package com.prisma.deploy.connector

import com.prisma.shared.models.ConnectorCapability.RelationLinkListCapability
import com.prisma.shared.models.Manifestations.{EmbeddedRelationLink, RelationTable}
import com.prisma.shared.models._

case class MigrationStepMapperImpl(projectId: String) extends MigrationStepMapper {
  def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Vector[DeployMutaction] = step match {
    case x: CreateModel =>
      val model = nextSchema.getModelByName_!(x.name)
      Vector(CreateModelTable(projectId, model))

    case x: DeleteModel =>
      val model                = previousSchema.getModelByName_!(x.name)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Vector(DeleteModelTable(projectId, model, model.dbNameOfIdField_!, scalarListFieldNames))

    case x: UpdateModel =>
      val model                = nextSchema.getModelByName(x.newName).getOrElse(nextSchema.getModelByName_!(x.newName.substring(2)))
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Vector(RenameTable(projectId = projectId, previousName = x.name, nextName = x.newName, scalarListFieldsNames = scalarListFieldNames))

    case x: CreateField =>
      val model = nextSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)

      field match {
        case _ if ReservedFields.idFieldName == field.name => Vector.empty
        case _: RelationField                              => Vector.empty
        case f: ScalarField if field.isScalarList          => Vector(CreateScalarListTable(projectId, model, f))
        case f: ScalarField if field.isScalarNonList       => Vector(CreateColumn(projectId, model, f))
      }

    case x: DeleteField =>
      val model = previousSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)

      field match {
        case _ if field.isRelation                   => Vector.empty
        case f: ScalarField if field.isScalarList    => Vector(DeleteScalarListTable(projectId, model, f))
        case f: ScalarField if field.isScalarNonList => Vector(DeleteColumn(projectId, model, f))
      }

    case x: UpdateField =>
      val oldModel = previousSchema.getModelByName_!(x.model)
      val newModel = nextSchema.getModelByName_!(x.newModel)
      val next     = nextSchema.getFieldByName_!(x.newModel, x.finalName)
      val previous = previousSchema.getFieldByName_!(x.model, x.name)

      lazy val createColumn          = CreateColumn(projectId, oldModel, next.asInstanceOf[ScalarField])
      lazy val updateColumn          = UpdateColumn(projectId, oldModel, previous.asInstanceOf[ScalarField], next.asInstanceOf[ScalarField])
      lazy val deleteColumn          = DeleteColumn(projectId, oldModel, previous.asInstanceOf[ScalarField])
      lazy val createScalarListTable = CreateScalarListTable(projectId, oldModel, next.asInstanceOf[ScalarField])
      lazy val deleteScalarListTable = DeleteScalarListTable(projectId, oldModel, previous.asInstanceOf[ScalarField])
      lazy val updateScalarListTable = UpdateScalarListTable(projectId, oldModel, newModel, previous.asInstanceOf[ScalarField], next.asInstanceOf[ScalarField])

      () match {
        case _ if previous.isRelation && next.isRelation                                                             => Vector.empty
        case _ if previous.isRelation && next.isScalarNonList                                                        => Vector(createColumn)
        case _ if previous.isRelation && next.isScalarList                                                           => Vector(createScalarListTable)
        case _ if previous.isScalarList && next.isScalarNonList                                                      => Vector(createColumn, deleteScalarListTable)
        case _ if previous.isScalarList && next.isRelation                                                           => Vector(deleteScalarListTable)
        case _ if previous.isScalarNonList && next.isScalarList                                                      => Vector(createScalarListTable, deleteColumn)
        case _ if previous.isScalarNonList && next.isRelation                                                        => Vector(deleteColumn)
        case _ if previous.isScalarNonList && next.isScalarNonList && previous.typeIdentifier == next.typeIdentifier => Vector(updateColumn)
        case _ if previous.isScalarList && next.isScalarList && previous.typeIdentifier == next.typeIdentifier       => Vector(updateScalarListTable)
        case _ if previous.isScalarNonList && next.isScalarNonList                                                   => Vector(deleteColumn, createColumn)
        case _ if previous.isScalarList && next.isScalarList                                                         => Vector(deleteScalarListTable, createScalarListTable)
      }

    case x: CreateRelation =>
      val relation = nextSchema.getRelationByName_!(x.name)
      val mutaction = relation.manifestation match {
        case Some(m: EmbeddedRelationLink) =>
          val modelA              = relation.modelA
          val modelB              = relation.modelB
          val (model, references) = if (m.inTableOfModelName == modelA.name) (modelA, modelB) else (modelB, modelA)

          CreateInlineRelation(projectId, model, references, m.referencingColumn)
        case _ =>
          CreateRelationTable(projectId, nextSchema, relation = relation)
      }
      Vector(mutaction)

    case x: DeleteRelation =>
      val relation = previousSchema.getRelationByName_!(x.name)
      val mutaction = relation.manifestation match {
        case Some(m: EmbeddedRelationLink) =>
          val modelA              = relation.modelA
          val modelB              = relation.modelB
          val (model, references) = if (m.inTableOfModelName == modelA.name) (modelA, modelB) else (modelB, modelA)

          DeleteInlineRelation(projectId, model, references, m.referencingColumn)
        case _ =>
          DeleteRelationTable(projectId, nextSchema, relation)
      }
      Vector(mutaction)

    case x: UpdateRelation =>
      val previousRelation      = previousSchema.getRelationByName_!(x.name)
      val nextRelation          = nextSchema.getRelationByName_!(x.finalName)
      val previousManifestation = previousRelation.manifestation.get
      val nextManifestation     = nextRelation.manifestation.get

      val manifestationChange = (previousManifestation, nextManifestation) match {
        case (p: EmbeddedRelationLink, _: RelationTable) =>
          Vector(
            DeleteInlineRelation(projectId, p.inTableOfModel(previousRelation), p.referencedModel(previousRelation), p.referencingColumn),
            CreateRelationTable(projectId, nextSchema, nextRelation)
          )
        case (p: EmbeddedRelationLink, n: EmbeddedRelationLink) =>
          Vector(
            DeleteInlineRelation(projectId, p.inTableOfModel(previousRelation), p.referencedModel(previousRelation), p.referencingColumn),
            CreateInlineRelation(projectId, n.inTableOfModel(nextRelation), n.referencedModel(nextRelation), n.referencingColumn)
          )
        case (_: RelationTable, _: RelationTable) =>
          // FIXME: should test what happens if the relation does not only contain renames but is using different models
          Vector(
            UpdateRelationTable(projectId, previousRelation, nextRelation)
          )
        case (p, n) => sys.error(s"Combination $p, $n not supported here")
      }

      manifestationChange

    case x: EnumMigrationStep =>
      Vector.empty

    case x: UpdateSecrets =>
      Vector.empty
  }
}
