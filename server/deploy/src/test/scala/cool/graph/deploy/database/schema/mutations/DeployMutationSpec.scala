package cool.graph.deploy.database.schema.mutations

import cool.graph.deploy.schema.mutations.{FunctionInput, HeaderInput}
import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models._
import cool.graph.stub.Stub
import org.scalatest.{FlatSpec, Matchers}

class DeployMutationSpec extends FlatSpec with Matchers with DeploySpecBase {

  import cool.graph.stub.Import._

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "DeployMutation" should "succeed for valid input" in {
    val (project, _) = setupProject(basicTypesGql)
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
       |    migration {
       |      applied
       |    }
       |    errors {
       |      description
       |    }
       |  }
       |}
      """.stripMargin)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC
  }

  "DeployMutation" should "create, update and delete scalar list" in {
    val (project, _) = setupProject(basicTypesGql)
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
                                 |    migration {
                                 |      applied
                                 |    }
                                 |    errors {
                                 |      description
                                 |    }
                                 |  }
                                 |}
      """.stripMargin)

    server.query(s"""
                                  |mutation {
                                  |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema2)}}){
                                  |    migration {
                                  |      applied
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
                                  |    migration {
                                  |      applied
                                  |    }
                                  |    errors {
                                  |      description
                                  |    }
                                  |  }
                                  |}
      """.stripMargin)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(5))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 5 // order is DESC
  }

  "DeployMutation" should "handle renames with migration values" in {
    val (project, _) = setupProject(basicTypesGql)
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
       |    migration {
       |      applied
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
        |    migration {
        |      applied
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
    val (project, _) = setupProject(basicTypesGql)
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
         |    migration {
         |      applied
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

    val (project, _)  = setupProject(schema)
    val loadedProject = projectPersistence.load(project.id).await.get

    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("id").get.isHidden shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual true
  }

  "DeployMutation" should "hide reserved fields instead of deleting them and reveal them instead of creating them" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val (project, _)  = setupProject(schema)
    val nameAndStage  = ProjectId.fromEncodedString(project.id)
    val loadedProject = projectPersistence.load(project.id).await.get

    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("id").get.isVisible shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual true

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
         |    migration {
         |      applied
         |    }
         |    errors {
         |      description
         |    }
         |  }
         |}""".stripMargin)

    updateResult.pathAsSeq("data.deploy.errors") should be(empty)

    val reloadedProject = projectPersistence.load(project.id).await.get

    reloadedProject.schema.getModelByName("TestModel").get.getFieldByName("id").get.isVisible shouldEqual false
    reloadedProject.schema.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual false
    reloadedProject.schema.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual false

    // todo assert client db cols?
  }

//  "DeployMutation" should "should not blow up on consecutive deploys" in {
//    val project = setupProject(basicTypesGql)
//
//    val schema =
//      """
//        |type A {
//        |  id: ID!@unique
//        |  i: Int
//        |  b: B @relation(name: "TADA")
//        |}
//        |type B {
//        |  i: Int
//        |  a: A
//        |}""".stripMargin
//
//    deploySchema(project, schema)
//    Thread.sleep(10000)
//    deploySchema(project, schema)
//    Thread.sleep(10000)
//    deploySchema(project, schema)
//
//    Thread.sleep(30000)
//  }

  "DeployMutation" should "return an error if a subscription query is invalid" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val (project, _) = setupProject(schema)

    val fnInput = FunctionInput(name = "my-function", query = "invalid query", url = "http://whatever.com", headers = Vector(HeaderInput("header1", "value1")))

    val result = {
      val ProjectId(name, stage) = project.projectId
      val stub = Request("POST", s"/$name/$stage/private")
        .stub(
          200,
          """{
          |  "data": {
          |    "validateSubscriptionQuery": {
          |      "errors": ["This did not work!"]
          |    }
          |  }
          |}
        """.stripMargin
        )
        .ignoreBody
      withStubs(stub) {
        deploySchema(project, schema, Vector(fnInput))
      }
    }
    result.pathAsSeq("data.deploy.errors") should not(be(empty))

    val reloadedProject = projectPersistence.load(project.id).await.get
    reloadedProject.functions should have(size(0))
  }

  "DeployMutation" should "create a server side subscription if the subscription query is valid" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val (project, _) = setupProject(schema)

    val fnInput = FunctionInput(name = "my-function", query = "my query", url = "http://whatever.com", headers = Vector(HeaderInput("header1", "value1")))
    val result = {
      val ProjectId(name, stage) = project.projectId
      val stub = Request("POST", s"/$name/$stage/private")
        .stub(
          200,
          """{
            |  "data": {
            |    "validateSubscriptionQuery": {
            |      "errors": []
            |    }
            |  }
            |}
          """.stripMargin
        )
        .ignoreBody

      withStubs(stub) {
        deploySchema(project, schema, Vector(fnInput))
      }
    }
    result.pathAsSeq("data.deploy.errors") should be(empty)

    val reloadedProject = projectPersistence.load(project.id).await.get
    reloadedProject.functions should have(size(1))
    val function = reloadedProject.functions.head.asInstanceOf[ServerSideSubscriptionFunction]
    function.name should equal(fnInput.name)
    function.query should equal(fnInput.query)
    val delivery = function.delivery.asInstanceOf[WebhookDelivery]
    delivery.url should equal(fnInput.url)
    delivery.headers should equal(Vector("header1" -> "value1"))
  }

  def withStubs(stubs: Stub*) = {
    // use a fixed port for every test because we instantiate the client only once in the dependencies
    withStubServer(List(stubs: _*), stubNotFoundStatusCode = 418, port = 8777)
  }

  def deploySchema(project: Project, schema: String, functions: Vector[FunctionInput] = Vector.empty) = {
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    server.query(s"""
      |mutation {
      |  deploy(input:{
      |    name: "${nameAndStage.name}"
      |    stage: "${nameAndStage.stage}"
      |    types: ${formatSchema(schema)}
      |    subscriptions: ${formatFunctions(functions)}
      |  }){
      |    migration {
      |      steps {
      |        type
      |      }
      |    }
      |    errors {
      |      description
      |    }
      |  }
      |}""".stripMargin)
  }

  "DeployMutation" should "error on a relationName that are too long (>54 chars)" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val (project, _)  = setupProject(schema)
    val nameAndStage  = ProjectId.fromEncodedString(project.id)
    val loadedProject = projectPersistence.load(project.id).await.get

    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("id").get.isVisible shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual true

    val updatedSchema = """
                          |type TestModel {
                          |  id: ID! @unique
                          |  test: String
                          |  t2: TestModel2 @relation(name: "ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJ")
                          |}
                          |
                          |type TestModel2 {
                          |  id: ID! @unique
                          |  test: String
                          |  t1: TestModel
                          |}
                        """.stripMargin

    server.queryThatMustFail(
      s"""
                                       |mutation {
                                       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(updatedSchema)}}){
                                       |    migration {
                                       |      applied
                                       |    }
                                       |    errors {
                                       |      description
                                       |    }
                                       |  }
                                       |}""".stripMargin,
      errorCode = 4004,
      errorContains = "ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJ is not a valid name for a relation."
    )
  }

  "DeployMutation" should "shorten autogenerated relationNames to a maximum of 54 characters" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val (project, _)  = setupProject(schema)
    val nameAndStage  = ProjectId.fromEncodedString(project.id)
    val loadedProject = projectPersistence.load(project.id).await.get

    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("id").get.isVisible shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual true
    loadedProject.schema.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual true

    val updatedSchema = """
                          |type TestModelWithAVeryLongName {
                          |  id: ID! @unique
                          |  test: String
                          |  t2: TestModel2WhichAlsoHappensToHaveAVeryLongName
                          |}
                          |
                          |type TestModel2WhichAlsoHappensToHaveAVeryLongName {
                          |  id: ID! @unique
                          |  test: String
                          |  t1: TestModelWithAVeryLongName
                          |}
                        """.stripMargin

    val updateResult = server.query(s"""
                                       |mutation {
                                       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(updatedSchema)}}){
                                       |    migration {
                                       |      applied
                                       |    }
                                       |    errors {
                                       |      description
                                       |    }
                                       |  }
                                       |}""".stripMargin)

    updateResult.pathAsSeq("data.deploy.errors") should be(empty)

    val reloadedProject = projectPersistence.load(project.id).await.get

    reloadedProject.schema.relations.head.name should be("TestModel2WhichAlsoHappensToTestModelWithAVeryLongName")
  }

  private def formatFunctions(functions: Vector[FunctionInput]) = {
    def formatFunction(fn: FunctionInput) = {
      s"""{
         |  name: ${escapeString(fn.name)}
         |  query: ${escapeString(fn.query)}
         |  url: ${escapeString(fn.url)}
         |  headers: ${formatArray(fn.headers, formatHeader)}
         |}
       """.stripMargin
    }
    def formatHeader(header: HeaderInput) = {
      s"""{
         |  name: ${escapeString(header.name)}
         |  value: ${escapeString(header.value)}
         |}""".stripMargin
    }
    def formatArray[T](objs: Vector[T], formatFn: T => String) = "[" + objs.map(formatFn).mkString(",") + "]"

    formatArray(functions, formatFunction)
  }
}
