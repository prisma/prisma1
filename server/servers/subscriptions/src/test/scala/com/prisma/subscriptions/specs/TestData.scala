package com.prisma.subscriptions.specs

import com.prisma.api.ApiTestDatabase
import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, Project}
import com.prisma.utils.await.AwaitUtils
import play.api.libs.json._

import scala.collection.immutable.SortedMap
object TestData extends AwaitUtils {
  def createTodo(
      text: String,
      json: JsValue,
      done: Option[Boolean] = None,
      project: Project,
      model: Model,
      testDatabase: ApiTestDatabase
  ): IdGCValue = {

    val raw = Map(
      "text" -> StringGCValue(text),
      "done" -> BooleanGCValue(done.getOrElse(true)),
      "json" -> JsonGCValue(json)
    )
    val withNullValues = model.scalarNonListFields.map { scalarField =>
      val value = raw.getOrElse(scalarField.name, NullGCValue)
      scalarField.name -> value
    }
    val mapWithNulls = SortedMap(withNullValues: _*)
    val args         = PrismaArgs(RootGCValue(mapWithNulls))

    val mutaction = TopLevelCreateNode(
      project = project,
      model = model,
      nonListArgs = args,
      listArgs = Vector.empty,
      nestedConnects = Vector.empty,
      nestedCreates = Vector.empty
    )

    testDatabase.runDatabaseMutactionOnClientDb(mutaction).results.collectFirst { case r: CreateNodeResult if r.mutaction == mutaction => r }.get.id
  }
}
