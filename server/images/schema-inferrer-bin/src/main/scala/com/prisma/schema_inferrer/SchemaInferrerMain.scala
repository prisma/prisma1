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
    println("WURST")
    val line = StdIn.readLine()
    println(s"READ: $line")

    val inputAsJson = Json.parse(line)
    val input       = inputAsJson.as[Input]

    val (capabilities, emptySchema) =
      if (line.contains("@id")) {
        println("IS V1.1")
        (ConnectorCapabilities.mysqlPrototype, Schema.emptyV11)
      } else {
        println("IS V1.0")
        (ConnectorCapabilities.mysql, Schema.empty)
      }

    val validationResult = DataModelValidatorImpl.validate(input.dataModel, FieldRequirementsInterface.empty, capabilities)
    val schema           = SchemaInferrer(capabilities).infer(emptySchema, SchemaMapping.empty, validationResult.get.dataModel, InferredTables.empty)

    println(Json.toJson(schema))
  }
}
