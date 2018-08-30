package com.prisma

import com.prisma.ConnectorTag.{MongoConnectorTag, MySqlConnectorTag, PostgresConnectorTag}
import com.prisma.config.{DatabaseConfig, PrismaConfig}
import enumeratum.{Enum, EnumEntry}
import org.scalatest.{Suite, SuiteMixin, Tag}

object IgnorePostgres extends Tag("ignore.postgres")
object IgnoreMySql    extends Tag("ignore.mysql")
object IgnoreMongo    extends Tag("ignore.mongo")

object IgnoreSet {
  val ignoreConnectorTags = Set(IgnorePostgres, IgnoreMySql, IgnoreMongo)
}

sealed trait ConnectorTag extends EnumEntry
object ConnectorTag extends Enum[ConnectorTag] {
  def values = findValues

  sealed trait RelationalConnectorTag extends ConnectorTag
  object RelationalConnectorTag       extends RelationalConnectorTag
  object MySqlConnectorTag            extends RelationalConnectorTag
  object PostgresConnectorTag         extends RelationalConnectorTag
  sealed trait DocumentConnectorTag   extends ConnectorTag
  object MongoConnectorTag            extends DocumentConnectorTag
}

trait ConnectorAwareTest extends SuiteMixin { self: Suite =>
  import IgnoreSet._
  def prismaConfig: PrismaConfig
  lazy val connector               = prismaConfig.databases.head
  private val isPrototype: Boolean = if (connector.connector == "mongo") true else false
  private val connectorTag = connector.connector match {
    case "mongo"    => MongoConnectorTag
    case "mysql"    => MySqlConnectorTag
    case "postgres" => PostgresConnectorTag
  }

  def doNotRunForPrototypes: Boolean = false

  def runOnlyForConnectors: Set[ConnectorTag] = ConnectorTag.values.toSet

  abstract override def tags: Map[String, Set[String]] = {
    val superTags = super.tags

    val isNotTheRightConnector = !runOnlyForConnectors.contains(connectorTag)
    if (isNotTheRightConnector) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because the current connector is not right
           | allowed connectors: ${runOnlyForConnectors.mkString(",")}
           | current connector: ${connectorTag}
         """.stripMargin
      )
      ignoreAllTests
    } else if (isPrototype && doNotRunForPrototypes) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because it should not run for prototypes and the current connector is a prototype
           | current connector: ${connectorTag}
         """.stripMargin
      )
      ignoreAllTests
    } else {
      ignoredTestsBasedOnIndividualTagging(connector, superTags)
    }
  }

  def ifConnectorIsActive[T](assertion: => T): Unit = {
    if (connector.active && connector.connector != "mongo") {
      assertion
    }
  }

  def ifConnectorIsPassive[T](assertion: => T): Unit = {
    if (!connector.active) {
      assertion
    }
  }

  private def ignoredTestsBasedOnIndividualTagging(connector: DatabaseConfig, tags: Map[String, Set[String]]) = {
    val ignoreConnectorTypes = ignoreConnectorTags.filter(_.name.endsWith(connector.connector))
    val tagNamesToIgnore     = ignoreConnectorTypes.map(_.name)
    tags.mapValues { value =>
      val isIgnored = value.exists(tagNamesToIgnore.contains)
      if (isIgnored) {
        value ++ Set("org.scalatest.Ignore")
      } else {
        value
      }
    }
  }

  protected def ignoreAllTests = {
    testNames.map { testName =>
      testName -> Set("org.scalatest.Ignore")
    }.toMap
  }
}
