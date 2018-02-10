package com.prisma.subscriptions.specs

import com.prisma.api.database.mutactions.mutactions.{AddDataItemToManyRelationByUniqueField, CreateDataItem}
import com.prisma.api.mutations.{CoolArgs, NodeSelector, ParentInfo}
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.{Enum, Model, Project}
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import spray.json.JsString

class SubscriptionFilterSpec extends FlatSpec with Matchers with SpecBase with AwaitUtils {
  val schema: SchemaDsl.SchemaBuilder = SchemaDsl.schema()
  val statusEnum: Enum                = schema.enum("Status", Vector("Active", "Done"))
  val comment: SchemaDsl.ModelBuilder = schema.model("Comment").field("text", _.String)
  val todo: SchemaDsl.ModelBuilder = schema
    .model("Todo")
    .field("text", _.String)
    .field("tags", _.String, isList = true)
    .field("status", _.Enum, enum = Some(statusEnum))
    .oneToManyRelation("comments", "todo", comment)

  val project: Project = schema.buildProject()
  val model: Model     = project.schema.getModelByName_!("Todo")

  override def beforeEach(): Unit = {
    super.beforeEach()
    testDatabase.setup(project)
    TestData.createTodo("test-node-id", "some todo", JsString("[1,2,{\"a\":\"b\"}]"), None, project, model, testDatabase)
    TestData.createTodo("important-test-node-id", "important!", JsString("[1,2,{\"a\":\"b\"}]"), None, project, model, testDatabase)

    testDatabase.runDbActionOnClientDb {
      CreateDataItem(
        project = project,
        where = NodeSelector.forId(project.schema.getModelByName_!("Comment"), "comment-id"),
        args = CoolArgs(Map("text" -> "some comment", "id" -> "comment-id"))
      ).execute.await.sqlAction
    }

    val parentInfo = ParentInfo(model.getFieldByName_!("comments"), NodeSelector.forId(model, "test-node-id"))

    val where = NodeSelector.forId(model, "comment-id")
    testDatabase.runDbActionOnClientDb { AddDataItemToManyRelationByUniqueField(project, parentInfo, where).execute.await.sqlAction }
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
        modelId = model.id,
        changedFields = Seq("text"),
        previousValues = """{"id":"test-node-id", "text":"event1", "status": "Active", "tags":[]}"""
      )

      sssEventsTestKit.publish(Only(s"subscription:event:${project.id}:updateTodo"), event)

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = """{
              |  "todo":{
              |    "mutation":"UPDATED",
              |    "previousValues":{"id":"test-node-id","text":"event1", "status":"Active"}
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
        modelId = model.id,
        changedFields = Seq("text"),
        previousValues = """{"id":"test-node-id", "text":"event2", "status": "Active", "tags": ["important"]}"""
      )

      sssEventsTestKit.publish(Only(s"subscription:event:${project.id}:updateTodo"), event)

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = """{"todo":{"mutation":"UPDATED","previousValues":{"id":"test-node-id","text":"event2", "tags":["important"]}}}"""
        )
      )
    }
  }

  def nodeEvent(nodeId: String = "test-node-id",
                mutationType: String = "UpdateNode",
                modelId: String,
                changedFields: Seq[String],
                previousValues: String): String = {
    Json.parse(previousValues) // throws if the string is not valid json
    val json = JsString(previousValues).toString()
    s"""{"nodeId":"test-node-id","modelId":"${model.id}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": $json}"""
  }
}
