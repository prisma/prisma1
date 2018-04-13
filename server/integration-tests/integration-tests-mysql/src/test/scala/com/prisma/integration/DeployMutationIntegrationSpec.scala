package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class DeployMutationIntegrationSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  // test warning without -force
  // test warning with -force
  // test no warning
  // test error
  // test no error

  //test multiple warnings
  //test multiple warnings with -force
  //test warnings and errors
  //test warnings and errors with -force

  //region Delete Model

  "Deleting a model" should "throw a warning if nodes already exist" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}
         |
         |type B {
         | name: String! @unique
         | value: Int
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type B {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change will result in data loss."}]}}}""")
  }

  "Deleting a model" should "throw a warning if nodes already exist but proceed if the -force flag is present" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}
         |
         |type B {
         | name: String! @unique
         | value: Int
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type B {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change will result in data loss."}]}}}""")
  }

  "Deleting a model" should "not throw a warning or error if no nodes  exist" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}
         |
         |type B {
         | name: String! @unique
         | value: Int
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type B {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  //endregion

  // region Create Field

  "Creating a required Field" should "error when nodes already exist and there is no default value" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int!
        |}""".stripMargin

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are creating a required field without a defaultValue but there are already nodes present."}],"warnings":[]}}}""")
  }

  "Creating a required Field" should "succeed when nodes already exist but there is a defaultValue" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int! @default(value: 12)
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

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

  // endregion

  // region Delete Field

  "Deleting a field" should "throw a warning if nodes are present" in {

    val schema =
      """type A {
        | name: String! @unique
        | dummy: String
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A", dummy: "test"}){name, dummy}}""", project).toString should be(
      """{"data":{"createA":{"name":"A","dummy":"test"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Deleting a field" should "throw a warning if nodes are present but proceed with -force flag" in {

    val schema =
      """type A {
        | name: String! @unique
        | dummy: String
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A", dummy: "test"}){name, dummy}}""", project).toString should be(
      """{"data":{"createA":{"name":"A","dummy":"test"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2, true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Deleting a field" should "succeed if no nodes are present" in {

    val schema =
      """type A {
        | name: String! @unique
        | dummy: String
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  // endregion

  //region Update Field

  "Updating a field from scalar non-list to scalar list" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from scalar non-list to scalar list" should "throw a warning if there is already data but proceed with -force" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from scalar non-list to scalar list" should "succeed if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a field from scalar List to scalar non-list" should "warn if there is already data" in {

    val schema =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{ name: "A", value: {set: [1,2,3]}}){value}}""", project).toString should be(
      """{"data":{"createA":{"value":[1,2,3]}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from scalar List to scalar non-list" should "succeed if there are no nodes yet" in {

    val schema =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a field from string to int" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "1"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to int" should "throw a warning if there is already data but proceed with -force" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "not a number"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to int" should "not throw a warning if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a field from string to a relation" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "1"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: A
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to a relation" should "throw a warning if there is already data but proceed with -force" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "not a number"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: A
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to a relation" should "not throw a warning if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: A
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a scalar field to required" should "not throw an error if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: String!
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a scalar field to required" should "not throw an error if all required fields already have data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "not a number"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: String!
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a scalar field to required" should "throw an error if a newly required field is null" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: String!
        |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are making a field required, but there are already nodes that would violate that constraint."}],"warnings":[]}}}""")
  }

  "Updating a relation field to required" should "not throw an error if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: [B!]!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: [B!]!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a relation field to required" should "not throw an error if all required fields already have data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: [B!]!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createB(data:{name: "B", a:{create: {name: "A"}}}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: [B!]!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a relation field to required" should "throw an error if a newly required field is null" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: [B!]!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createB(data:{name: "B"}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: [B!]!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are making a field required, but there are already nodes that would violate that constraint."}],"warnings":[]}}}""")
  }

  // endregion

  // region Delete Enum

  "Deleting an Enum" should "not throw a warning if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: AB
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Deleting an Enum" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: AB
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Deleting an Enum" should "throw a warning if there is already data but proceed with -force" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: AB
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }
  // endregion

  // region Update Enum

  "Updating an Enum to delete cases" should "not throw an error if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating an Enum to delete cases" should "not throw an error if there is already data but the remove enum value is not in use" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A, enums: {set:[A, A]}}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating an Enum to delete cases" should "throw an error if there is already data and the removed enum value is in use on a list" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A, enums: {set:[A, B]}}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are deleting the value 'B' of the enum 'AB', but that value is in use."}],"warnings":[]}}}""")
  }

  "Updating an Enum to delete cases" should "throw an error if there is already data and the removed enum value is in use on a non-list" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: B, enums: {set:[A, A]}}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | enum: AB
         | enums: [AB!]!
         |}
         |
         |enum AB{
         |  A
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are deleting the value 'B' of the enum 'AB', but that value is in use."}],"warnings":[]}}}""")
  }

  "Updating an Enum to delete cases" should "throw multiple errors if several of the removed cases are in use" in {

    val schema =
      """|type A {
         | name: String! @unique
         | enum: ABCD
         | enums: [ABCD!]!
         |}
         |
         |enum ABCD{
         |  A
         |  B
         |  C
         |  D
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: D, enums: {set:[C,B,A]}}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | enum: ABCD
         | enums: [ABCD!]!
         |}
         |
         |enum ABCD{
         |  A
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are deleting the value 'B' of the enum 'ABCD', but that value is in use."},{"description":"You are deleting the value 'C' of the enum 'ABCD', but that value is in use."},{"description":"You are deleting the value 'D' of the enum 'ABCD', but that value is in use."}],"warnings":[]}}}""")
  }
  // endregion

  // region Create Relation

  "Creating a relation that is not required" should "just work even if there are already nodes" in {

    val schema =
      """|type A {
         | name: String! @unique
         |}
         |
         |type B {
         | name: String! @unique
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A"}){name}}""", project)
    apiServer.query("""mutation{createB(data:{name: "B"}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: B
         |}
         |
         |type B {
         | name: String! @unique
         | a: [A!]!
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Creating a relation that is required" should "just work if there are no nodes yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         |}
         |
         |type B {
         | name: String! @unique
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: B!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Creating a relation that required" should "error if there are already nodes" in {

    val schema =
      """|type A {
         | name: String! @unique
         |}
         |
         |type B {
         | name: String! @unique
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A"}){name}}""", project)
    apiServer.query("""mutation{createB(data:{name: "B"}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: B!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  // endregion

}
