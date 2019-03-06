package com.prisma

import com.prisma.ConnectorTag.{MongoConnectorTag, MySqlConnectorTag, PostgresConnectorTag, SQLiteConnectorTag}
import com.prisma.config.{DatabaseConfig, PrismaConfig}
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import enumeratum.{Enum, EnumEntry}
import org.scalatest.{Suite, SuiteMixin, Tag}

object IgnorePostgres extends Tag("ignore.postgres")
object IgnoreMySql    extends Tag("ignore.mysql")
object IgnoreMongo    extends Tag("ignore.mongo")
object IgnoreSQLite   extends Tag("ignore.sqlite")

object IgnoreSet {
  val ignoreConnectorTags = Set(IgnorePostgres, IgnoreMySql, IgnoreMongo, IgnoreSQLite)
}

sealed trait ConnectorTag extends EnumEntry
object ConnectorTag extends Enum[ConnectorTag] {
  def values = findValues

  sealed trait RelationalConnectorTag extends ConnectorTag
  object RelationalConnectorTag       extends RelationalConnectorTag
  object MySqlConnectorTag            extends RelationalConnectorTag
  object PostgresConnectorTag         extends RelationalConnectorTag
  object SQLiteConnectorTag           extends RelationalConnectorTag
  sealed trait DocumentConnectorTag   extends ConnectorTag
  object MongoConnectorTag            extends DocumentConnectorTag
}

trait ConnectorAwareTest extends SuiteMixin { self: Suite =>
  import IgnoreSet._
  def prismaConfig: PrismaConfig

  lazy val connector = prismaConfig.databases.head
  private lazy val connectorTag = connector.connector match {
    case "mongo"                    => MongoConnectorTag
    case "mysql"                    => MySqlConnectorTag
    case "postgres"                 => PostgresConnectorTag
    case "sqlite" | "sqlite-native" => SQLiteConnectorTag
  }
  private lazy val isPrototype: Boolean = prismaConfig.prototype.getOrElse(false) // connectorTag == MongoConnectorTag

  def capabilities: ConnectorCapabilities
  def runOnlyForConnectors: Set[ConnectorTag]           = ConnectorTag.values.toSet
  def doNotRunForConnectors: Set[ConnectorTag]          = Set.empty
  def runOnlyForCapabilities: Set[ConnectorCapability]  = Set.empty
  def doNotRunForCapabilities: Set[ConnectorCapability] = Set.empty
  def doNotRunForPrototypes: Boolean                    = false
  def doNotRun: Boolean                                 = false

  abstract override def tags: Map[String, Set[String]] = { // this must NOT be a val. Otherwise ScalaTest does not behave correctly.
    if (shouldSuiteBeIgnored || doNotRun) {
      ignoreAllTests
    } else {
      ignoredTestsBasedOnIndividualTagging(connector)
    }
  }

  private lazy val shouldSuiteBeIgnored: Boolean = { // this must be a val. Otherwise printing would happen many times.
    val connectorHasTheRightCapabilities = runOnlyForCapabilities.forall(capabilities.has) || runOnlyForCapabilities.isEmpty
    val connectorHasAWrongCapability     = doNotRunForCapabilities.exists(capabilities.has)
    val isTheRightConnector              = runOnlyForConnectors.contains(connectorTag) && !doNotRunForConnectors.contains(connectorTag)

    if (!isTheRightConnector) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because the current connector is not right
           | allowed connectors: ${runOnlyForConnectors.mkString(",")}
           | disallowed connectors: ${doNotRunForConnectors.mkString(",")}
           | current connector: $connectorTag
         """.stripMargin
      )
      true
    } else if (isPrototype && doNotRunForPrototypes) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because it should not run for prototypes and the current connector is a prototype
           | current connector: $connectorTag
         """.stripMargin
      )
      true
    } else if (!connectorHasTheRightCapabilities) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because it does not have the right capabilities
           | required capabilities: ${runOnlyForCapabilities.mkString(",")}
           | connector capabilities: ${capabilities.capabilities.mkString(",")}
         """.stripMargin
      )
      true
    } else if (connectorHasAWrongCapability) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because it has a wrong capability
           | wrong capabilities: ${doNotRunForCapabilities.mkString(",")}
           | connector capabilities: ${capabilities.capabilities.mkString(",")}
         """.stripMargin
      )
      true
    } else {
      false
    }
  }

  def ifConnectorIsNotSQLite[T](assertion: => T): Unit = if (connectorTag != SQLiteConnectorTag) assertion
  def ifConnectorIsSQLite[T](assertion: => T): Unit    = if (connectorTag == SQLiteConnectorTag) assertion
  def ifConnectorIsNotMongo[T](assertion: => T): Unit  = if (connectorTag != MongoConnectorTag) assertion
  def ifConnectorIsActive[T](assertion: => T): Unit    = if (connector.active && connectorTag != MongoConnectorTag) assertion
  def ifConnectorIsPassive[T](assertion: => T): Unit   = if (!connector.active) assertion

  private def ignoredTestsBasedOnIndividualTagging(connector: DatabaseConfig) = {
    val ignoreConnectorTypes = ignoreConnectorTags.filter(_.name.endsWith(connector.connector))
    val tagNamesToIgnore     = ignoreConnectorTypes.map(_.name)
    super.tags.mapValues { value =>
      val isIgnored = value.exists(tagNamesToIgnore.contains)
      if (isIgnored) {
        value ++ Set("org.scalatest.Ignore")
      } else {
        value
      }
    }
  }

  private def ignoreAllTests = {
    testNames.map { testName =>
      testName -> Set("org.scalatest.Ignore")
    }.toMap
  }
}
