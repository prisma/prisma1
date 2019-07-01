package util

import org.scalatest.{Suite, WordSpecLike}
import util.ConnectorTag.{MongoConnectorTag, RelationalConnectorTag}

case class TestDataModels(
    mongo: Vector[String],
    sql: Vector[String]
)
object TestDataModels {
  def apply(mongo: String, sql: String): TestDataModels = {
    TestDataModels(mongo = Vector(mongo), sql = Vector(sql))
  }
}

case class TestDataModelsWrapper(
    dataModel: TestDataModels,
    connectorTag: ConnectorTag,
    connectorName: String,
    database: ApiTestDatabase
)(implicit suite: Suite)
    extends WordSpecLike {

  def test[T](indexToTest: Int)(fn: String => T) = internal(Some(indexToTest))(fn)
  def test[T](fn: String => T)                   = internal(None)(fn)

  private def internalV11[T](indexToTest: Option[Int])(fn: Project => T) = {
    internal(indexToTest) { dm =>
      val project = ProjectDsl.fromString(dm)
      database.setup(project)
      fn(project)
    }
  }

  private def internal[T](indexToTest: Option[Int])(fn: String => T) = {
    val dataModelsToTest = connectorTag match {
      case MongoConnectorTag         => dataModel.mongo
      case _: RelationalConnectorTag => dataModel.sql
    }

    var didRunATest = false
    dataModelsToTest.zipWithIndex.foreach {
      case (dm, index) =>
        val testThisOne = indexToTest.forall(_ == index)
        if (testThisOne) {
          didRunATest = testThisOne
          println("*" * 75)
          println(s"name:  $connectorName")
          println(s"index: $index")
          println(s"tag:   ${connectorTag.entryName}")
          println("*" * 75)
          fn(dm)
        }
    }

    if (!didRunATest) {
      println("There was no Datamodel for the provided index!")
    }
  }

}
