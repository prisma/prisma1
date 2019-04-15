package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class UpdateEnumDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Updating an Enum to delete cases" should "not throw an error if there is no data yet" in {

    val schema =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB{
         |  A
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating an Enum to delete cases" should "not throw an error if there is already data but the remove enum value is not in use" in {

    val schema =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A, enums: {set:[A, A]}}){name}}""", project)

    val schema2 =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB{
         |  A
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating an Enum to delete cases" should "throw an error if there is already data and the removed enum value is in use on a list" in {

    val schema =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A, enums: {set:[A, B]}}){name}}""", project)

    val schema2 =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB{
         |  A
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are deleting the value 'B' of the enum 'AB', but that value is in use."}],"warnings":[]}}}""")
  }

  "Updating an Enum to delete cases" should "throw an error if there is already data and the removed enum value is in use on a non-list" in {

    val schema =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: B, enums: {set:[A, A]}}){name}}""", project)

    val schema2 =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB {
         |  A
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are deleting the value 'B' of the enum 'AB', but that value is in use."}],"warnings":[]}}}""")
  }

  "Updating an Enum to delete cases" should "throw multiple errors if several of the removed cases are in use" in {

    val schema =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: ABCD
         |  enums: [ABCD] $scalarListDirective
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
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: ABCD
         |  enums: [ABCD] $scalarListDirective
         |}
         |
         |enum ABCD {
         |  A
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are deleting the value 'B' of the enum 'ABCD', but that value is in use."},{"description":"You are deleting the value 'C' of the enum 'ABCD', but that value is in use."},{"description":"You are deleting the value 'D' of the enum 'ABCD', but that value is in use."}],"warnings":[]}}}""")
  }

  "Updating an Enum to rename it" should "succeed even if there is data" in {

    val schema =
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: ABCD
         |  enums: [ABCD] $scalarListDirective
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
      s"""|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |  enums: [AB] $scalarListDirective
         |}
         |
         |enum AB @rename(oldName: "ABCD"){
         |  A
         |  B
         |  C
         |  D
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }
}
