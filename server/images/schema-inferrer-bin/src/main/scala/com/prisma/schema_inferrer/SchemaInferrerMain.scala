package com.prisma.schema_inferrer
import com.prisma.deploy.connector.{FieldRequirementsInterface, InferredTables}
import com.prisma.deploy.migration.inference.{SchemaInferrer, SchemaMapping}
import com.prisma.deploy.migration.validation.DataModelValidatorImpl
import com.prisma.shared.models.{ConnectorCapabilities, Schema}
import play.api.libs.json.{Json, Reads}

import scala.io.StdIn

object SchemaInferrerMain {
  case class Input(dataModel: String, previousSchema: Schema)

  import com.prisma.shared.models.ProjectJsonFormatter._

  implicit lazy val inputReads: Reads[Input] = Json.reads[Input]

  def main(args: Array[String]): Unit = {
    val inputAsJson = Json.parse(StdIn.readLine())
    val input       = inputAsJson.as[Input]

    val capabilities     = ConnectorCapabilities.mysqlPrototype
    val validationResult = DataModelValidatorImpl.validate(input.dataModel, FieldRequirementsInterface.empty, capabilities)
//    println(validationResult)
    val schema = SchemaInferrer(capabilities).infer(input.previousSchema, SchemaMapping.empty, validationResult.get.dataModel, InferredTables.empty)
//    println(schema)
//    println("------------------------------------------------------")
    println(Json.toJson(schema))
  }

}
