package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.mongo.database.ProjectDocument
import com.prisma.shared.models
import com.prisma.shared.models.Migration
import com.prisma.utils.mongo.{DocumentFormat, DocumentReads, JsonBsonConversion, MongoExtensions}
import org.mongodb.scala.Document
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, Json}

object DbMapper extends JsonBsonConversion with MongoExtensions {
  import com.prisma.shared.models.MigrationStepsJsonFormatter._
  import com.prisma.shared.models.ProjectJsonFormatter._

  implicit val migrationDocumentFormat: DocumentFormat[Migration] = migrationFormat

  implicit val projectReads: DocumentReads[ProjectDocument] = {
    (
      (JsPath \ "id").read[String] and
        (JsPath \ "secrets").read[JsValue] and
        (JsPath \ "allowQueries").read[Boolean] and
        (JsPath \ "allowMutations").read[Boolean] and
        (JsPath \ "functions").read[JsValue]
    )(ProjectDocument.apply _)
  }

  def convertToDocument(project: models.Project): Document = {
    val secretsJson   = Json.toJson(project.secrets)
    val functionsJson = Json.toJson(project.functions)

    val projectDocument = ProjectDocument(
      id = project.id,
      secretsJson,
      project.allowQueries,
      project.allowMutations,
      functionsJson
    )
    Document(
      "id"             -> project.id,
      "secrets"        -> project.secrets,
      "allowQueries"   -> project.allowQueries,
      "allowMutations" -> project.allowMutations,
      "functions"      -> jsonToBson(projectDocument.functions)
    )
  }

  def convertToDocument(migration: models.Migration): Document = migrationDocumentFormat.writes(migration)

  def convertToProjectModel(project: Document, migration: models.Migration): models.Project = {
    val projectDocument = project.as[ProjectDocument](projectReads)
    convertToProjectModel(projectDocument, migration)
  }

  def convertToProjectModel(projectDocument: ProjectDocument, migration: models.Migration): models.Project = {
    models.Project(
      id = projectDocument.id,
      ownerId = "",
      revision = migration.revision,
      schema = migration.schema,
      secrets = projectDocument.secrets.as[Vector[String]],
      allowQueries = projectDocument.allowQueries,
      allowMutations = projectDocument.allowMutations,
      functions = migration.functions.toList
    )
  }

  def convertToMigrationModel(migrationDocument: Document): models.Migration = {
    val migration = migrationDocument.as[Migration]
    models.Migration(
      projectId = migration.projectId,
      revision = migration.revision,
      schema = migration.schema,
      functions = migration.functions,
      status = migration.status,
      applied = migration.applied,
      rolledBack = migration.rolledBack,
      steps = migration.steps,
      errors = migration.errors,
      startedAt = migration.startedAt,
      finishedAt = migration.finishedAt
    )
  }
}
