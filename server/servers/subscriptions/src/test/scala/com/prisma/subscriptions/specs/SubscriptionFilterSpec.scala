package com.prisma.subscriptions.specs

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.{Enum, Model, Project}
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsString, Json}

class SubscriptionFilterSpec extends FlatSpec with Matchers with SubscriptionSpecBase with AwaitUtils {

  val project = SchemaDsl.fromStringV11() {
    s"""
      |type Todo {
      |  id: ID! @id
      |  text: String
      |  tags: [String] $scalarListDirective
      |  status: Status
      |  comments: [Comment] $listInlineDirective
      |}
      |
      |type Comment {
      |  id: ID! @id
      |  text: String
      |  todo: Todo
      |}
      |
      |enum Status {
      |  Active
      |  Done
      |}
    """.stripMargin
  }

  val model: Model = project.schema.getModelByName_!("Todo")

  var testNodeId, importantTestNodeId: String = null

  override def beforeEach(): Unit = {
    super.beforeEach()
    testDatabase.setup(project)
    testNodeId = TestData.createTodo("some todo", JsString("[1,2,{\"a\":\"b\"}]"), None, project, model, testDatabase).value.toString
    importantTestNodeId = TestData.createTodo("important!", JsString("[1,2,{\"a\":\"b\"}]"), None, project, model, testDatabase).value.toString

    val raw: List[(String, GCValue)] = List(("text", StringGCValue("some comment")), ("id", StringIdGCValue("comment-id")))
    val args                         = PrismaArgs(RootGCValue(raw: _*))

    testDatabase.runDatabaseMutactionOnClientDb(
      TopLevelCreateNode(
        project = project,
        model = model,
        nonListArgs = args,
        listArgs = Vector.empty,
        nestedCreates = Vector.empty,
        nestedConnects = Vector.empty
      ))

//    val extendedPath = path.appendEdge(model.getRelationFieldByName_!("comments")).lastEdgeToNodeEdge(NodeSelector.forCuid(model, "comment-id"))
//    testDatabase.runDatabaseMutactionOnClientDb(AddDataItemToManyRelationByPath(project, extendedPath))
    // fixme: how must we replace the AddRelation?
  }

  "The Filter" should "support enums in previous values" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(
          id = "3",
          query = """subscription {
              |  todo(where: {mutation_in: UPDATED}) {
              |    mutation
              |    previousValues {
              |      id
              |      text
              |      status
              |    }
              |  }
              |}""".stripMargin
        )
      )

      sleep(8000)

      val event = nodeEvent(
        modelId = model.name,
        changedFields = Seq("text"),
        previousValues = s"""{"id":"$testNodeId", "text":"event1", "status": "Active", "tags":[]}"""
      )

      sssEventsTestKit.publish(Only(s"subscription:event:${project.id}:updateTodo"), event)

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = s"""{
              |  "todo":{
              |    "mutation":"UPDATED",
              |    "previousValues":{"id":"$testNodeId", "text":"event1", "status":"Active"}
              |  }
              |}""".stripMargin
        )
      )
    }
  }

  "this" should "work when using aliases" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(
          id = "3",
          query = """subscription {
                    |  alias: todo{
                    |    mutation
                    |    previousValues {
                    |      id
                    |      text
                    |      status
                    |    }
                    |  }
                    |}""".stripMargin
        )
      )

      sleep(8000)

      val event = nodeEvent(
        modelId = model.name,
        changedFields = Seq("text"),
        previousValues = s"""{"id":"$testNodeId", "text":"event1", "status": "Active", "tags":[]}"""
      )

      sssEventsTestKit.publish(Only(s"subscription:event:${project.id}:updateTodo"), event)

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = s"""{
                       |  "alias":{
                       |    "mutation":"UPDATED",
                       |    "previousValues":{"id":"$testNodeId", "text":"event1", "status":"Active"}
                       |  }
                       |}""".stripMargin
        )
      )
    }
  }

  "this" should "support scalar lists in previous values" ignore {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(
          id = "3",
          query = """subscription {
                    |  todo(where: {mutation_in: UPDATED}) {
                    |    mutation
                    |    previousValues {
                    |      id
                    |      text
                    |      tags
                    |    }
                    |  }
                    |}""".stripMargin
        )
      )

      sleep(4000)

      val event = nodeEvent(
        modelId = model.name,
        changedFields = Seq("text"),
        previousValues = s"""{"id":"$testNodeId", "text":"event2", "status": "Active", "tags": ["important"]}"""
      )

      sssEventsTestKit.publish(Only(s"subscription:event:${project.id}:updateTodo"), event)

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = s"""{"todo":{"mutation":"UPDATED","previousValues":{"id":"$testNodeId","text":"event2", "tags":["important"]}}}"""
        )
      )
    }
  }

  def nodeEvent(nodeId: String = "$testNodeId",
                mutationType: String = "UpdateNode",
                modelId: String,
                changedFields: Seq[String],
                previousValues: String): String = {
    Json.parse(previousValues) // throws if the string is not valid json
    s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": $previousValues}"""
  }
}
