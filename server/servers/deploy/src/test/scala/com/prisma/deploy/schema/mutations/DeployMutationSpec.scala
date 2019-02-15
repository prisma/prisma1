package com.prisma.deploy.schema.mutations

import com.prisma.IgnoreMongo
import com.prisma.deploy.specutils.ActiveDeploySpecBase
import com.prisma.shared.models._
import org.scalatest.{FlatSpec, Matchers}

class DeployMutationSpec extends FlatSpec with Matchers with ActiveDeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "DeployMutation" should "succeed for valid input" in {
    val (project, _) = setupProject(basicTypesGql)

    // Full feature set deploy
    val schema = basicTypesGql +
      """
        |type TestModel2 {
        |  id: ID! @unique
        |  stringField: String @default(value: "MuchDefaultWow")
        |  requiredStringField: String!
        |  stringListField: [String]
        |  requiredStringListField: [String]
        |  boolField: Boolean
        |  requiredBoolField: Boolean!
        |  boolListField: [Boolean]
        |  requiredBoolListField: [Boolean]
        |  dateTimeField: DateTime
        |  requiredDateTimeField: DateTime!
        |  dateTimeListField: [DateTime]
        |  requiredDateTimeListField: [DateTime]
        |  intField: Int
        |  requiredIntField: Int!
        |  intListField: [Int]
        |  requiredIntListField: [Int]
        |  floatField: Float
        |  requiredFloatField: Float!
        |  floatListField: [Float]
        |  requiredFloatListField: [Float]
        |  oneRelation: TestModel3 @relation(name: "Test2OnTest3")
        |  requiredOneRelation: TestModel4! @relation(name: "Test2OnTest4")
        |  multiRelation: [TestModel5] @relation(name: "Test2OnTest5")
        |  requiredMultiRelation: [TestModel6] @relation(name: "Test2OnTest6")
        |  enumField: Testnum
        |  requiredEnumField: Testnum!
        |  enumListField: [Testnum]
        |  requiredEnumListField: [Testnum]
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

    server.deploySchema(project, schema)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC
  }

  "DeployMutation" should "create, update and delete scalar list" in {
    val (project, _) = setupProject(basicTypesGql)

    val schema1 =
      """
        |type TestModel {
        |  id: ID! @unique
        |  stringListField: [String]
        |}
      """

    val schema2 =
      """
        |type TestModel {
        |  id: ID! @unique
        |  stringListField: [Int]
        |}
      """

    val schema3 =
      """
        |type TestModel {
        |  id: ID! @unique
        |  intListField: [Int]
        |}
      """

    server.deploySchema(project, schema1)
    server.deploySchema(project, schema2)
    server.deploySchema(project, schema3)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(5))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 5 // order is DESC
  }

  "DeployMutation" should "fail if reserved fields are malformed" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)

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
        """
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
                 """

    val (project, _)  = setupProject(schema)
    val loadedProject = projectPersistence.load(project.id).await.get

    loadedProject.schema.getModelByName_!("TestModel").getFieldByName_!("id").isHidden shouldEqual true
    loadedProject.schema.getModelByName_!("TestModel").getFieldByName_!("createdAt").isHidden shouldEqual true
    loadedProject.schema.getModelByName_!("TestModel").getFieldByName_!("updatedAt").isHidden shouldEqual true
  }

  "DeployMutation" should "hide reserved fields instead of deleting them and reveal them instead of creating them" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """

