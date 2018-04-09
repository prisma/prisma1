package com.prisma.deploy.database.schema.mutations

import com.prisma.deploy.specutils.DeploySpecBase
import org.scalatest.{FlatSpec, Matchers}

class SeveralRelationsBetweenSameModelsSpec extends FlatSpec with Matchers with DeploySpecBase {

  "DeployMutation" should "be able to change a field from scalar list to scalar non-list" in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  # b1: B @relation(name: "AB1")
        |  # b2: B @relation(name: "AB2")
        |  # b3: B @relation(name: "AB3")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  # a1: A @relation(name: "AB1")
        |  # a2: A @relation(name: "AB2")
        |  # a3: A @relation(name: "AB3")
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(name: "AB1")
        |  b2: B @relation(name: "AB2")
        |  # b3: B @relation(name: "AB3")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(name: "AB1")
        |  a2: A @relation(name: "AB2")
        |  # a3: A @relation(name: "AB3")
        |}"""

    val updatedProject = server.deploySchema(project, schema1)

    val schema2 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(name: "AB1")
        |  b2: B @relation(name: "AB2")
        |  b3: B @relation(name: "AB3")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(name: "AB1")
        |  a2: A @relation(name: "AB2")
        |  a3: A @relation(name: "AB3")
        |}"""

    val updatedProject2 = server.deploySchema(project, schema2)

  }
}
