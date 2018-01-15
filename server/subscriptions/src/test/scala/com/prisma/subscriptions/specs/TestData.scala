package com.prisma.subscriptions.specs

import com.prisma.api.ApiTestDatabase
import com.prisma.api.database.mutactions.mutactions.CreateDataItem
import com.prisma.api.mutations.MutationTypes.ArgumentValue
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
      model = model,
      values = List(
        ArgumentValue(name = "text", value = text),
        ArgumentValue(name = "id", value = id),
        ArgumentValue(name = "done", value = done.getOrElse(true)),
        ArgumentValue(name = "json", value = json)
      )
    )
    val action = mutaction.execute.await.sqlAction
    testDatabase.runDbActionOnClientDb(action)
  }
}
