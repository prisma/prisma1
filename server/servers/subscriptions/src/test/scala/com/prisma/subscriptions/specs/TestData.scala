package com.prisma.subscriptions.specs

import com.prisma.api.ApiTestDatabase
import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, Project}
import com.prisma.utils.await.AwaitUtils
import play.api.libs.json._
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

    val raw: List[(String, GCValue)] =
      List(("text", StringGCValue(text)), ("id", IdGCValue(id)), ("done", BooleanGCValue(done.getOrElse(true))), ("json", JsonGCValue(json)))
    val args = PrismaArgs(RootGCValue(raw: _*))

    val mutaction = CreateDataItem(
      project = project,
      path = Path.empty(NodeSelector.forId(model, id)),
      nonListArgs = args,
      listArgs = Vector.empty
    )

    testDatabase.runDatabaseMutactionOnClientDb(mutaction)
  }
}
