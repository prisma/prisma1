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
        |  enumField: Testnum
        |  requiredEnumField: Testnum!
        |  enumListField: [Testnum!]
        |  requiredEnumListField: [Testnum!]!
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
        |
        |enum Testnum {
        |  Test1
        |  Test2
        |}
      """.stripMargin

    val result = server.query(s"""
       |mutation {
       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}}){
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

  "DeployMutation" should "create, update and delete scalar list" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    val schema1 =
      """
        |type TestModel {
        |  id: ID! @unique
        |  stringListField: [String!]
        |}
      """.stripMargin

    val schema2 =
      """
        |type TestModel {
        |  id: ID! @unique
        |  stringListField: [Int!]
        |}
      """.stripMargin

    val schema3 =
      """
        |type TestModel {
        |  id: ID! @unique
        |  intListField: [Int!]
        |}
      """.stripMargin

    val result1 = server.query(s"""
                                 |mutation {
                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema1)}}){
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

    result1.pathAsString("data.deploy.project.name") shouldEqual nameAndStage.name
    result1.pathAsString("data.deploy.project.stage") shouldEqual nameAndStage.stage

    server.query(s"""
                                  |mutation {
                                  |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema2)}}){
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

    server.query(s"""
                                  |mutation {
                                  |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema3)}}){
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

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(5))
    migrations.exists(!_.hasBeenApplied) shouldEqual false
    migrations.head.revision shouldEqual 5 // order is DESC
  }

  "DeployMutation" should "handle renames with migration values" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    val schema = basicTypesGql +
      """
        |type TestModel2 {
        |  id: ID! @unique
        |  test: String
        |}
      """.stripMargin

    val result = server.query(s"""
       |mutation {
       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}}){
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
        |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}}){
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

  "DeployMutation" should "fail if reserved fields are malformed" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    def tryDeploy(field: String) = {
      val schema = basicTypesGql +
        s"""
          |type TestModel2 {
          |  $field
          |  test: String
          |}
        """.stripMargin

      val result = server.query(
        s"""
         |mutation {
         |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}}){
         |    project {
         |      name
         |      stage
         |    }
         |    errors {
         |      description
         |    }
         |  }
         |}
        """.stripMargin
      )

      // Query must fail
      result.pathExists("data.deploy.errors") shouldEqual true
    }

    tryDeploy("id: String! @unique")
    tryDeploy("id: ID!")
    tryDeploy("id: ID @unique")
    tryDeploy("""id: ID! @default(value: "Woot")""")

    tryDeploy("updatedAt: String! @unique")
    tryDeploy("updatedAt: DateTime!")
    tryDeploy("updatedAt: DateTime @unique")
    tryDeploy("""updatedAt: DateTime! @default(value: "Woot")""")
  }

  "DeployMutation" should "create hidden reserved fields if they are not specified in the types" in {
    val schema = """
                   |type TestModel {
                   |  test: String
                   |}
                 """.stripMargin

    val project       = setupProject(schema)
    val loadedProject = projectPersistence.load(project.id).await.get

    loadedProject.getModelByName("TestModel").get.getFieldByName("id").get.isHidden shouldEqual true
    loadedProject.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual true
    loadedProject.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual true
  }

  "DeployMutation" should "hide reserved fields instead of deleting them and reveal them instead of creating them" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val project       = setupProject(schema)
    val nameAndStage  = ProjectId.fromEncodedString(project.id)
    val loadedProject = projectPersistence.load(project.id).await.get

    loadedProject.getModelByName("TestModel").get.getFieldByName("id").get.isVisible shouldEqual true
    loadedProject.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual true
    loadedProject.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual true

    val updatedSchema = """
                          |type TestModel {
                          |  test: String
                          |  createdAt: DateTime!
                          |  updatedAt: DateTime!
                          |}
                        """.stripMargin

    val updateResult = server.query(s"""
                                       |mutation {
                                       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(updatedSchema)}}){
                                       |    project {
                                       |      name
                                       |      stage
                                       |    }
                                       |    errors {
                                       |      description
                                       |    }
                                       |  }
                                       |}""".stripMargin)

    updateResult.pathAsSeq("data.deploy.errors") should be(empty)

    val reloadedProject = projectPersistence.load(project.id).await.get

    reloadedProject.getModelByName("TestModel").get.getFieldByName("id").get.isVisible shouldEqual false
    reloadedProject.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual false
    reloadedProject.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual false

    // todo assert client db cols?
  }
}
