package com.prisma.api.queries

import com.prisma.{IgnoreMongo, IgnoreMySql, IgnorePostgres, IgnoreSQLite}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MultiItemConnectionQuerySpec extends FlatSpec with Matchers with ApiSpecBase {

  val project = SchemaDsl.fromStringV11() {
    """type Todo {
      |  id: ID! @id
      |  title: String!
      |}
    """.stripMargin
  }

  "the connection query" should "return empty edges" in {
    database.setup(project)

    val result = server.query(
      s"""{
         |  todoesConnection{
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todoesConnection":{"edges":[]}}}""")
  }

  "the connection query" should "return single node" in {
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .query(
        s"""mutation {
           |  createTodo(data: {title: "$title"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    val result = server.query(
      s"""{
         |  todoesConnection{
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todoesConnection":{"edges":[{"node":{"title":"Hello World!"}}]}}}""")
  }

  "the connection query" should "filter by any field" in {
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .query(
        s"""mutation {
           |  createTodo(data: {title: "$title"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    server
      .query(
        s"""{
         |  todoesConnection(where: {title: "INVALID"}){
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
        project
      )
      .toString should equal("""{"data":{"todoesConnection":{"edges":[]}}}""")

    server
      .query(
        s"""{
         |  todoesConnection(where: {title: "${title}"}){
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
        project
      )
      .toString should equal("""{"data":{"todoesConnection":{"edges":[{"node":{"title":"Hello World!"}}]}}}""")
  }

  "the connection query" should "work when using cursors on Mongo" taggedAs (IgnoreMySql, IgnorePostgres, IgnoreSQLite) in {

    val project2 = SchemaDsl.fromStringV11() {
      """type User {
        |  id: ID! @id
        |  name: String
        |  following: [User!]! @relation(name: "UserToFollow", link: INLINE)
        |  followers: [User!]! @relation(name: "UserToFollow")
        |}""".stripMargin
    }

    database.setup(project2)

    val a = server.query(s"""mutation{createUser(data:{name: "a", followers:{create:[{name:"b"}, {name:"c"}, {name:"x"}]}}){id}}""", project2)
    val d = server.query(s"""mutation{createUser(data:{name: "d", followers:{create:[{name:"e"}, {name:"f"}, {name:"x"}]}}){id}}""", project2)
    val g = server.query(s"""mutation{createUser(data:{name: "g", followers:{create:[{name:"h"}, {name:"i"}, {name:"x"}]}}){id}}""", project2)
    val k = server.query(s"""mutation{createUser(data:{name: "k", followers:{create:[{name:"l"}, {name:"m"}, {name:"x"}]}}){id}}""", project2)

    val result = server.query(
      s"""{
         |  usersConnection(where: {
         |    followers_some: {
         |      name: "x"
         |    },
         |  }, first: 2, after: "${a.pathAsString("data.createUser.id")}") {
         |    edges {
         |      node {
         |        name
         |      }
         |    }
         |  }
         |}""".stripMargin,
      project2
    )

    result.toString should be("""{"data":{"usersConnection":{"edges":[{"node":{"name":"d"}},{"node":{"name":"g"}}]}}}""")

  }

  "the connection query" should "work when using cursors when not on Mongo" taggedAs (IgnoreMongo) in {

    val project2 = SchemaDsl.fromStringV11() {
      """type User {
        |  id: ID! @id
        |  name: String
        |  following: [User!]! @relation(name: "UserToFollow", link: TABLE)
        |  followers: [User!]! @relation(name: "UserToFollow")
        |}""".stripMargin
    }

    database.setup(project2)

    val a = server.query(s"""mutation{createUser(data:{name: "a", followers:{create:[{name:"b"}, {name:"c"}, {name:"x"}]}}){id}}""", project2)
    val d = server.query(s"""mutation{createUser(data:{name: "d", followers:{create:[{name:"e"}, {name:"f"}, {name:"x"}]}}){id}}""", project2)
    val g = server.query(s"""mutation{createUser(data:{name: "g", followers:{create:[{name:"h"}, {name:"i"}, {name:"x"}]}}){id}}""", project2)
    val k = server.query(s"""mutation{createUser(data:{name: "k", followers:{create:[{name:"l"}, {name:"m"}, {name:"x"}]}}){id}}""", project2)

    val result = server.query(
      s"""{
         |  usersConnection(where: {
         |    followers_some: {
         |      name: "x"
         |    },
         |  }, first: 2, after: "${a.pathAsString("data.createUser.id")}") {
         |    edges {
         |      node {
         |        name
         |      }
         |    }
         |  }
         |}""".stripMargin,
      project2
    )

    result.toString should be("""{"data":{"usersConnection":{"edges":[{"node":{"name":"d"}},{"node":{"name":"g"}}]}}}""")

  }

}
