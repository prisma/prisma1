package com.prisma.integration

import com.prisma.IgnoreMongo
import org.scalatest.{FlatSpec, Matchers}

class ChangingModelsOfRelationsSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Changing the model a relation points to" should "delete the existing relation data" taggedAs (IgnoreMongo) in {

    val schema =
      """type A {
        |  id: ID! @id
        |  a: String! @unique
        |  b: B @relation(name: "Relation" link: INLINE)
        |}
        |
        |type B {
        |  id: ID! @id
        |  b: String! @unique
        |  a: A @relation(name: "Relation")
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b: "B"}}}){a}}""", project)
    apiServer.query("""mutation{createC(data:{c:"C"}){c}}""", project)

    val schema1 =
      """type A {
        |  id: ID! @id
        |  a: String! @unique
        |  b: C @relation(name: "Relation" link: INLINE)
        |}
        |
        |type B {
        |  id: ID! @id
        |  b: String! @unique
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  a: A @relation(name: "Relation")
        |}"""

    val updatedProject = deployServer.deploySchemaThatMustWarnAndReturnProject(project, schema1, true)

    val as = apiServer.query("""{as{a, b{c}}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"a":"A","b":null}]}}""")
  }

  "Deleting one of the models of a relation and replacing it with a new one" should "work" in {

    val schema =
      """type A {
        |  id: ID! @id
        |  a: String! @unique
        |  b: B @relation(name: "Relation" link: INLINE)
        |}
        |
        |type B {
        |  id: ID! @id
        |  b: String! @unique
        |  a: A @relation(name: "Relation")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b: "B"}}}){a}}""", project)

    val schema1 =
      """type A {
        |  id: ID! @id
        |  a: String! @unique
        |  b: C @relation(name: "Relation" link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  b: String! @unique
        |  a: A @relation(name: "Relation")
        |}"""

    val updatedProject = deployServer.deploySchemaThatMustWarnAndReturnProject(project, schema1, true)

    val as = apiServer.query("""{as{a, b{b}}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"a":"A","b":null}]}}""")
  }

  "Renaming a model with @rename but keeping its relation" should "work" taggedAs (IgnoreMongo) in {

    val schema =
      """type A {
        |  id: ID! @id
        |  a: String! @unique
        |  b: B @relation(name: "Relation" link: INLINE)
        |}
        |
        |type B {
        |  id: ID! @id
        |  b: String! @unique
        |  a: A @relation(name: "Relation")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b: "B"}}}){a}}""", project)

    val schema1 =
      """type A {
        |  id: ID! @id
        |  a: String! @unique
        |  b: C @relation(name: "Relation" link: INLINE)
        |}
        |
        |type C @rename(oldName: "B"){
        |  id: ID! @id
        |  b: String! @unique
        |  a: A @relation(name: "Relation")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{a, b{b}}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"a":"A","b":{"b":"B"}}]}}""")

    val cs = apiServer.query("""{cs{b, a{a}}}""", updatedProject)
    cs.toString should be("""{"data":{"cs":[{"b":"B","a":{"a":"A"}}]}}""")
  }
}
