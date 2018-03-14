package com.prisma.deploy.connector

import com.prisma.shared.models._

case class MigrationStepMapperImpl(projectId: String) extends MigrationStepMapper {
  def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Vector[DeployMutaction] = step match {
    case x: CreateModel =>
      Vector(CreateModelTable(projectId, x.name))

    case x: DeleteModel =>
      val model                = previousSchema.getModelByName_!(x.name)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Vector(DeleteModelTable(projectId, x.name, scalarListFieldNames))

    case x: UpdateModel =>
      val model                = nextSchema.getModelByName_!(x.newName)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Vector(RenameTable(projectId = projectId, previousName = x.name, nextName = x.newName, scalarListFieldsNames = scalarListFieldNames))

    case x: CreateField =>
      val model = nextSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)

      () match {
        case _ if ReservedFields.reservedFieldNames.contains(field.name) => Vector.empty
        case _ if field.isRelation                                       => Vector.empty
        case _ if field.isScalarList                                     => Vector(CreateScalarListTable(projectId, model.name, field.name, field.typeIdentifier))
        case _ if field.isScalarNonList                                  => Vector(CreateColumn(projectId, model, field))
      }

    case x: DeleteField =>
      val model = previousSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)

      () match {
        case _ if field.isRelation      => Vector.empty
        case _ if field.isScalarList    => Vector(DeleteScalarListTable(projectId, model.name, field.name, field.typeIdentifier))
        case _ if field.isScalarNonList => Vector(DeleteColumn(projectId, model, field))
      }

    case x: UpdateField =>
      val model    = nextSchema.getModelByName_!(x.model)
      val next     = nextSchema.getFieldByName_!(x.model, x.finalName)
      val previous = previousSchema.getFieldByName_!(x.model, x.name)

      lazy val createColumn          = CreateColumn(projectId, model, next)
      lazy val updateColumn          = UpdateColumn(projectId, model, previous, next)
      lazy val deleteColumn          = DeleteColumn(projectId, model, previous)
      lazy val createScalarListTable = CreateScalarListTable(projectId, model.name, next.name, next.typeIdentifier)
      lazy val updateScalarListTable = UpdateScalarListTable(projectId, model, model, previous, next)
      lazy val deleteScalarListTable = DeleteScalarListTable(projectId, model.name, previous.name, previous.typeIdentifier)

      () match {
        case _ if previous.isRelation && next.isRelation           => Vector.empty
        case _ if previous.isRelation && next.isScalarNonList      => Vector(createColumn)
        case _ if previous.isRelation && next.isScalarList         => Vector(createScalarListTable)
        case _ if previous.isScalarList && next.isScalarList       => Vector(updateScalarListTable)
        case _ if previous.isScalarList && next.isScalarNonList    => Vector(createColumn, deleteScalarListTable)
        case _ if previous.isScalarList && next.isRelation         => Vector(deleteScalarListTable)
        case _ if previous.isScalarNonList && next.isScalarNonList => Vector(updateColumn)
        case _ if previous.isScalarNonList && next.isScalarList    => Vector(createScalarListTable, deleteColumn)
        case _ if previous.isScalarNonList && next.isRelation      => Vector(deleteColumn)
      }

    case x: CreateRelation =>
      val relation = nextSchema.getRelationByName_!(x.name)
      Vector(CreateRelationTable(projectId, nextSchema, relation))

    case x: DeleteRelation =>
      val relation = previousSchema.getRelationByName_!(x.name)
      Vector(DeleteRelationTable(projectId, nextSchema, relation))

    case x: UpdateRelation =>
      x.newName.map { newName =>
        val previousRelation = previousSchema.getRelationByName_!(x.name)
        val nextRelation     = nextSchema.getRelationByName_!(newName)
        RenameTable(projectId = projectId, previousName = previousRelation.id, nextName = nextRelation.id, scalarListFieldsNames = Vector.empty)
      }.toVector

    case x: EnumMigrationStep =>
      Vector.empty

    case x: UpdateSecrets =>
      Vector.empty
  }
}
