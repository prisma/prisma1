package com.prisma.shared.schema_dsl

import com.prisma.config.ConfigLoader
import com.prisma.deploy.connector.{DeployConnector, MissingBackRelations}
import com.prisma.deploy.migration.inference.{SchemaInferrer, SchemaMapping}
import com.prisma.deploy.migration.validation.{DataModelValidator, DataModelValidatorImpl}
import com.prisma.shared.models.ConnectorCapability.LegacyDataModelCapability
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils
import org.scalactic.{Bad, Good}
import org.scalatest.Suite

object SchemaDsl extends AwaitUtils {

  def fromStringV11ForExistingDatabase(id: String = TestIds.testProjectId)(sdlString: String)(implicit deployConnector: DeployConnector): Project = {
    val actualCapas = deployConnector.capabilities.capabilities.filter(_ != LegacyDataModelCapability)
    val project = fromString(
      id = id,
      capabilities = ConnectorCapabilities(actualCapas),
      dataModelValidator = DataModelValidatorImpl,
      emptyBaseSchema = Schema.empty
    )(sdlString)
    project.copy(manifestation = ProjectManifestation.empty) // we don't want the altered manifestation here
  }

  def fromStringV11(id: String = TestIds.testProjectId)(sdlString: String)(implicit deployConnector: DeployConnector, suite: Suite): Project = {
    val actualCapas = deployConnector.capabilities.capabilities.filter(_ != LegacyDataModelCapability)
    fromString(
      id = projectId(suite),
      capabilities = ConnectorCapabilities(actualCapas),
      dataModelValidator = DataModelValidatorImpl,
      emptyBaseSchema = Schema.emptyV11
    )(sdlString.stripMargin)
  }

  def fromStringV11Capabilities(
      capabilities: Set[ConnectorCapability] = Set.empty
  )(sdlString: String)(implicit suite: Suite): Project = {
    fromString(
      id = projectId(suite),
      capabilities = ConnectorCapabilities(capabilities),
      dataModelValidator = DataModelValidatorImpl,
      emptyBaseSchema = Schema.emptyV11
    )(sdlString.stripMargin)
  }

  private def projectId(suite: Suite): String = {
    // GetFieldFromSQLUniqueException blows up if we generate longer names, since we then exceed the postgres limits for constraint names
    // todo: actually fix GetFieldFromSQLUniqueException instead
    val nameThatMightBeTooLong = suite.getClass.getSimpleName
    nameThatMightBeTooLong.substring(0, Math.min(32, nameThatMightBeTooLong.length))
  }

  private def fromString(
      id: String,
      capabilities: ConnectorCapabilities,
      dataModelValidator: DataModelValidator,
      emptyBaseSchema: Schema
  )(sdlString: String): Project = {
    val emptySchemaMapping = SchemaMapping.empty

    val prismaSdl = dataModelValidator.validate(sdlString, capabilities) match {
      case Good(result) =>
        result.dataModel
      case Bad(errors) =>
        sys.error(
          s"""Encountered the following errors during schema validation. Please fix:
           |${errors.mkString("\n")}
         """.stripMargin
        )
    }

    val schema                 = SchemaInferrer(capabilities).infer(emptyBaseSchema, emptySchemaMapping, prismaSdl)
    val withBackRelationsAdded = MissingBackRelations.add(schema)
    val manifestation = ConfigLoader.load().databases.head.connector match {
      case x if x == "postgres" => ProjectManifestation(database = Some(id + "_DB"), schema = Some(id + "_S"), x)
      case y                    => ProjectManifestation(database = Some(id + "_DB"), schema = None, y)
    }
    TestProject().copy(id = id, schema = withBackRelationsAdded, manifestation = manifestation)
  }
}
