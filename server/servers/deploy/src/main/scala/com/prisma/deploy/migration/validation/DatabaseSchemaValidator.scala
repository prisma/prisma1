package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.{Column, DatabaseSchema, Table}
import com.prisma.shared.models.{Field, Model, Schema}
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
  def check = modelErrors ++ fieldErrors

  val modelErrors = schema.models.flatMap { model =>
    val missingTableError = table(model).isEmpty.toOption {
      DeployError(model.name, s"Could not find the table for the model ${model.name} in the database.")
    }
    missingTableError
  }

  val fieldErrors = {
    val tmp = for {
      model <- schema.models
      field <- model.fields
    } yield {
      (table(model).isDefined && column(field).isEmpty).toOption {
        DeployError(model.name, field.name, "asjdfkl")
      }
    }

    tmp.flatten
  }

  private def table(model: Model): Option[Table]   = databaseSchema.table(model.dbName)
  private def column(field: Field): Option[Column] = table(field.model).flatMap(_.column(field.dbName))
}
