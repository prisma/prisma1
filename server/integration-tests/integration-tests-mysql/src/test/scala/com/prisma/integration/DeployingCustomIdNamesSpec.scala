package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class DeployingCustomIdNamesSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Using Custom Id field names with relations" should "work" in {

    val schema =
      """type Person {
        |  id: ID! @id
        |  age: Int! @unique
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type Game {
        |  gameId: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  lastUpdatedOn: String!
        |  players: [Player!]! @relation(link: TABLE, name: "GamePlayer")
        |}
        |
        |type Player {
        |  playerId: ID! @id
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  lastUpdatedOn: String!
        |  games: [Game!]! @relation(name: "GamePlayer")
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema1, 3, true)
  }
}
