package com.prisma

import com.prisma.config.PrismaConfig
import org.scalatest.{Suite, SuiteMixin, Tag}

object IgnorePostgres extends Tag("ignore.postgres")
object IgnoreMySql    extends Tag("ignore.mysql")

trait ConnectorAwareTest extends SuiteMixin { self: Suite =>
  def prismaConfig: PrismaConfig

  abstract override def tags: Map[String, Set[String]] = {
    val superTags   = super.tags
    val connector   = prismaConfig.databases.head.connector
    val tagToIgnore = "ignore." + connector
    superTags.mapValues { value =>
      if (value.contains(tagToIgnore)) {
        value ++ Set("org.scalatest.Ignore")
      } else {
        value
      }
    }
  }
}