    val (project, _)  = setupProject(schema)
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
                        """

    server.deploySchema(project, updatedSchema)

    val reloadedProject = projectPersistence.load(project.id).await.get

    reloadedProject.schema.getModelByName("TestModel").get.getFieldByName("id").get.isVisible shouldEqual false
    reloadedProject.schema.getModelByName("TestModel").get.getFieldByName("createdAt").get.isHidden shouldEqual false
    reloadedProject.schema.getModelByName("TestModel").get.getFieldByName("updatedAt").get.isHidden shouldEqual false

    // todo assert client db cols?
  }

  "DeployMutation" should "should not blow up on consecutive deploys" ignore {
    val (project, _) = setupProject(basicTypesGql)

    val schema =
      """
        |type A {
        |  id: ID!@unique
        |  i: Int
        |  b: B @relation(name: "TADA")
        |}
        |type B {
        |  i: Int
        |  a: A
        |}""".stripMargin

    server.deploySchema(project, schema)
    Thread.sleep(10000)
    server.deploySchema(project, schema)
    Thread.sleep(10000)
    server.deploySchema(project, schema)

    Thread.sleep(30000)
  }

  "DeployMutation" should "return an error if a subscription query is invalid" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val (project, _) = setupProject(schema)

    val fnInput = FunctionInput(name = "failing", query = "invalid query", url = "http://whatever.com", headers = Vector(HeaderInput("header1", "value1")))
    val result  = deploySchema(project, schema, Vector(fnInput))
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
    val result  = deploySchema(project, schema, Vector(fnInput))

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

  def deploySchema(project: Project, schema: String, functions: Vector[FunctionInput] = Vector.empty) = {
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
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
    val nameAndStage  = testDependencies.projectIdEncoder.fromEncodedString(project.id)
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
                          |}
                        """.stripMargin

