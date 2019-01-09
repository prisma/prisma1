package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class AddingOptionalBackRelationDuringMigrationSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Adding a missing back-relation of non-list type" should "work when there are no violating occurences of Team" in {

    val schema =
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

    val (project, _) = setupProject(schema)

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:1
        |                           teamLeft:{create:{name: "Bayern"}},
        |                           teamRight:{create:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    val matches = apiServer.query("""{matches{number, teamLeft{name},teamRight{name},winner{name}}}""", project)
    matches.toString should be("""{"data":{"matches":[{"number":1,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}}]}}""")

    val teams = apiServer.query("""{teams{name}}""", project)
    teams.toString should be("""{"data":{"teams":[{"name":"Bayern"},{"name":"Real"}]}}""")

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  win: Match @relation(name: "TeamMatchWinner")
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: Team @relation(name: "TeamMatchLeft")
        |  teamRight: Team @relation(name: "TeamMatchRight")
        |  winner: Team @relation(name: "TeamMatchWinner")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val updatedTeams = apiServer.query("""{teams{name, win{number}}}""", updatedProject)
    updatedTeams.toString should be("""{"data":{"teams":[{"name":"Bayern","win":null},{"name":"Real","win":{"number":1}}]}}""")
  }

  "Adding a missing back-relation of list type" should "work when there is only one pair yet" in {

    val schema =
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

    val (project, _) = setupProject(schema)

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:1
        |                           teamLeft:{create:{name: "Bayern"}},
        |                           teamRight:{create:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    val matches = apiServer.query("""{matches{number, teamLeft{name},teamRight{name},winner{name}}}""", project)
    matches.toString should be("""{"data":{"matches":[{"number":1,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}}]}}""")

    val teams = apiServer.query("""{teams{name}}""", project)
    teams.toString should be("""{"data":{"teams":[{"name":"Bayern"},{"name":"Real"}]}}""")

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  wins: [Match] @relation(name: "TeamMatchWinner")
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: Team @relation(name: "TeamMatchLeft")
        |  teamRight: Team @relation(name: "TeamMatchRight")
        |  winner: Team @relation(name: "TeamMatchWinner")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val updatedTeams = apiServer.query("""{teams{name, wins{number}}}""", updatedProject)
    updatedTeams.toString should be("""{"data":{"teams":[{"name":"Bayern","wins":[]},{"name":"Real","wins":[{"number":1}]}]}}""")
  }

  "Adding a missing back-relation of list type" should "work when there are already multiple relation pairs" in {

    val schema =
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

    val (project, _) = setupProject(schema)

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:1
        |                           teamLeft:{create:{name: "Bayern"}},
        |                           teamRight:{create:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:2
        |                           teamLeft:{connect:{name: "Bayern"}},
        |                           teamRight:{connect:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    val matches = apiServer.query("""{matches{number, teamLeft{name},teamRight{name},winner{name}}}""", project)
    matches.toString should be(
      """{"data":{"matches":[{"number":1,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}},{"number":2,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}}]}}""")

    val teams = apiServer.query("""{teams{name}}""", project)
    teams.toString should be("""{"data":{"teams":[{"name":"Bayern"},{"name":"Real"}]}}""")

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  wins: [Match] @relation(name: "TeamMatchWinner")
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: Team @relation(name: "TeamMatchLeft")
        |  teamRight: Team @relation(name: "TeamMatchRight")
        |  winner: Team @relation(name: "TeamMatchWinner")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val updatedTeams = apiServer.query("""{teams{name, wins{number}}}""", updatedProject)
    updatedTeams.toString should be("""{"data":{"teams":[{"name":"Bayern","wins":[]},{"name":"Real","wins":[{"number":1},{"number":2}]}]}}""")
  }

  "Adding a missing back-relation of non-list type" should "not work when there are already multiple relation pairs" in {

    val schema =
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

    val (project, _) = setupProject(schema)

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:1
        |                           teamLeft:{create:{name: "Bayern"}},
        |                           teamRight:{create:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:2
        |                           teamLeft:{connect:{name: "Bayern"}},
        |                           teamRight:{connect:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    val matches = apiServer.query("""{matches{number, teamLeft{name},teamRight{name},winner{name}}}""", project)
    matches.toString should be(
      """{"data":{"matches":[{"number":1,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}},{"number":2,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}}]}}""")

    val teams = apiServer.query("""{teams{name}}""", project)
    teams.toString should be("""{"data":{"teams":[{"name":"Bayern"},{"name":"Real"}]}}""")

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  wins: Match @relation(name: "TeamMatchWinner")
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: Team @relation(name: "TeamMatchLeft")
        |  teamRight: Team @relation(name: "TeamMatchRight")
        |  winner: Team @relation(name: "TeamMatchWinner")
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)

    res.toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are adding a singular backrelation field to a type but there are already pairs in the relation that would violate that constraint."}],"warnings":[]}}}""")
  }

  "Adding a missing back-relation of non-list type" should "work when there are already multiple nodes but they are not in a relation" in {

    val schema =
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

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createMatch(data:{number:1}){number}}""", project)
    apiServer.query("""mutation{createMatch(data:{number:2}){number}}""", project)
    apiServer.query("""mutation{createTeam(data:{name:"Bayern"}){name}}""", project)
    apiServer.query("""mutation{createTeam(data:{name:"Real"}){name}}""", project)

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  wins: Match @relation(name: "TeamMatchWinner")
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: Team @relation(name: "TeamMatchLeft")
        |  teamRight: Team @relation(name: "TeamMatchRight")
        |  winner: Team @relation(name: "TeamMatchWinner")
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema1, 3)
  }

  "Adding several missing back-relations of list type" should "work even when there are already multiple relation pairs" in {

    val schema =
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

    val (project, _) = setupProject(schema)

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:1
        |                           teamLeft:{create:{name: "Bayern"}},
        |                           teamRight:{create:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    apiServer.query(
      """mutation{createMatch(data:{
        |                           number:2
        |                           teamLeft:{connect:{name: "Bayern"}},
        |                           teamRight:{connect:{name: "Real"}},
        |                           winner:{connect:{name: "Real"}}
        |                           }
        |){number}}""",
      project
    )

    val matches = apiServer.query("""{matches{number, teamLeft{name},teamRight{name},winner{name}}}""", project)
    matches.toString should be(
      """{"data":{"matches":[{"number":1,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}},{"number":2,"teamLeft":{"name":"Bayern"},"teamRight":{"name":"Real"},"winner":{"name":"Real"}}]}}""")

    val teams = apiServer.query("""{teams{name}}""", project)
    teams.toString should be("""{"data":{"teams":[{"name":"Bayern"},{"name":"Real"}]}}""")

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  wins: [Match] @relation(name: "TeamMatchWinner")
        |  lefts: [Match] @relation(name: "TeamMatchLeft")
        |}
        |
        |type Match {
        |  number: Int @unique
        |  teamLeft: Team @relation(name: "TeamMatchLeft")
        |  teamRight: Team @relation(name: "TeamMatchRight")
        |  winner: Team @relation(name: "TeamMatchWinner")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val updatedTeams = apiServer.query("""{teams{name, wins{number}, lefts{number}}}""", updatedProject)
    updatedTeams.toString should be(
      """{"data":{"teams":[{"name":"Bayern","wins":[],"lefts":[{"number":1},{"number":2}]},{"name":"Real","wins":[{"number":1},{"number":2}],"lefts":[]}]}}""")
  }
}
