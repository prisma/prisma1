package com.prisma.deploy.migration.validation
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.shared.models.Schema

trait DatabaseSchemaValidator {
  def check(schema: Schema, databaseSchema: DatabaseSchema): Vector[DeployError]
}

object DatabaseSchemaValidatorImpl extends DatabaseSchemaValidator {
  override def check(schema: Schema, databaseSchema: DatabaseSchema): Vector[DeployError] = {
    DatabaseSchemaValidatorImpl(schema, databaseSchema).check
  }
}

case class DatabaseSchemaValidatorImpl(schema: Schema, databaseSchema: DatabaseSchema) {
  def check: Vector[DeployError] = Vector.empty
}
