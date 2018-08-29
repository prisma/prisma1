package com.prisma

import com.prisma.config.{DatabaseConfig, PrismaConfig}
import org.scalatest.{Suite, SuiteMixin, Tag}

object IgnorePostgres extends Tag("ignore.postgres")
object IgnoreMySql    extends Tag("ignore.mysql")
object IgnoreMongo    extends Tag("ignore.mongo")

object IgnoreActive  extends Tag("ignore.active")
object IgnorePassive extends Tag("ignore.passive")
object IgnoreSet {
  val ignoreConnectorTags = Set(IgnorePostgres, IgnoreMySql, IgnoreMongo)
}

trait ConnectorAwareTest extends SuiteMixin { self: Suite =>
  import IgnoreSet._
  def prismaConfig: PrismaConfig
  lazy val connector = prismaConfig.databases.head

  def runSuiteOnlyForActiveConnectors: Boolean  = false
  def runSuiteOnlyForPassiveConnectors: Boolean = false
  def doNotRunSuiteForMongo: Boolean            = false

  abstract override def tags: Map[String, Set[String]] = {
    val superTags = super.tags

    if (runSuiteOnlyForActiveConnectors && !connector.active) {
      ignoreAllTests
    } else if (runSuiteOnlyForPassiveConnectors && (connector.active || connector.connector == "mongo")) {
      ignoreAllTests
    } else if (doNotRunSuiteForMongo && connector.connector == "mongo") {
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
    val ignoreActiveOrPassive = if (connector.active) IgnoreActive else IgnorePassive
    val ignoreConnectorTypes  = ignoreConnectorTags.filter(_.name.endsWith(connector.connector))
    val tagNamesToIgnore      = (Set(ignoreActiveOrPassive) ++ ignoreConnectorTypes).map(_.name)
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
