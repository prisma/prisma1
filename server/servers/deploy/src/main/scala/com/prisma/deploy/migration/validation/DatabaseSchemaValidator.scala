package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.{Column, DatabaseSchema, Table}
import com.prisma.shared.models.Manifestations.{EmbeddedRelationLink, RelationTable}
import com.prisma.shared.models.{Field, Model, Relation, Schema}
import com.prisma.utils.boolean.BooleanUtils

trait DatabaseSchemaValidator {
  def check(schema: Schema, databaseSchema: DatabaseSchema): Vector[DeployError]
}

object DatabaseSchemaValidatorImpl extends DatabaseSchemaValidator {
  override def check(schema: Schema, databaseSchema: DatabaseSchema): Vector[DeployError] = {
    DatabaseSchemaValidatorImpl(schema, databaseSchema).check.toVector
  }
}

case class DatabaseSchemaValidatorImpl(schema: Schema, databaseSchema: DatabaseSchema) extends BooleanUtils {
  def check = modelErrors ++ fieldErrors ++ relationErrors

  val modelErrors = schema.models.flatMap { model =>
    table(model).isEmpty.toOption {
      DeployError(model.name, s"Could not find the table for the model ${model.name} in the database.")
    }
  }

  val fieldErrors = {
    val tmp = for {
      model <- schema.models
      field <- model.scalarFields
      _     <- table(model).toVector // only run the validation if the table exists
    } yield {
      column(field) match {
        case Some(column) if field.typeIdentifier != column.typeIdentifier =>
          Some(
            DeployError(
              model.name,
              field.name,
              s"The underlying column for the field ${field.name} has an incompatible type. The field has type `${field.typeIdentifier.userFriendlyTypeName}` and the column has type `${column.typeIdentifier.userFriendlyTypeName}`."
            ))
        case None =>
          Some(DeployError(model.name, field.name, s"Could not find the column for the field ${field.name} in the database."))
        case _ =>
          None
      }
    }

    tmp.flatten
  }

  val relationErrors = schema.relations.flatMap { relation =>
    relation.manifestation match {
      case Some(link: EmbeddedRelationLink) =>
        validateEmbeddedRelationLink(link)

      case Some(relationTable: RelationTable) =>
        validateRelationTable(relation, relationTable)

      case _ => None // can't happen because the new SchemaInferrer always sets it
    }
  }

  private def validateEmbeddedRelationLink(embeddedRelationLink: EmbeddedRelationLink): Option[DeployError] = {
    val model = schema.getModelByName_!(embeddedRelationLink.inTableOfModelName)
    val field = model.relationFields.find(_.dbName == embeddedRelationLink.referencingColumn).get

    column(field) match {
      case None =>
        Some(DeployError(model.name, field.name, s"Could not find the column for the inline relation field ${field.name} in the database."))

      case Some(Column.withForeignKey(fk)) if fk.table != field.relatedModel_!.dbName =>
        Some(
          DeployError(
            model.name,
            field.name,
            s"The column for the inline relation field ${field.name} is not referencing the right table. It should reference the table of model `${field.relatedModel_!.name}` but is referencing the table `${fk.table}`."
          ))

      case Some(Column.withForeignKey(fk)) if fk.column != field.relatedModel_!.dbNameOfIdField_! =>
        Some(
          DeployError(
            model.name,
            field.name,
            s"The column for the inline relation field ${field.name} is not referencing a valid column. Those columns must always reference the column of the id field of related model. So it should reference `${field.relatedModel_!.dbNameOfIdField_!}` instead of `${fk.column}`."
          ))

      case Some(Column.withForeignKey(_)) =>
        None // the FK constraint is fine

      case _ =>
        Some(DeployError(model.name, field.name, s"The column for the inline relation field ${field.name} is missing a foreign key constraint."))
    }
  }

  private def validateRelationTable(relation: Relation, manifestation: RelationTable): Vector[DeployError] = {
    val nameOfLinkTableType = relation.name
    databaseSchema.table(manifestation.table) match {
      case None =>
        Vector(DeployError(nameOfLinkTableType, s"Could not find the table `${manifestation.table}` for the relation `${relation.name}` in the database."))
      case Some(table) =>
        def missingColumn(columnName: String) = table.hasNotColumn(columnName).toOption {
          DeployError(nameOfLinkTableType, columnName, s"The link table ${relation.name} is missing the column `$columnName`.")
        }
        def fkReferenceWrong(columnName: String, relatedModel: Model) = {
          table.column(columnName) match {
            case Some(Column.withForeignKey(fk)) if fk.table != relatedModel.dbName =>
              Some(
                DeployError(
                  nameOfLinkTableType,
                  columnName,
                  s"The column `$columnName` of the relation table `${table.name}` is not referencing the right table. It should reference the table of model `${relatedModel.name}` but is referencing the table `${fk.table}`."
                ))

            case Some(Column.withForeignKey(fk)) if fk.column != relatedModel.dbNameOfIdField_! =>
              Some(
                DeployError(
                  nameOfLinkTableType,
                  columnName,
                  s"The column `$columnName` of the relation table `${table.name}` is not referencing a valid column. Those columns must always reference the column of the id field of the related model. So it should reference `${relatedModel.dbNameOfIdField_!}` instead of `${fk.column}`."
                ))

            case Some(Column.withForeignKey(_)) =>
              None

            case Some(_) =>
              Some(
                DeployError(
                  nameOfLinkTableType,
                  columnName,
                  s"The column `$columnName` for the relation table `${table.name}` is missing a foreign key constraint."
                ))

            case None =>
              None // is already handled by the method above
          }
        }
        val missingIdColumn = manifestation.idColumn.flatMap(missingColumn)
        val errorsOfAColumn = missingColumn(relation.modelAColumn) ++ fkReferenceWrong(relation.modelAColumn, relation.modelA)
        val errorsOfBColumn = missingColumn(relation.modelBColumn) ++ fkReferenceWrong(relation.modelBColumn, relation.modelB)

        (missingIdColumn ++ errorsOfAColumn ++ errorsOfBColumn).toVector
    }
  }

  private def table(model: Model): Option[Table]   = databaseSchema.table(model.dbName)
  private def column(field: Field): Option[Column] = table(field.model).flatMap(_.column(field.dbName))
}
