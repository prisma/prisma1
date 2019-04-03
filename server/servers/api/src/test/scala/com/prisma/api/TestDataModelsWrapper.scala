package com.prisma.api
import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.{MongoConnectorTag, RelationalConnectorTag}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{Suite, WordSpecLike}

case class TestDataModels(
    mongo: Vector[String],
    sql: Vector[String]
)

case class TestDataModelsWrapper(
    dataModel: TestDataModels,
    connectorTag: ConnectorTag,
    connectorName: String,
    database: ApiTestDatabase
)(implicit deployConnector: DeployConnector, suite: Suite)
    extends WordSpecLike {

  def testEach[T](fn: (String, String) => T) = {
    connectorTag match {
      case MongoConnectorTag =>
        dataModel.mongo.zipWithIndex.foreach {
          case (dm, index) =>
            fn(s" mongo $index", dm)
        }

      case _: RelationalConnectorTag =>
        dataModel.sql.zipWithIndex.foreach {
          case (dm, index) =>
            fn(s" sql $index", dm)
        }
    }
  }

  def test[T](indexToTest: Int)(fn: String => T)     = internal(Some(indexToTest))(fn)
  def test[T](fn: String => T)                       = internal(None)(fn)
  def testV11[T](indexToTest: Int)(fn: Project => T) = internalV11(Some(indexToTest))(fn)
  def testV11[T](fn: Project => T)                   = internalV11(None)(fn)

  private def internalV11[T](indexToTest: Option[Int])(fn: Project => T) = {
    internal(indexToTest) { dm =>
      val project = SchemaDsl.fromStringV11()(dm)
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
