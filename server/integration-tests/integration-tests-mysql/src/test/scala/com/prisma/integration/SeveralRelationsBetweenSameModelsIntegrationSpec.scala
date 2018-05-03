package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class SeveralRelationsBetweenSameModelsIntegrationSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

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

    val updatedProject = deployServer.deploySchema(project, schema1)

    apiServer.query("""mutation{createA(data:{title:"A1" b1:{create:{title: "B1"}}, b2:{create:{title: "B2"}}}){id}}""", updatedProject)

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

    val updatedProject2 = deployServer.deploySchema(project, schema2)

    updatedProject2.schema.relations.size should be(3)
    updatedProject2.schema.relations(0).name should be("""AB1""")
    updatedProject2.schema.relations(1).name should be("""AB3""")
    updatedProject2.schema.relations(2).name should be("""AB2""")

    val unchangedRelationContent = apiServer.query("""{as{title, b1{title},b2{title},b3{title}}}""", updatedProject2)

    unchangedRelationContent.toString should be("""{"data":{"as":[{"title":"A1","b1":{"title":"B1"},"b2":{"title":"B2"},"b3":null}]}}""")
  }

  "DeployMutation" should "be able to handle setting a new relation with a name" in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  title: String
        | }
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        | }"""

    val (project, _) = setupProject(schema)

    project.schema.relations.size should be(0)

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(name: "NewName")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(name: "NewName")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(1)
    updatedProject.schema.relations.head.name should be("""NewName""")
  }

  "DeployMutation" should "be able to handle renaming relations that don't have a name yet" in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B 
        | }
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A
        | }"""

    val (project, _) = setupProject(schema)

    project.schema.relations.size should be(1)
    project.schema.relations.head.name should be("""AToB""")

    apiServer.query("""mutation{createA(data:{title:"A1" b1:{create:{title: "B1"}}}){id}}""", project)

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(name: "NewName")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(name: "NewName")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(1)
    updatedProject.schema.relations.head.name should be("""NewName""")

    val unchangedRelationContent = apiServer.query("""{as{title, b1{title}}}""", updatedProject)

    unchangedRelationContent.toString should be("""{"data":{"as":[{"title":"A1","b1":{"title":"B1"}}]}}""")
  }

  "DeployMutation" should "be able to handle renaming relations that are already named" in {

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

    apiServer.query("""mutation{createA(data:{title:"A1" b1:{create:{title: "B1"}}}){id}}""", project)

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b1: B @relation(oldName: "AB1", name: "NewName")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a1: A @relation(oldName: "AB1", name: "NewName")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(1)
    updatedProject.schema.relations.head.name should be("""NewName""")

    val unchangedRelationContent = apiServer.query("""{as{title, b1{title}}}""", updatedProject)

    unchangedRelationContent.toString should be("""{"data":{"as":[{"title":"A1","b1":{"title":"B1"}}]}}""")
  }

  "Going from two named relations between the same models to one unnamed one" should "error due to ambiguity" in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  b: B @relation(name: "AB1")
        |  b2: B @relation(name: "AB2")
        | }
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a: A @relation(name: "AB1")
        |  a2: A @relation(name: "AB2")
        | }"""

    val (project, _) = setupProject(schema)

    project.schema.relations.size should be(2)
    project.schema.relations.head.name should be("""AB1""")
    project.schema.relations.last.name should be("""AB2""")

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  b: B
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |}"""

    deployServer.deploySchemaThatMustError(project, schema1)
  }

  "Going from two named relations between the same models to one named one without a backrelation" should "work" in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b: B @relation(name: "AB1")
        |  b2: B @relation(name: "AB2")
        | }
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |  a: A @relation(name: "AB1")
        |  a2: A @relation(name: "AB2")
        | }"""

    val (project, _) = setupProject(schema)

    project.schema.relations.size should be(2)
    project.schema.relations.head.name should be("""AB1""")
    project.schema.relations.last.name should be("""AB2""")

    apiServer.query("""mutation{createA(data:{title:"A1" b:{create:{title: "B1"}}}){id}}""", project)

    val schema1 =
      """type A {
        |  id: ID! @unique
        |  title: String
        |  b: B @relation(name: "AB1")
        |}
        |
        |type B {
        |  id: ID! @unique
        |  title: String
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(1)
    updatedProject.schema.relations.head.name should be("""AB1""")

    val unchangedRelationContent = apiServer.query("""{as{title, b{title}}}""", updatedProject)

    unchangedRelationContent.toString should be("""{"data":{"as":[{"title":"A1","b":{"title":"B1"}}]}}""")
  }

  "Going from two named relations between the same models to one named one without a backrelation" should "work even when there is a rename" in {

    val schema =
      """type A {
          |  id: ID! @unique
          |  title: String
          |  b: B @relation(name: "AB1")
          |  b2: B @relation(name: "AB2")
          | }
          |
          |type B {
          |  id: ID! @unique
          |  title: String
          |  a: A @relation(name: "AB1")
          |  a2: A @relation(name: "AB2")
          | }"""

    val (project, _) = setupProject(schema)

    project.schema.relations.size should be(2)
    project.schema.relations.head.name should be("""AB1""")
    project.schema.relations.last.name should be("""AB2""")

    apiServer.query("""mutation{createA(data:{title:"A1" b:{create:{title: "B1"}}}){id}}""", project)

    val schema1 =
      """type A {
          |  id: ID! @unique
          |  title: String
          |  b: B @relation(name: "AB2" oldName: "AB1")
          |}
          |
          |type B {
          |  id: ID! @unique
          |  title: String
          |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(1)
    updatedProject.schema.relations.head.name should be("""AB2""")

    val unchangedRelationContent = apiServer.query("""{as{title, b{title}}}""", updatedProject)

    unchangedRelationContent.toString should be("""{"data":{"as":[{"title":"A1","b":{"title":"B1"}}]}}""")
  }

}
