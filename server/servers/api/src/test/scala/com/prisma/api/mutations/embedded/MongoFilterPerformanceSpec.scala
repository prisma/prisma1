package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoFilterPerformanceSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Join Relation Filter on many to many relation" should "work on one level" in {

    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  int: Int! @unique
        |  posts: [Post] @mongoRelation(field: "posts")
        |}
        |
        |type Post {
        |  id: ID! @unique
        |  author: User
        |  int: Int! @unique
        |  comments: [Comment] @mongoRelation(field: "comments")
        |}
        |
        |type Comment {
        |  id: ID! @unique
        |  int: Int! @unique
        |  post: Post @mongoRelation(field: "comments")
        |}"""
    }

    database.setup(project)

    def createData(int: Int) = {
      val query = s"""
                 |mutation {
                 |  createUser(data: {
                 |                    int:$int
                 |                    posts:{create:[
                 |                      {
                 |                        int: ${1000 + int}0
                 |                        comments:{create:[
                 |                            {int: ${1000 + int}00}
                 |                            {int: ${1000 + int}01}
                 |                            {int: ${1000 + int}02}
                 |                            {int: ${1000 + int}03}
                 |                            {int: ${1000 + int}04}
                 |                            {int: ${1000 + int}05}
                 |                            {int: ${1000 + int}06}
                 |                            {int: ${1000 + int}07}
                 |                            {int: ${1000 + int}08}
                 |                            {int: ${1000 + int}09}
                 |
                 |                        ]}
                 |                      },
                 |                      {
                 |                        int: ${1000 + int}1
                 |                        comments:{create:[
                 |                            {int: ${1000 + int}10}
                 |                            {int: ${1000 + int}11}
                 |                            {int: ${1000 + int}12}
                 |                            {int: ${1000 + int}13}
                 |                            {int: ${1000 + int}14}
                 |                            {int: ${1000 + int}15}
                 |                            {int: ${1000 + int}16}
                 |                            {int: ${1000 + int}17}
                 |                            {int: ${1000 + int}18}
                 |                            {int: ${1000 + int}19}
                 |                        ]}
                 |                      },
                 |                      {
                 |                        int: ${1000 + int}2
                 |                        comments:{create:[
                 |                            {int: ${1000 + int}20}
                 |                            {int: ${1000 + int}21}
                 |                            {int: ${1000 + int}22}
                 |                            {int: ${1000 + int}23}
                 |                            {int: ${1000 + int}24}
                 |                            {int: ${1000 + int}25}
                 |                            {int: ${1000 + int}26}
                 |                            {int: ${1000 + int}27}
                 |                            {int: ${1000 + int}28}
                 |                            {int: ${1000 + int}29}
                 |                        ]}
                 |                      }
                 |                    ]}
    }) {int}} """

      server.query(query, project)
    }

    val mutStart = System.currentTimeMillis()
    for (x <- 1 to 100) {
      createData(x)
    }
    val mutEnd = System.currentTimeMillis()

    println("Creation: " + (mutEnd - mutStart))

    def filterquery: Unit = {
      val qStart = System.currentTimeMillis()
      server.query("""query{users(where:{int_gt: 5, int_lt: 19, posts_some:{int_gt: 10000, comments_some: {int_gt:10000}}}){int, posts{int}}}""", project)
      val qEnd = System.currentTimeMillis()
      println("Query: " + (qEnd - qStart))
    }

    for (x <- 1 to 10) {
      filterquery
    }

  }

}
