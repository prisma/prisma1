package com.prisma
import com.prisma.ConnectorTag.{MongoConnectorTag, RelationalConnectorTag}
import org.scalatest.WordSpecLike

case class TestDataModelsWrapper(
    dataModel: TestDataModels,
    connectorTag: ConnectorTag
) extends WordSpecLike {
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
}
