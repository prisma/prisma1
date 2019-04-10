package com.prisma.integration

import com.prisma.{ConnectorTag, IgnoreMongo, IgnoreMySql}
import com.prisma.ConnectorTag.{MySqlConnectorTag, PostgresConnectorTag}
import org.scalatest.{FlatSpec, Matchers}

class PrefillingFieldsWithDefaultOrMigrationValueSpec extends FlatSpec with Matchers with IntegrationBaseSpec {
  override def runOnlyForConnectors: Set[ConnectorTag] = Set(MySqlConnectorTag, PostgresConnectorTag)

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

  "Creating a required Field of type ID" should "not error when there is no defaultValue but there are no nodes yet" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | test: ID!
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Creating a required Field of type UUID" should "not error when there is no defaultValue but there are no nodes yet" taggedAs (IgnoreMongo, IgnoreMySql) in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | test: UUID!
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Creating a required Field of type UUID" should "not error when there is no defaultValue" taggedAs (IgnoreMongo, IgnoreMySql) in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "test"}){name}}""", project)

    val schema1 =
      """type A {
        | name: String! @unique
        | test: UUID!
        |}""".stripMargin

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, true)
    res.toString should include("""The fields will be pre-filled with the value `550e8400-e29b-11d4-a716-446655440000`.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{as{name, test}}", updatedProject).toString should be(
      """{"data":{"as":[{"name":"test","test":"550e8400-e29b-11d4-a716-446655440000"}]}}""")
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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, true)
    res.toString should include("""The fields will be pre-filled with the value `0`.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, new}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"new":0}]}}""")
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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
    res.toString should include("""The fields will be pre-filled with the value `1`.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, new}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"new":1}]}}""")
  }

  "Making a field required without default value" should "set the internal migration value for null values" in {

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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
    res.toString should include("""The fields will be pre-filled with the value: `0`.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"test":3},{"age":2,"test":0}]}}""")
  }

  "Making an optional field required with default value" should "set the default value on null fields" in {

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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
    res.toString should include("""The fields will be pre-filled with the value: `1`.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"test":1},{"age":2,"test":3}]}}""")
  }

  "Changing the typeIdentifier of a required field without default value" should "set the internal migration value for all values" in {

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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
    res.toString should include(""" The fields will be pre-filled with the value: ``.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"test":""}]}}""")
  }

  "Changing the typeIdentifier for a required field with default value" should "set the default value on all fields" in {

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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
    res.toString should include("""The fields will be pre-filled with the value: `default`.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"test":"default"}]}}""")

  }

  "Changing the typeIdentifier of a optional field without default value" should "set all existing values to null" in {

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

  "Changing the typeIdentifier for a optional field with default value" should "set all fields to null" in {

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

  "Changing the typeIdentifier of a optional field AND making it required without default value" should "set all values to the default migration value" in {

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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
    res.toString should include("""The fields will be pre-filled with the value: ``.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be("""{"data":{"persons":[{"age":1,"test":""},{"age":2,"test":""}]}}""")
  }

  "Changing the typeIdentifier of a optional field AND making it required with default value" should "set all fields to the default value" in {

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

    val res = deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
    res.toString should include("""The fields will be pre-filled with the value: `default`.""")

    val updatedProject = deployServer.deploySchema(project, schema1)
    apiServer.query("query{persons{age, test}}", updatedProject).toString should be(
      """{"data":{"persons":[{"age":1,"test":"default"},{"age":2,"test":"default"}]}}""")
  }

  "Making a unique field required without default value" should "should error" in {

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

  "Making a unique field required with default value" should "should error" in {

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
