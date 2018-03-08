package com.prisma.deploy.migration

import com.prisma.deploy.migration.mutactions._
import com.prisma.shared.models._

case class MigrationStepMapperImpl(projectId: String) extends MigrationStepMapper {
  def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Option[DeployMutaction] = step match {
    case x: CreateModel =>
      Some(CreateModelTable(projectId, x.name))

    case x: DeleteModel =>
      val model                = previousSchema.getModelByName_!(x.name)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Some(DeleteModelTable(projectId, x.name, scalarListFieldNames))

    case x: UpdateModel =>
      val model                = nextSchema.getModelByName_!(x.newName)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Some(RenameTable(projectId = projectId, previousName = x.name, nextName = x.newName, scalarListFieldsNames = scalarListFieldNames))

    case x: CreateField =>
      val model = nextSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)
      if (ReservedFields.isReservedFieldName(field.name) || !field.isScalar) {
        None
      } else {
        if (field.isList) {
          Some(CreateScalarListTable(projectId, model.name, field.name, field.typeIdentifier))
        } else {
          Some(CreateColumn(projectId, model, field))
        }
      }

    case x: DeleteField =>
      val model = previousSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)
      if (field.isList && !field.isRelation) {
        Some(DeleteScalarListTable(projectId, model.name, field.name, field.typeIdentifier))
      } else if (field.isScalar) {
        // TODO: add test case for not deleting columns for relation fields
        Some(DeleteColumn(projectId, model, field))
      } else {
        None
      }

    case x: UpdateField =>
      val model         = nextSchema.getModelByName_!(x.model)
      val nextField     = nextSchema.getFieldByName_!(x.model, x.finalName)
      val previousField = previousSchema.getFieldByName_!(x.model, x.name)

      if (previousField.isList) {
        // todo: also handle changing to/from scalar list
        Some(UpdateScalarListTable(projectId, model, model, previousField, nextField))
      } else if (previousField.isScalar) {
        Some(UpdateColumn(projectId, model, previousField, nextField))
      } else {
        None
      }

    case x: EnumMigrationStep =>
      None

    case x: CreateRelation =>
      val relation = nextSchema.getRelationByName_!(x.name)
      Some(CreateRelationTable(projectId, nextSchema, relation))

    case x: DeleteRelation =>
      val relation = previousSchema.getRelationByName_!(x.name)
      Some(DeleteRelationTable(projectId, nextSchema, relation))

    case x: UpdateRelation =>
      x.newName.map { newName =>
        val previousRelation = previousSchema.getRelationByName_!(x.name)
        val nextRelation     = nextSchema.getRelationByName_!(newName)
        RenameTable(projectId = projectId, previousName = previousRelation.id, nextName = nextRelation.id, scalarListFieldsNames = Vector.empty)
      }
  }
}
