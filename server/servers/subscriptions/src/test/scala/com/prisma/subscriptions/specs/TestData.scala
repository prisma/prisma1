package com.prisma.subscriptions.specs

import com.prisma.api.ApiTestDatabase
import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, Project}
import com.prisma.utils.await.AwaitUtils
import play.api.libs.json._
object TestData extends AwaitUtils {
  def createTodo(
      text: String,
      json: JsValue,
      done: Option[Boolean] = None,
      project: Project,
      model: Model,
      testDatabase: ApiTestDatabase
  ): IdGCValue = {

    val raw: List[(String, GCValue)] = List(("text", StringGCValue(text)), ("done", BooleanGCValue(done.getOrElse(true))), ("json", JsonGCValue(json)))
    val args                         = PrismaArgs(RootGCValue(raw: _*))

    val mutaction = TopLevelCreateNode(
      project = project,
      model = model,
      nonListArgs = args,
      listArgs = Vector.empty,
      nestedConnects = Vector.empty,
      nestedCreates = Vector.empty
    )

    testDatabase.runDatabaseMutactionOnClientDb(mutaction).databaseResult.asInstanceOf[CreateNodeResult].id
  }
}
