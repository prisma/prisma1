package writes.nonEmbedded.nestedMutations

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class NestedConnectMutationInsideUpsertSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "A P1 to CM relation" should "be connectable by id within an upsert in the create case" in {
    schemaP1optToCM.testV11 { project =>
      val childId = server.query("""mutation { createChild(data: {c:"c1"}){ id } }""", project).pathAsString("data.createChild.id")

      val result = server.query(
        s"""mutation{upsertParent(where: {id: "5beea4aa6183dd734b2dbd9b"}, create: {p: "p1", childOpt:{connect:{id:"$childId"}}}, update: {p: "p-new"}) {
           |    childOpt{c}
           |  }
           |}
      """,
        project
      )

      result.toString should be("""{"data":{"upsertParent":{"childOpt":{"c":"c1"}}}}""")
    }
  }

  "A P1 to CM relation" should "be connectable by id within an upsert in the update case" in {
    schemaP1optToCM.testV11 { project =>
      val childId  = server.query("""mutation { createChild(data: {c:"c1"}){ id } }""", project).pathAsString("data.createChild.id")
      val parentId = server.query("""mutation { createParent(data: {p:"p1"}){ id } }""", project).pathAsString("data.createParent.id")

      val result = server.query(
        s"""mutation{upsertParent(where: {id: "$parentId"}, create: {p: "p new"}, update: {p: "p updated",childOpt:{connect:{id:"$childId"}}}) {
           |    childOpt{c}
           |  }
           |}
      """,
        project
      )

      result.toString should be("""{"data":{"upsertParent":{"childOpt":{"c":"c1"}}}}""")
    }
  }

  "A P1 to CM relation" should "be connectable by unique field within an upsert in the update case" in {
    schemaP1optToCM.testV11 { project =>
      server.query("""mutation { createChild(data: {c:"c1"}){ id } }""", project)
      server.query("""mutation { createParent(data: {p:"p1"}){ id } }""", project)

      val result = server.query(
        s"""mutation{upsertParent(where: {p: "p1"}, create: {p: "p new"}, update: {p: "p updated",childOpt:{connect:{c:"c1"}}}) {
           |    childOpt{c}
           |  }
           |}
      """,
        project
      )

      result.toString should be("""{"data":{"upsertParent":{"childOpt":{"c":"c1"}}}}""")
    }
  }

  "a one to many relation" should "throw the correct error for a connect by unique field within an upsert in the update case" in {

    schemaP1optToCM.testV11 { project =>
      server.query("""mutation { createChild(data: {c:"c1"}){ id } }""", project)
      server.query("""mutation { createParent(data: {p:"p1"}){ id } }""", project)

      server.queryThatMustFail(
        s"""mutation{upsertParent(where: {p: "p1"}, create: {p: "new p"}, update: {p: "p updated",childOpt:{connect:{c:"DOES NOT EXIST"}}}) {
           |    childOpt{c}
           |  }
           |}
      """,
        project,
        3039
      )
    }
  }
}