    val res = server.deploySchemaThatMustErrorWithCode(project, updatedSchema, false, 0)

    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"The field `t2` has an invalid name in the `@relation` directive. It can only have up to 54 characters and must have the shape [A-Z][a-zA-Z0-9]*"}],"warnings":[]}}}""".stripMargin)

  }

  "DeployMutation" should "shorten autogenerated relationNames to a maximum of 54 characters" in {
    val schema = """
                   |type TestModel {
                   |  id: ID! @unique
                   |  test: String
                   |}
                 """.stripMargin

    val (project, _)  = setupProject(schema)
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

    server.deploySchema(project, updatedSchema)

    val reloadedProject = projectPersistence.load(project.id).await.get

    reloadedProject.schema.relations.head.name should be("TestModel2WhichAlsoHappensToTestModelWithAVeryLongName")
  }

  "DeployMutation" should "error if defaultValue are provided for list fields" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val schema =
      """
        |type TestModel {
        |  id: ID! @unique
        |  requiredIntList: [Int] @default(value: "[1,2]") 
        |}
      """.stripMargin

    val result1 = server.query(s"""
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

    result1.pathAsSeq("data.deploy.errors").head.toString should include("List fields cannot have defaultValues.")
  }

  "DeployMutation" should "throw a correct error for an invalid query" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val schema =
      """
        |{
        |  id: ID! @unique
        |}
      """.stripMargin

    val result1 = server.queryThatMustFail(
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
      """.stripMargin,
      3017
    )
  }

  "DeployMutation" should "work with self relations" in {
    val (project, _) = setupProject(basicTypesGql)
    val schema =
      """
        |type Comment{
        |  id: ID! @unique
        |  title: String
        |  comments: [Comment] @relation(name: "RelatedComments")
        |  parent: Comment @relation(name: "RelatedComments")
        |}
      """.stripMargin

    server.deploySchema(project, schema)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC
  }

  "DeployMutation" should "work with cascading delete directives" in {
    val (project, _) = setupProject(basicTypesGql)
    val schema =
      """
        |type Author{
        |  id: ID! @unique
        |  name: String!
        |  comments: [Comment] @relation(name: "AuthorComments" onDelete: CASCADE)
        |}
        |
        |type Comment{
        |  id: ID! @unique
        |  title: String
        |  author: Author! @relation(name: "AuthorComments" onDelete: SET_NULL)
        |}
      """.stripMargin

    server.deploySchema(project, schema)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC
  }

  "DeployMutation" should "detect and report addition and removal of secrets if that is the only change" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)

    server.query(
      s"""
         |mutation {
         |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(basicTypesGql)}, secrets: ["secret"]}){
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

    val updatedProject: Seq[Project] = projectPersistence.loadAll().await
    updatedProject.head.secrets.head should be("secret")
    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC
    migrations.head.steps should be(Vector(UpdateSecrets(Vector("secret"))))

    server.query(
      s"""
         |mutation {
         |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(basicTypesGql)}}){
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

    val updatedProject2: Seq[Project] = projectPersistence.loadAll().await
    updatedProject2.head.secrets should be(List.empty)
    val migrations2 = migrationPersistence.loadAll(project.id).await
    migrations2 should have(size(4))
    migrations2.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations2.head.revision shouldEqual 4 // order is DESC
    migrations2.head.steps should be(Vector(UpdateSecrets(Vector())))
  }

  "DeployMutation" should "not change secrets if there are errors in the deploy (invalid functions)" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val schema =
      """
        |{
        |  id: ID! @unique
        |}
      """.stripMargin

    server.queryThatMustFail(
      s"""
         |mutation {
         |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}, secrets: ["new Secret"]}){
         |    migration {
         |      applied
         |    }
         |    errors {
         |      description
         |    }
         |  }
         |}
      """.stripMargin,
      3017
    )
    val updatedProject: Seq[Project] = projectPersistence.loadAll().await
    updatedProject.head.secrets should be(List.empty)
    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(2))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 2 // order is DESC
  }

  "DeployMutation" should "throw a proper error if detecting an ambiguous relation update" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val schema =
      """
        |type Note {
        | name: String! @unique
        | # creator: User!
        | members: [User]
        |}
        |
        |type User {
        | name: String! @unique
        |}
      """.stripMargin

    server.deploySchema(project, schema)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC

    val schema2 =
      """
        |type Note {
        | name: String! @unique
        | creator: User! @relation(name: "Creator")
        | members: [User] @relation(name: "MemberOf")
        |}
        |
        |type User {
        | name: String! @unique
        | Notes: [Note] @relation(name: "Creator")
        | memberOf: [Note] @relation(name: "MemberOf")
        |}
      """.stripMargin

    server.queryThatMustFail(
      s"""
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
      """.stripMargin,
      errorCode = 3018,
      errorContains =
        "There is a relation ambiguity during the migration. Please first name the old relation on your schema. The ambiguity is on a relation between Note and User."
    )
  }

  "DeployMutation" should "throw a proper error if detecting an ambiguous relation update 2" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val schema =
      """
        |type Note {
        | name: String! @unique
        | creator: User! @relation(name: "Creator")
        | members: [User] @relation(name: "MemberOf")
        |}
        |
        |type User {
        | name: String! @unique
        | Notes: [Note] @relation(name: "Creator")
        | memberOf: [Note] @relation(name: "MemberOf")
        |}
      """.stripMargin

    server.deploySchema(project, schema)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC

    val schema2 =
      """
        |type Note {
        | name: String! @unique
        | # creator: User!
        | members: [User]
        |}
        |
        |type User {
        | name: String! @unique
        |}
      """.stripMargin

    server.queryThatMustFail(
      s"""
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
      """.stripMargin,
      errorCode = 3018,
      errorContains = "There is a relation ambiguity during the migration. The ambiguity is on a relation between Note and User."
    )
  }

  "DeployMutation" should "be able to change a field from scalar non-list to scalar list" in {
    val (project, _) = setupProject(basicTypesGql)
    val schema =
      """
        |type A {
        | name: String! @unique
        | value: Int
        |}
      """.stripMargin

    server.deploySchema(project, schema)

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC

    val schema2 =
      """
        |type A {
        | name: String! @unique
        | value: [Int]
        |}
      """.stripMargin

    server.deploySchema(project, schema2)

    val migrations2 = migrationPersistence.loadAll(project.id).await
    migrations2 should have(size(4))
    migrations2.exists(x => x.status != MigrationStatus.Success) shouldEqual false
    migrations2.head.revision shouldEqual 4 // order is DESC
  }

  "DeployMutation" should "succeed with id fields of type UUID" taggedAs (IgnoreMongo) in {
    val (project, _) = setupProject(basicTypesGql)
    val schema =
      """
        |type A {
        | id: UUID! @unique
        |}
      """.stripMargin

    val updatedProject = server.deploySchema(project, schema)
    val idField        = updatedProject.schema.getModelByName_!("A").idField_!
    idField.typeIdentifier should be(TypeIdentifier.UUID)
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
    def formatArray[T](objs: Vector[T], formatFn: T => String) = objs.map(formatFn).mkString(start = "[", sep = ",", end = "]")

    formatArray(functions, formatFunction)
  }
}
