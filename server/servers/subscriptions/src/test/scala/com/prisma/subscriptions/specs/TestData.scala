package com.prisma.subscriptions.specs

import com.prisma.api.ApiTestDatabase
import com.prisma.api.connector.{CoolArgs, CreateDataItem, NodeSelector, Path}
import com.prisma.shared.models.{Model, Project}
import com.prisma.utils.await.AwaitUtils
import spray.json.JsValue

object TestData extends AwaitUtils {
  def createTodo(
      id: String,
      text: String,
      json: JsValue,
      done: Option[Boolean] = None,
      project: Project,
      model: Model,
      testDatabase: ApiTestDatabase
  ) = {
    val mutaction = CreateDataItem(
      project = project,
      path = Path.empty(NodeSelector.forId(model, id)),
      args = CoolArgs(Map("text" -> text, "id" -> id, "done" -> done.getOrElse(true), "json" -> json))
    )
    testDatabase.runDatabaseMutactionOnClientDb(mutaction)
  }
}
