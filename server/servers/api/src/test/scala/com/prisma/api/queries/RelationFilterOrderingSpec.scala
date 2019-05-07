package com.prisma.api.queries

import com.prisma.api.{ApiSpecBase, TestDataModels}
import org.scalatest.{FlatSpec, Matchers}

class RelationFilterOrderingSpec extends FlatSpec with Matchers with ApiSpecBase {

  val datamodels = {
    val dm1 = """type Blog {
                  id: ID! @id
                  title: String!
                  score: Int!
                  labels: [Label!]! @relation(name: "BlogLabels", link: INLINE)
                }
                type Label {
                  id: ID! @id
                  text: String! @unique
                }"""

    val dm2 = """type Blog {
                  id: ID! @id
                  title: String!
                  score: Int!
                  labels: [Label!]! @relation(name: "BlogLabels", link: TABLE)
                }
                type Label {
                  id: ID! @id
                  text: String! @unique
                }"""

    TestDataModels(mongo = Vector(dm1), sql = Vector(dm2))
  }

  "Using relational filters" should "return items in the specified order" in {
    datamodels.testV11 { project =>
      server.query(s"""mutation {createLabel(data: {text: "x"}) {text }}""", project)

      server.query(s"""mutation {createBlog(data: {title: "blog_1", score: 10,labels: {connect: {text: "x"}}}) {title}}""", project)
      server.query(s"""mutation {createBlog(data: {title: "blog_1", score: 20,labels: {connect: {text: "x"}}}) {title}}""", project)
      server.query(s"""mutation {createBlog(data: {title: "blog_1", score: 30,labels: {connect: {text: "x"}}}) {title}}""", project)

      val res1 = server.query("""query {blogs(first: 2, orderBy: score_DESC) {title, score}}""", project)

      res1.toString should be("""{"data":{"blogs":[{"title":"blog_1","score":30},{"title":"blog_1","score":20}]}}""")

      val res2 = server.query("""query {blogs (first: 2, orderBy: score_DESC, where:{labels_some: {text: "x"}}) {title, score}}""", project)
      res2.toString should be("""{"data":{"blogs":[{"title":"blog_1","score":30},{"title":"blog_1","score":20}]}}""")

    }
  }
}
