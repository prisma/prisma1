package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class DeployingDefaultValuesSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Adding a defaultValue of the wrong type" should "provide a proper error" in {

    val schema =
      """type Person {
        |  id: ID! @id
        |  age: Int! @unique
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Person {
        |  id: ID! @id
        |  age: Int! @unique @default(value: "notANumber")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"The value \"notANumber\" is not a valid default for fields of type Int."}],"warnings":[]}}}""")
  }

  "Changing a field type to one incompatible with its default value" should "provide a proper error" in {

    val schema =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  age: String! @unique @default(value:"onehundred")
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Person {
         |  id: ID! @id
         |  name: String
         |  age: Int! @unique @default(value:"onehundred")
         |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"The value \"onehundred\" is not a valid default for fields of type Int."}],"warnings":[]}}}""")
  }

  "Deploying a default value on a list" should "provide a proper error" in {

    val schema =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  children: [String] @scalarList(strategy: RELATION)
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  children: [String] @scalarList(strategy: RELATION) @default(value:"[\"Pia\",\"Paul\"]")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"The `@default` directive must only be placed on scalar fields that are not lists."}],"warnings":[]}}}""")
  }

  "Making a field with a defaultValue a list" should "provide a proper error" in {

    val schema =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  children: String @default(value:"Pia")
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  children: [String] @default(value:"Pia")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"The `@default` directive must only be placed on scalar fields that are not lists."}],"warnings":[]}}}""")
  }

  "Deploying a wrong enum default value" should "provide a proper error" in {

    val schema =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  enum: AB
        |}
        |
        |enum AB{
        |  A
        |  B
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  enum: AB @default(value:C)
        |}
        |
        |enum AB{
        |  A
        |  B
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"The default value is invalid for this enum. Valid values are: A, B."}],"warnings":[]}}}""")
  }

  "Deploying a correct enum default value" should "work" in {

    val schema =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  enum: AB
        |}
        |
        |enum AB{
        |  A
        |  B
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Person {
        |  id: ID! @id
        |  name: String
        |  enum: AB @default(value:B)
        |}
        |
        |enum AB{
        |  A
        |  B
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val res = apiServer.query("""mutation{createPerson(data:{name:"Test"}){enum}}""", updatedProject)
    res.toString should be("""{"data":{"createPerson":{"enum":"B"}}}""")
  }

}
