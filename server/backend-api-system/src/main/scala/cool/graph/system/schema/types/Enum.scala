package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import sangria.schema.{Field, ListType, ObjectType, StringType, fields, interfaces}

object Enum {

  lazy val Type: ObjectType[SystemUserContext, models.Enum] = {
    ObjectType(
      "Enum",
      "This is an enum",
      interfaces[SystemUserContext, models.Enum](nodeInterface),
      idField[SystemUserContext, models.Enum] ::
        fields[SystemUserContext, models.Enum](
        Field("name", StringType, resolve = _.value.name),
        Field("values", ListType(StringType), resolve = _.value.values)
      )
    )
  }
}
