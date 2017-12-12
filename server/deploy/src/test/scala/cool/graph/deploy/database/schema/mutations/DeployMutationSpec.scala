package cool.graph.deploy.database.schema.mutations

import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models.ProjectId
import org.scalatest.{FlatSpec, Matchers}

class DeployMutationSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "DeployMutation" should "succeed for valid input" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    // Full feature set deploy
    val schema = basicTypesGql +
      """
        |type TestModel2 {
        |  id: ID! @unique
        |  stringField: String @default(value: "MuchDefaultWow")
        |  requiredStringField: String!
        |  stringListField: [String!]
        |  requiredStringListField: [String!]!
        |  boolField: Boolean
        |  requiredBoolField: Boolean!
        |  boolListField: [Boolean!]
        |  requiredBoolListField: [Boolean!]!
        |  dateTimeField: DateTime
        |  requiredDateTimeField: DateTime!
        |  dateTimeListField: [DateTime!]
        |  requiredDateTimeListField: [DateTime!]!
        |  intField: Int
        |  requiredIntField: Int!
        |  intListField: [Int!]
        |  requiredIntListField: [Int!]!
        |  floatField: Float
        |  requiredFloatField: Float!
        |  floatListField: [Float!]
        |  requiredFloatListField: [Float!]!
        |  oneRelation: TestModel3 @relation(name: "Test2OnTest3")
        |  requiredOneRelation: TestModel4! @relation(name: "Test2OnTest4")
        |  multiRelation: [TestModel5!]! @relation(name: "Test2OnTest5")
        |  requiredMultiRelation: [TestModel6!]! @relation(name: "Test2OnTest6")
        |}
        |
        |type TestModel3 {
        |  id: ID! @unique
        |  back: TestModel2 @relation(name: "Test2OnTest3")
        |}
        |
        |type TestModel4 {
        |  id: ID! @unique
        |  back: TestModel2! @relation(name: "Test2OnTest4")
        |}
        |
        |type TestModel5 {
        |  id: ID! @unique
        |  back: TestModel2 @relation(name: "Test2OnTest5")
        |}
        |
        |type TestModel6 {
        |  id: ID! @unique
        |  back: TestModel2! @relation(name: "Test2OnTest6")
        |}
      """.stripMargin

    val result = server.query(s"""
       |mutation {
       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: "${schema.replaceAll("\n", " ").replaceAll("\\\"", "\\\\\"")}"}){
       |    project {
       |      name
       |      stage
       |    }
       |    errors {
       |      description
       |    }
       |  }
       |}
      """.stripMargin)

    result.pathAsString("data.deploy.project.name") shouldEqual nameAndStage.name
    result.pathAsString("data.deploy.project.stage") shouldEqual nameAndStage.stage

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(!_.hasBeenApplied) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC
  }

  "DeployMutation" should "handle renames with migration values" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    // Full feature set deploy
    val schema = basicTypesGql +
      """
        |type TestModel2 {
        |  id: ID! @unique
        |  test: String
        |}
      """.stripMargin

    val result = server.query(s"""
       |mutation {
       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: "${schema.replaceAll("\n", " ")}"}){
       |    project {
       |      name
       |      stage
       |    }
       |    errors {
       |      description
       |    }
       |  }
       |}
      """.stripMargin)

    result.pathAsSeq("data.deploy.errors") should be(empty)

    // Todo create some client data to check / migrate

    val updatedSchema = basicTypesGql +
      """
        |type TestModel2 {
        |  id: ID! @unique
        |  renamed: String @migrationValue(value: "SuchMigrationWow")
        |}
      """.stripMargin

    val updateResult = server.query(s"""
        |mutation {
        |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: "${schema.replaceAll("\n", " ")}"}){
        |    project {
        |      name
        |      stage
        |    }
        |    errors {
        |      description
        |    }
        |  }
        |}
      """.stripMargin)

    updateResult.pathAsSeq("data.deploy.errors") should be(empty)

//    val migrations = migrationPersistence.loadAll(project.id).await
//    migrations should have(size(3))
//    migrations.exists(!_.hasBeenApplied) shouldEqual false
//    migrations.head.revision shouldEqual 3 // order is DESC
  }
}
