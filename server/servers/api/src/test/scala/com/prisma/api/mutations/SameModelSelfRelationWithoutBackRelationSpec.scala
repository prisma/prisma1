package com.prisma.api.mutations

import com.prisma.api.{ApiSpecBase, TestDataModels}
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import org.scalatest.{FlatSpec, Matchers}

class SameModelSelfRelationWithoutBackRelationSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "A Many to Many Self Relation" should "be accessible from only one side" in {
    val testDataModels = {
      val dm1 = """type Post {
                    id: ID! @id
                    identifier: Int @unique
                    related: [Post] @relation(name: "RelatedPosts" link: INLINE)
                  }"""

      val dm2 = """type Post {
                    id: ID! @id
                    identifier: Int @unique
                    related: [Post] @relation(name: "RelatedPosts")
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
      val dm1 = """type Post {
                    id: ID! @id
                    identifier: Int @unique
                    related: Post @relation(name: "RelatedPosts" link: INLINE)
                  }"""

      val dm2 = """type Post {
                    id: ID! @id
                    identifier: Int @unique
                    related: Post @relation(name: "RelatedPosts")
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
