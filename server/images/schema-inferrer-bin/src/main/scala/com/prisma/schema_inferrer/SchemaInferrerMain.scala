package com.prisma.schema_inferrer
import com.prisma.deploy.connector.{FieldRequirementsInterface, InferredTables}
import com.prisma.deploy.migration.inference.{SchemaInferrer, SchemaMapping}
import com.prisma.deploy.migration.validation.DataModelValidatorImpl
import com.prisma.shared.models.{ConnectorCapabilities, Schema}
import play.api.libs.json.{Json, Reads}

import scala.io.StdIn

object SchemaInferrerMain {
  case class Input(dataModel: String)

  import com.prisma.shared.models.ProjectJsonFormatter._

  implicit lazy val inputReads: Reads[Input] = Json.reads[Input]

  def main(args: Array[String]): Unit = {
    val line        = StdIn.readLine()
    val inputAsJson = Json.parse(line)
    val input       = inputAsJson.as[Input]

    val (capabilities, emptySchema) =
      if (line.contains("@id")) { // todo doesn't work as intended,
        (ConnectorCapabilities.mysqlPrototype, Schema.emptyV11)
      } else {
        (ConnectorCapabilities.mysql, Schema.empty)
      }

    val validationResult = DataModelValidatorImpl.validate(input.dataModel, FieldRequirementsInterface.empty, capabilities)
    val schema           = SchemaInferrer(capabilities).infer(emptySchema, SchemaMapping.empty, validationResult.get.dataModel, InferredTables.empty)

    println(Json.toJson(schema))
  }
}
