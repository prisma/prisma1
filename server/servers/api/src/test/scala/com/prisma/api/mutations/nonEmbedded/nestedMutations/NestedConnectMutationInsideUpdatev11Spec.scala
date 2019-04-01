package com.prisma.api.mutations.nonEmbedded.nestedMutations

import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.MongoConnectorTag
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}

class NestedConnectMutationInsideUpdatev11Spec extends WordSpecLike with Matchers with ApiSpecBase with SchemaBasev11 {
  override def runOnlyForConnectors: Set[ConnectorTag] = Set(MongoConnectorTag)

  schemaP1reqToCMA.testEach { (testSuffix, dm) =>
    "a P1! to CM  relation with the child already in a relation should be connectable through a nested mutation by unique" + testSuffix in {
      val project = SchemaDsl.fromStringv11() { dm }
      database.setup(project)

      server.query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    childReq{
          |       c
          |    }
          |  }
          |}""",
        project
      )

      server.query(
        """mutation {
          |  createParent(data: {
          |    p: "p2"
          |    childReq: {
          |      create: {c: "c2"}
          |    }
          |  }){
          |    childReq{
          |       c
          |    }
          |  }
          |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      val res = server.query(
        s"""
           |mutation {
           |  updateParent(
           |  where: {p: "p2"}
           |  data:{
           |    childReq: {connect: {c: "c1"}}
           |  }){
           |    childReq {
           |      c
           |    }
           |  }
           |}
      """,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childReq":{"c":"c1"}}}}""")

      server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"children":[{"c":"c1","parentsOpt":[{"p":"p1"},{"p":"p2"}]},{"c":"c2","parentsOpt":[]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }
}
