package writes

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class SameModelSelfRelationWithoutBackRelationSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "A Many to Many Self Relation" should "be accessible from only one side" in {
    val testDataModels = {
      val dm1 = """model Post {
                    id         String @id @default(cuid())
                    identifier Int?   @unique
                    related    Post[] @relation(name: "RelatedPosts", references: [id])
                    parents    Post[] @relation(name: "RelatedPosts")
                  }"""

      val dm2 = """model Post {
                    id         String @id @default(cuid())
                    identifier Int?   @unique
                    related    Post[] @relation(name: "RelatedPosts")
                    parents   Post[] @relation(name: "RelatedPosts")
                  }"""
      TestDataModels(mongo = dm1, sql = dm2)
    }

    testDataModels.testV11 { project =>
      server.query("mutation{createPost(data:{identifier: 1}){identifier}}", project)
      server.query("mutation{createPost(data:{identifier: 2}){identifier}}", project)

      server.query(
        """mutation {
                   |  updatePost (
                   |  
                   |    where:{identifier: 1}
                   |    data: {
                   |      related: {
                   |        connect: {
                   |          identifier: 2
                   |        }
                   |      }
                   |    }
                   |  ) {
                   |    identifier
                   |  }
                   |}""",
        project
      )

      server.query("{post(where:{identifier: 1}){identifier, related{identifier}}}", project).toString should be(
        """{"data":{"post":{"identifier":1,"related":[{"identifier":2}]}}}""")
      server.query("{post(where:{identifier: 2}){identifier, related{identifier}}}", project).toString should be(
        """{"data":{"post":{"identifier":2,"related":[]}}}""")
    }

  }

  "A One to One Self Relation" should "be accessible from only one side" in {
    val testDataModels = {
      val dm1 = """model Post {
                    id         String @id @default(cuid())
                    identifier Int?   @unique
                    related    Post?  @relation(name: "RelatedPosts" references: [id])
                    parents    Post[] @relation(name: "RelatedPosts")
                  }"""

      val dm2 = """model Post {
                    id         String @id @default(cuid())
                    identifier Int?   @unique
                    related    Post?  @relation(name: "RelatedPosts")
                    parents    Post[] @relation(name: "RelatedPosts")
                  }"""

      TestDataModels(mongo = dm1, sql = dm2)
    }

    testDataModels.testV11 { project =>
      server.query("mutation{createPost(data:{identifier: 1}){identifier}}", project)
      server.query("mutation{createPost(data:{identifier: 2}){identifier}}", project)
      server.query(
        """mutation {
        |  updatePost (
        |    where:{identifier: 1}
        |    data: {
        |      related: {
        |        connect: {
        |          identifier: 2
        |        }
        |      }
        |    }
        |  ) {
        |    identifier
        |  }
        |}""",
        project
      )

      server.query("{post(where:{identifier: 1}){identifier, related{identifier}}}", project).toString should be(
        """{"data":{"post":{"identifier":1,"related":{"identifier":2}}}}""")

      server.query("{post(where:{identifier: 2}){identifier, related{identifier}}}", project).toString should be(
        """{"data":{"post":{"identifier":2,"related":null}}}""")
    }

  }
}
