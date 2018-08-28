package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.MissingBackRelations
import com.prisma.deploy.connector.mongo.database.{MigrationDocument, ProjectDocument}
import com.prisma.shared.models
import com.prisma.shared.models.MigrationStatus.MigrationStatus
import com.prisma.shared.models.{MigrationStep, Schema}
import org.joda.time.DateTime
import org.mongodb.scala.Document
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{JsPath, JsValue, Json}
import play.api.libs.functional.syntax._

object DbMapper {
  import DefaultDocumentReads._
  import com.prisma.utils.json.JsonUtils._
  import com.prisma.shared.models.ProjectJsonFormatter._
  import com.prisma.shared.models.MigrationStepsJsonFormatter._

  implicit val migrationDocumentFormat: DocumentFormat[MigrationDocument] = {
    (
      (JsPath \ "projectId").format[String] and
        (JsPath \ "revision").format[Int] and
        (JsPath \ "schema").format[JsValue] and
        (JsPath \ "functions").format[JsValue] and
        (JsPath \ "status").format[MigrationStatus] and
        (JsPath \ "applied").format[Int] and
        (JsPath \ "rolledBack").format[Int] and
        (JsPath \ "steps").format[JsValue] and
        (JsPath \ "errors").format[JsValue] and
        (JsPath \ "startedAt").formatNullable[DateTime] and
        (JsPath \ "finishedAt").formatNullable[DateTime]
    ).apply(MigrationDocument.apply _, unlift(MigrationDocument.unapply))
  }

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

  def convertToDocument(migration: models.Migration): Document = {
    val schemaJson         = Json.toJson(migration.schema)
    val functionsJson      = Json.toJson(migration.functions)
    val migrationStepsJson = Json.toJson(migration.steps)
    val errorsJson         = Json.toJson(migration.errors)

    val migrationDocument = MigrationDocument(
      projectId = migration.projectId,
      revision = migration.revision,
      schema = schemaJson,
      functions = functionsJson,
      status = migration.status,
      applied = migration.applied,
      rolledBack = migration.rolledBack,
      steps = migrationStepsJson,
      errors = errorsJson,
      startedAt = migration.startedAt,
      finishedAt = migration.finishedAt
    )
    migrationDocumentFormat.writes(migrationDocument)
  }

  def convertToDocument(migrationDocument: MigrationDocument): Document = migrationDocumentFormat.writes(migrationDocument)

  def convertToProjectModel(project: Document, migration: Document): models.Project = {
    val projectDocument   = project.readAs[ProjectDocument](projectReads).get
    val migrationDocument = migration.readAs[MigrationDocument](migrationDocumentFormat).get
    convertToProjectModel(projectDocument, migrationDocument)
  }

  def convertToProjectModel(projectDocument: ProjectDocument, migrationDocument: MigrationDocument): models.Project = {
    models.Project(
      id = projectDocument.id,
      ownerId = "",
      revision = migrationDocument.revision,
      schema = Schema.empty, //convertSchema(migration.schema),
      secrets = projectDocument.secrets.as[Vector[String]],
      allowQueries = projectDocument.allowQueries,
      allowMutations = projectDocument.allowMutations,
      functions = migrationDocument.functions.as[List[models.Function]]
    )
  }

  def convertToMigrationModel(migration: Document): models.Migration = {
    val migrationDocument = migration.readAs[MigrationDocument](migrationDocumentFormat).get
    models.Migration(
      projectId = migrationDocument.projectId,
      revision = migrationDocument.revision,
      schema = convertSchema(migrationDocument.schema),
      functions = migrationDocument.functions.as[Vector[models.Function]],
      status = migrationDocument.status,
      applied = migrationDocument.applied,
      rolledBack = migrationDocument.rolledBack,
      steps = migrationDocument.steps.as[Vector[MigrationStep]],
      errors = migrationDocument.errors.as[Vector[String]],
      startedAt = migrationDocument.startedAt,
      finishedAt = migrationDocument.finishedAt
    )
  }

  private def convertSchema(schema: JsValue): Schema = {
    val schemaWithMissingBackRelations = schema.as[Schema]
    MissingBackRelations.add(schemaWithMissingBackRelations)
  }
}
