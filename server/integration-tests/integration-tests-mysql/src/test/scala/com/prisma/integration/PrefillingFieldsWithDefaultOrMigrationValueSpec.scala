package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class PrefillingFieldsWithDefaultOrMigrationValueSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  //Affected Deploy Actions
  // creating new required field                 -> set column to default/migValue for all  rows  -> createColumn
  // making field required                       -> set column to default/migValue for all rows where it is null -> updateColumn
  // changing type of a (newly) required field   -> set column to default/migValue for all  rows -> createColumn

  //Necessary changes
  //Remove validations
  //Remove errors
  //Create shared MigvalueMatcher
  //Change queries to insert default/migration value
  //Add tests
  //Fix broken tests

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
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
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

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be("""{"data":{"deploy":{"migration":null,"errors":[{"description":"Invalid value 'notANumber' for type Int."}],"warnings":[]}}}""")
  }

}
