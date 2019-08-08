package queries

import org.scalatest.{FlatSpec, Matchers}
import util._

class RelationFilterOrderingSpec extends FlatSpec with Matchers with ApiSpecBase {

  val datamodels = {
    val dm1 = """model Blog {
                  id String @id @default(cuid())
                  title String
                  score Int
                  labels Label[] @relation(references: [id])
                }
                model Label {
                  id String @id @default(cuid())
                  text String @unique
                  blogs Blog[]
                }"""

    val dm2 = """model Blog {
                  id String @id @default(cuid())
                  title String
                  score Int
                  labels Label[]
                }
                model Label {
                  id String @id @default(cuid())
                  text String @unique
                  blogs Blog[]
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
