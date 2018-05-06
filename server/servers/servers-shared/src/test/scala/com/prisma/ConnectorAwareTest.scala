package com.prisma

import com.prisma.config.{DatabaseConfig, PrismaConfig}
import org.scalatest.{Suite, SuiteMixin, Tag}

object IgnorePostgres extends Tag("ignore.postgres")
object IgnoreMySql    extends Tag("ignore.mysql")
object IgnoreActive   extends Tag("ignore.active")
object IgnorePassive  extends Tag("ignore.passive")

trait ConnectorAwareTest extends SuiteMixin { self: Suite =>
  def prismaConfig: PrismaConfig

  def runSuiteOnlyForActiveConnectors: Boolean  = false
  def runSuiteOnlyForPassiveConnectors: Boolean = false

  abstract override def tags: Map[String, Set[String]] = {
    val superTags = super.tags
    val connector = prismaConfig.databases.head

    if (runSuiteOnlyForActiveConnectors && !connector.active) {
      ignoreAllTests
    } else if (runSuiteOnlyForPassiveConnectors && connector.active) {
      ignoreAllTests
    } else {
      ignoredTestsBasedOnInvidualTagging(connector, superTags)
    }
  }

  private def ignoredTestsBasedOnInvidualTagging(connector: DatabaseConfig, tags: Map[String, Set[String]]) = {
    val ignoreActiveOrPassive = if (connector.active) IgnoreActive else IgnorePassive
    val ignoreConnectorTypes = {
      val ignoreConnectorTags = Set(IgnorePostgres, IgnoreMySql)
      ignoreConnectorTags.filter(_.name.endsWith(connector.connector))
    }
    val tagNamesToIgnore = (Set(ignoreActiveOrPassive) ++ ignoreConnectorTypes).map(_.name)
    tags.mapValues { value =>
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
