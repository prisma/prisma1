package com.prisma.integration

import com.prisma.IgnoreMongo
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

    deployServer.deploySchemaThatMustErrorWithCode(project, schema1, errorCode = 3018)
  }

  "Going from two named relations between the same models to one named one without a backrelation" should "work" taggedAs (IgnoreMongo) in {

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

  "Going from two named relations between the same models to one named one without a backrelation" should "work even when there is a rename" taggedAs (IgnoreMongo) in {

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

  "Several missing backrelations on the same type" should "work when there are relation directives provided" in {

    val schema =
      """type TeamMatch {
        |  name: String! @unique
        |}
        |
        |type Match {
        |  number: Int @unique
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Team {
        |  name: String! @unique
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: Team @relation(name: "TeamMatchLeft")
        |  teamRight: Team @relation(name: "TeamMatchRight")
        |  winner: Team @relation(name: "TeamMatchWinner")
        |}"""
    val updatedProject = deployServer.deploySchema(project, schema1)

    updatedProject.schema.relations.size should be(3)

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:1
        |                           teamLeft:{create:{name: "Bayern"}},
        |                           teamRight:{create:{name: "Real"}},
        |                           winner:{create:{name: "Real2"}}
        |                           }                           
        |){number}}""",
      updatedProject
    )

    val matches = apiServer.query("""{matches{number, teamLeft{name},teamRight{name},winner{name}}}""", updatedProject)
    matches.toString should be("""{"data":{"matches":[{"number":1,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real2"}}]}}""")

    val teams = apiServer.query("""{teams{name}}""", updatedProject)
    teams.toString should be("""{"data":{"teams":[{"name":"Bayern"},{"name":"Real"},{"name":"Real2"}]}}""")

  }

  // should move to schemavalidationspec

  "One missing backrelation and one unnamed relation on the other side" should "error" in {

    val (project, _) = setupProject(basicTypesGql)
    val schema1 =
      """type TeamMatch {
        |  key: String! @unique
        |  match: Match
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: TeamMatch @relation(name: "TeamMatchLeft")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are trying to set the relation 'TeamMatchLeft' from `Match` to `TeamMatch` and are only providing a relation directive on `Match`. Since there is also a relation field without a relation directive on `TeamMatch` pointing towards `Match` that is ambiguous. Please provide the same relation directive on `TeamMatch` if this is supposed to be the same relation. If you meant to create two separate relations without backrelations please provide a relation directive with a different name on `TeamMatch`."}],"warnings":[]}}}""")
  }

  "Several missing backrelations on the same type and one unnamed relation on the other side" should "error" in {
    val (project, _) = setupProject(basicTypesGql)

    val schema1 =
      """type TeamMatch {
        |  key: String! @unique
        |  match: Match
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: TeamMatch @relation(name: "TeamMatchLeft")
        |  teamRight: TeamMatch @relation(name: "TeamMatchRight")
        |  winner: TeamMatch @relation(name: "TeamMatchWinner")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are trying to set the relation 'TeamMatchLeft' from `Match` to `TeamMatch` and are only providing a relation directive on `Match`. Since there is also a relation field without a relation directive on `TeamMatch` pointing towards `Match` that is ambiguous. Please provide the same relation directive on `TeamMatch` if this is supposed to be the same relation. If you meant to create two separate relations without backrelations please provide a relation directive with a different name on `TeamMatch`."},{"description":"You are trying to set the relation 'TeamMatchRight' from `Match` to `TeamMatch` and are only providing a relation directive on `Match`. Since there is also a relation field without a relation directive on `TeamMatch` pointing towards `Match` that is ambiguous. Please provide the same relation directive on `TeamMatch` if this is supposed to be the same relation. If you meant to create two separate relations without backrelations please provide a relation directive with a different name on `TeamMatch`."},{"description":"You are trying to set the relation 'TeamMatchWinner' from `Match` to `TeamMatch` and are only providing a relation directive on `Match`. Since there is also a relation field without a relation directive on `TeamMatch` pointing towards `Match` that is ambiguous. Please provide the same relation directive on `TeamMatch` if this is supposed to be the same relation. If you meant to create two separate relations without backrelations please provide a relation directive with a different name on `TeamMatch`."}],"warnings":[]}}}""")
  }

  "Several missing backrelation to different models" should "work" in {

    val (project, _) = setupProject(basicTypesGql)
    val schema1 =
      """type TeamMatch {
        |  key: String! @unique
        |}
        |
        |type TeamMatch2 {
        |  key: String! @unique
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: TeamMatch @relation(name: "TeamMatchLeft")
        |  teamLeft2: TeamMatch2 @relation(name: "TeamMatchLeft2")
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema1, 3)
  }

}
