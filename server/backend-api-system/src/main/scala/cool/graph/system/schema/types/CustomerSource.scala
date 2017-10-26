package cool.graph.system.schema.types

import sangria.schema._

import cool.graph.shared.models

object CustomerSource {
  lazy val Type = EnumType(
    "CustomerSourceType",
    values = List(
      EnumValue(models.CustomerSource.LEARN_RELAY.toString, value = models.CustomerSource.LEARN_RELAY),
      EnumValue(models.CustomerSource.LEARN_APOLLO.toString, value = models.CustomerSource.LEARN_APOLLO),
      EnumValue(models.CustomerSource.DOCS.toString, value = models.CustomerSource.DOCS),
      EnumValue(models.CustomerSource.WAIT_LIST.toString, value = models.CustomerSource.WAIT_LIST)
    )
  )
}
