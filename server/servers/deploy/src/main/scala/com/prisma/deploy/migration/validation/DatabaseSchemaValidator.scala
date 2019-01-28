package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.{Column, DatabaseSchema, Table}
import com.prisma.shared.models.FieldBehaviour.{ScalarListBehaviour, ScalarListStrategy}
import com.prisma.shared.models.Manifestations.{EmbeddedRelationLink, RelationTable}
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models._
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
  val TI = TypeIdentifier

  def check = modelErrors ++ fieldErrors ++ relationErrors

  val modelErrors = schema.models.flatMap { model =>
    table(model).isEmpty.toOption {
      DeployError(model.name, s"Could not find the table for the model `${model.name}` in the database.")
    }
  }

  val fieldErrors = {
    val tmp = for {
      model <- schema.models
      field <- model.scalarFields
      _     <- table(model).toVector // only run the validation if the table exists
    } yield {
      if (field.isScalarNonList) {
        validateScalarNonListField(field).toVector
      } else {
        validateScalarListField(field)
      }
    }

    tmp.flatten
  }

  def validateScalarNonListField(field: ScalarField) = {
    require(field.isScalarNonList)
    val errorMessage = column(field) match {
      case Some(column) if !typesAreCompatible(field.typeIdentifier, column.typeIdentifier) =>
        Some(
          s"The underlying column for the field `${field.name}` has an incompatible type. The field has type `${field.typeIdentifier.userFriendlyTypeName}` and the column has type `${column.typeIdentifier.userFriendlyTypeName}`."
        )
      case Some(column) if column.isRequired && !field.isRequired =>
        Some(fieldMustBeRequired(field))
      case Some(column) if !column.isRequired && field.isRequired =>
        Some(fieldMustBeOptional(field))
      case None =>
        Some(s"Could not find the column for the field `${field.name}` in the database.")
      case _ =>
        None
    }

    errorMessage.map { msg =>
      DeployError(field.model.name, field.name, msg)
    }
  }

  def validateScalarListField(field: ScalarField) = {
    require(field.isScalarList)
    val errors = field.behaviour match {
      case Some(ScalarListBehaviour(ScalarListStrategy.Relation)) =>
        validateScalarListFieldWithRelationStrategy(field)
      case _ =>
        Vector.empty
    }

    errors.map(err => DeployError(field.model.name, field.name, err)).toVector
  }

  def validateScalarListFieldWithRelationStrategy(field: ScalarField) = {
    val tableName = field.model.dbName + "_" + field.dbName
    databaseSchema.table(tableName) match {
      case None =>
        Vector(s"Could not find the underlying table for the scalar list field `${field.name}`.")
      case Some(table) =>
        def columnError(columnName: String, typeIdentifier: TypeIdentifier) = table.column(columnName) match {
          case None =>
            Some(s"The underlying table for the scalar list field `${field.name}` is missing the required column `$columnName`.")
          case Some(column) if !typesAreCompatible(typeIdentifier, column.typeIdentifier) =>
            Some(
              s"The column `$columnName` in the underlying table for the scalar list field `${field.name}` has the wrong type. It has the type `${column.typeIdentifier.userFriendlyTypeName}` but it should have the type `${typeIdentifier.userFriendlyTypeName}`.")
          case _ =>
            None
        }
        columnError("nodeId", TI.String) ++ columnError("position", TI.Int) ++ columnError("value", field.typeIdentifier)
    }
  }

  def typesAreCompatible(field: TypeIdentifier, column: TypeIdentifier): Boolean = {
    val TI = TypeIdentifier
    (field, column) match {
      case (TI.Cuid, TI.String) => true
      case (TI.Json, TI.String) => true
      case _                    => field == column
    }
  }

  val relationErrors = schema.relations.flatMap { relation =>
    relation.manifestation match {
      case Some(link: EmbeddedRelationLink)   => validateEmbeddedRelationLink(link)
      case Some(relationTable: RelationTable) => validateRelationTable(relation, relationTable)
      case _                                  => None // can't happen because the new SchemaInferrer always sets it
    }
  }

  private def validateEmbeddedRelationLink(embeddedRelationLink: EmbeddedRelationLink): Option[DeployError] = {
    val model = schema.getModelByName_!(embeddedRelationLink.inTableOfModelName)
    val field = model.relationFields.find(_.dbName == embeddedRelationLink.referencingColumn).get

    val errorMessage = column(field) match {
      case None =>
        Some(s"Could not find the column for the inline relation field `${field.name}` in the database.")
      case Some(Column.withForeignKey(fk)) if fk.table != field.relatedModel_!.dbName =>
        Some(
          s"The column for the inline relation field `${field.name}` is not referencing the right table. It should reference the table of model `${field.relatedModel_!.name}` but is referencing the table `${fk.table}`."
        )
      case Some(Column.withForeignKey(fk)) if fk.column != field.relatedModel_!.dbNameOfIdField_! =>
        Some(
          s"The column for the inline relation field `${field.name}` is not referencing a valid column. Those columns must always reference the column of the id field of related model. So it should reference `${field.relatedModel_!.dbNameOfIdField_!}` instead of `${fk.column}`."
        )
      case Some(column) if column.isRequired && !field.isRequired => Some(fieldMustBeRequired(field))
      case Some(column) if !column.isRequired && field.isRequired => Some(fieldMustBeOptional(field))
      case Some(Column.withForeignKey(_))                         => None // the FK constraint is fine
      case _                                                      => Some(s"The column for the inline relation field `${field.name}` is missing a foreign key constraint.")
    }
    errorMessage.map { msg =>
      DeployError(model.name, field.name, msg)
    }
  }

  private def validateRelationTable(relation: Relation, manifestation: RelationTable): Vector[DeployError] = {
    val nameOfLinkTableType = relation.name
    databaseSchema.table(manifestation.table) match {
      case None =>
        Vector(DeployError(nameOfLinkTableType, s"Could not find the table `${manifestation.table}` for the relation `${relation.name}` in the database."))
      case Some(table) =>
        def missingColumn(columnName: String) = table.hasNotColumn(columnName).toOption {
          DeployError(nameOfLinkTableType, columnName, s"The link table `${relation.name}` is missing the column `$columnName`.")
        }
        def fkReferenceWrong(columnName: String, relatedModel: Model) = {
          val errorMessage = table.column(columnName) match {
            case Some(Column.withForeignKey(fk)) if fk.table != relatedModel.dbName =>
              Some(
                s"The column `$columnName` of the relation table `${table.name}` is not referencing the right table. It should reference the table of model `${relatedModel.name}` but is referencing the table `${fk.table}`."
              )
            case Some(Column.withForeignKey(fk)) if fk.column != relatedModel.dbNameOfIdField_! =>
              Some(
                s"The column `$columnName` of the relation table `${table.name}` is not referencing a valid column. Those columns must always reference the column of the id field of the related model. So it should reference `${relatedModel.dbNameOfIdField_!}` instead of `${fk.column}`."
              )
            case Some(Column.withForeignKey(_)) =>
              None
            case Some(_) =>
              Some(s"The column `$columnName` for the relation table `${table.name}` is missing a foreign key constraint.")
            case None =>
              None // is already handled by the method above
          }

          errorMessage.map { msg =>
            DeployError(nameOfLinkTableType, columnName, msg)
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

  private def fieldMustBeRequired(field: Field) = {
    s"The underlying column for the field `${field.name}` is required but the field is declared optional. Please declare it as required: `${field.name}: ${userFriendlyTypeName(field)}!`."
  }

  private def fieldMustBeOptional(field: Field) = {
    s"The underlying column for the field `${field.name}` is optional but the field is declared required. Please declare it as optional by removing the `!`: `${field.name}: ${userFriendlyTypeName(field)}`."
  }

  private def userFriendlyTypeName(field: Field) = field match {
    case f: ScalarField   => f.typeIdentifier.userFriendlyTypeName
    case f: RelationField => f.relatedModel_!.name
  }
}
