package com.prisma.deploy.schema.mutations

import com.prisma.IgnoreSQLite
import com.prisma.deploy.specutils.ActiveDeploySpecBase
import org.scalatest.{FlatSpec, Matchers}

class SeveralRelationsBetweenSameModelsSpec extends FlatSpec with Matchers with ActiveDeploySpecBase {

  "DeployMutation" should "be able to name a relation that previously had no name" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B 
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A
        |}"""

    val (project, _) = setupProject(schema)

    project.schema.relations.size should be(1)
    project.schema.relations.head.name should be("""AToB""")

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(name: "AB1")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(name: "AB1")
        |}"""

    val updatedProject = server.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(1)
    updatedProject.schema.relations.head.name should be("""AB1""")
  }

  "DeployMutation" should "be able to handle more than two relations between models" in {

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

    project.schema.relations.size should be(0)

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

    updatedProject.schema.relations.size should be(2)
    updatedProject.schema.relations(0).name should be("""AB1""")
    updatedProject.schema.relations(1).name should be("""AB2""")

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

    updatedProject2.schema.relations.size should be(3)
    updatedProject2.schema.relations(0).name should be("""AB1""")
    updatedProject2.schema.relations(1).name should be("""AB3""")
    updatedProject2.schema.relations(2).name should be("""AB2""")
  }

  "DeployMutation" should "be able to handle renaming relations" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(name: "AB1")
        | }
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(name: "AB1")
        | }"""

    val (project, _) = setupProject(schema)

    project.schema.relations.size should be(1)
    project.schema.relations.head.name should be("""AB1""")

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(oldName: "AB1", name: "NewName")
        |}
        |
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(oldName: "AB1", name: "NewName")
        |}"""

    val updatedProject = server.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(1)
    updatedProject.schema.relations.head.name should be("""NewName""")
  }
}
