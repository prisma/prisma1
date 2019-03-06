package com.prisma.integration

import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.MongoConnectorTag
import org.scalatest.{FlatSpec, Matchers}

class PrefillingFieldsWithDefaultOrMigrationValueMongoSpec extends FlatSpec with Matchers with IntegrationBaseSpec {
  //Mongo ClientDBQueries are for the most part not yet implemented and just return false
  //Therefore all tests relying on these are ignored
  override def runOnlyForConnectors: Set[ConnectorTag] = Set(MongoConnectorTag)

  "Creating a required Field" should "not error when there is no defaultValue but there are no nodes yet" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int!
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Adding a required field without default value" should "set the internal migration value" in {

    val schema =
      """type Person {
        |  age: Int! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  new: Int!
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include("""There are already nodes for the model `Person` that would violate that constraint.""")
  }

  "Adding a required field with default value" should "set the default value" in {

    val schema =
      """type Person {
        |  age: Int! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  new: Int! @default(value: 1)
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include("""There are already nodes for the model `Person` that would violate that constraint.""")

  }

  "Making a field required without default value" should "set the internal migration value for null values" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: Int!
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include("""The fields will be pre-filled with the value: `0`.""")
  }

  "Making an optional field required with default value" should "set the default value on null fields" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2, test: 3}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: Int! @default(value: 1)
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include("""The fields will be pre-filled with the value: `1`.""")
  }

  "Changing the typeIdentifier of a required field without default value" should "set the internal migration value for all values" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int!
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: String!
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include(""" The fields will be pre-filled with the value: ``.""")
  }

  "Changing the typeIdentifier for a required field with default value" should "set the default value on all fields" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int!
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: String! @default(value: "default")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include("""The fields will be pre-filled with the value: `default`.""")
  }

  "Changing the typeIdentifier of a optional field without default value" should "set all existing values to null" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: String
        |}"""

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, true)
    res.toString should include("""This change may result in data loss.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"test":null},{"age":2,"test":null}]}}""")
  }

  "Changing the typeIdentifier for a optional field with default value" should "set all fields to null" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: String @default(value: "default")
        |}"""

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, true)
    res.toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"test":null},{"age":2,"test":null}]}}""")
  }

  "Changing the typeIdentifier of a optional field AND making it required without default value" should "set all values to the default migration value" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: String!
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include("""The fields will be pre-filled with the value: ``.""")
  }

  "Changing the typeIdentifier of a optional field AND making it required with default value" should "set all fields to the default value" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: String! @default(value: "default")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include("""The fields will be pre-filled with the value: `default`.""")
  }

  "Making a unique field required without default value" should "should error" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: Int! @unique
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include(
      """You are updating the field `test` to be required and unique. But there are already nodes for the model `Person` that would violate that constraint.""")
  }

  "Making a unique field required with default value" should "should error" ignore {

    val schema =
      """type Person {
        |  age: Int! @unique
        |  test: Int @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("mutation{createPerson(data:{age: 1, test: 3}){age}}", project)
    apiServer.query("mutation{createPerson(data:{age: 2}){age}}", project)

    val schema1 =
      """type Person {
        |  age: Int!
        |  test: Int! @unique @default(value: 10)
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should include(
      """You are updating the field `test` to be required and unique. But there are already nodes for the model `Person` that would violate that constraint.""")
  }
}
