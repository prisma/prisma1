//package com.prisma.migration
//
//import com.prisma.deploy.migration._
//import com.prisma.shared.models._
//import com.prisma.shared.schema_dsl.SchemaDsl
//import org.scalactic.{Bad, Good, Or}
//import org.scalatest.{FlatSpec, Matchers}
//
//class MigrationStepsExecutorSpec extends FlatSpec with Matchers {
//  val executor: MigrationStepsExecutor = ???
//
//  val emptyProject = SchemaDsl().buildProject()
//
//  val modelName = "MyModel"
//  val fieldName = "myField"
//
//  "Adding a model to a project" should "succeed if the does not exist yet" in {
//    val project = SchemaDsl().buildProject()
//    val result  = executeStep(project, CreateModel(modelName))
//    val expectedProject = {
//      val schema = SchemaDsl()
//      schema.model(modelName)
//      schema.buildProject()
//    }
//    result should equal(Good(expectedProject))
//  }
//
//  "Adding a model to a project" should "fail if the model exists already" in {
//    val project = {
//      val schema = SchemaDsl()
//      schema.model(modelName)
//      schema.buildProject()
//    }
//    val result = executeStep(project, CreateModel(modelName))
//    result should equal(Bad(ModelAlreadyExists(modelName)))
//  }
//
//  "Deleting a model from the project" should "succeed if the model exists" in {
//    val project = {
//      val schema = SchemaDsl()
//      schema.model(modelName)
//      schema.buildProject()
//    }
//    val result = executeStep(project, DeleteModel(modelName))
//    result should equal(Good(emptyProject))
//  }
//
//  "Deleting a model from the project" should "fail if the model does not exist" in {
//    val result = executeStep(emptyProject, DeleteModel(modelName))
//    result should equal(Bad(ModelDoesNotExist(modelName)))
//  }
//
//  "Adding a field to a model" should "succeed if the model exists and the field not yet" in {
//    val project = {
//      val schema = SchemaDsl()
//      schema.model(modelName)
//      schema.buildProject()
//    }
//    val expectedProject = {
//      val schema = SchemaDsl()
//      schema.model(modelName).field(fieldName, _.String)
//      schema.buildProject()
//    }
//    val migrationStep = CreateField(
//      model = modelName,
//      name = fieldName,
//      typeName = TypeIdentifier.String.toString,
//      isRequired = false,
//      isList = false,
//      isUnique = false,
//      defaultValue = None
//    )
//    val result = executeStep(project, migrationStep)
//    result should equal(Good(expectedProject))
//  }
//
//  "Adding a field to a model" should "fail if the model does not exist" in {
//    val migrationStep = CreateField(
//      model = modelName,
//      name = fieldName,
//      typeName = TypeIdentifier.String.toString,
//      isRequired = false,
//      isList = false,
//      isUnique = false,
//      defaultValue = None
//    )
//    val result = executeStep(emptyProject, migrationStep)
//    result should equal(Bad(ModelDoesNotExist(modelName)))
//  }
//
//  "Deleting a field" should "succeed if the field exists" in {
//    val migrationStep = DeleteField(
//      model = modelName,
//      name = fieldName
//    )
//    val project = {
//      val schema = SchemaDsl()
//      schema.model(modelName).field(fieldName, _.String)
//      schema.buildProject()
//    }
//    val expectedProejct = {
//      val schema = SchemaDsl()
//      schema.model(modelName)
//      schema.buildProject()
//    }
//    val result = executeStep(project, migrationStep)
//    result should equal(Good(expectedProejct))
//  }
//
//  "Deleting a field" should "fail if the field does not exist" in {
//    val migrationStep = DeleteField(
//      model = modelName,
//      name = fieldName
//    )
//    val project = {
//      val schema = SchemaDsl()
//      schema.model(modelName)
//      schema.buildProject()
//    }
//    val result = executeStep(project, migrationStep)
//    result should equal(Bad(FieldDoesNotExist(modelName, fieldName)))
//  }
//
//  "Deleting a field" should "fail if the model does not exist" in {
//    val migrationStep = DeleteField(
//      model = modelName,
//      name = fieldName
//    )
//    val result = executeStep(emptyProject, migrationStep)
//    result should equal(Bad(ModelDoesNotExist(modelName)))
//  }
//
////  val exampleField = Field(
////    id = "myField",
////    name = "myField",
////    typeIdentifier = TypeIdentifier.String,
////    description = None,
////    isRequired = false,
////    isList = false,
////    isUnique = false,
////    isSystem = false,
////    isReadonly = false,
////    enum = None,
////    defaultValue = None
////  )
//
//  def executeStep(project: Project, migrationStep: MigrationStep): Or[Project, MigrationStepError] = {
//    executor.execute(project, MigrationSteps(Vector(migrationStep)))
//  }
//}
