package com.prisma.deploy.connector.mongo.impl

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector.MissingBackRelations
import com.prisma.shared.models
import com.prisma.shared.models.{Migration, ProjectManifestation}
import com.prisma.utils.mongo.{DocumentFormat, JsonBsonConversion, MongoExtensions}
import org.mongodb.scala.Document
import play.api.libs.functional.syntax._
import play.api.libs.json.JsPath

object DbMapper extends JsonBsonConversion with MongoExtensions {
  import com.prisma.shared.models.MigrationStepsJsonFormatter._
  import com.prisma.shared.models.ProjectJsonFormatter._

  implicit val migrationDocumentFormat: DocumentFormat[Migration] = playFormatToDocumentFormat(migrationFormat)

  implicit val projectDocumentFormat: DocumentFormat[ProjectDocument] = playFormatToDocumentFormat {
    (
      (JsPath \ "id").format[String] and
        (JsPath \ "secrets").format[Vector[String]] and
        (JsPath \ "allowQueries").format[Boolean] and
        (JsPath \ "allowMutations").format[Boolean]
    )(ProjectDocument.apply _, unlift(ProjectDocument.unapply))
  }

  def convertToDocument(project: models.Project): Document = {
    val projectDocument = ProjectDocument(
      project.id,
      project.secrets,
      project.allowQueries,
      project.allowMutations
    )
    projectDocumentFormat.writes(projectDocument)
  }

  def convertToDocument(migration: models.Migration): Document = migrationDocumentFormat.writes(migration)

  def convertToProjectModel(project: Document, migration: models.Migration, manifestation: ProjectManifestation): models.Project = {
    val projectDocument = project.as[ProjectDocument]
    convertToProjectModel(projectDocument, migration, manifestation)
  }

  def convertToProjectModel(projectDocument: ProjectDocument, migration: models.Migration, manifestation: ProjectManifestation): models.Project = {
    val schemaWithMissingBackRelations = migration.schema
    val schemaWithAddedBackRelations   = MissingBackRelations.add(schemaWithMissingBackRelations)

    models.Project(
      id = projectDocument.id,
      secrets = projectDocument.secrets,
      allowQueries = projectDocument.allowQueries,
      allowMutations = projectDocument.allowMutations,
      revision = migration.revision,
      schema = schemaWithAddedBackRelations,
      functions = migration.functions.toList,
      manifestation = manifestation,
      rawDataModel = migration.rawDataModel
    )
  }

  def convertToMigrationModel(migrationDocument: Document): models.Migration = migrationDocument.as[Migration]
}

case class ProjectDocument(
    id: String,
    secrets: Vector[String],
    allowQueries: Boolean,
    allowMutations: Boolean
)
