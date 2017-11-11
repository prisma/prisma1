package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import sangria.schema.{Field, ObjectType, StringType, fields, interfaces}

object ProjectDatabase {
  lazy val Type: ObjectType[SystemUserContext, models.ProjectDatabase] = ObjectType(
    "ProjectDatabase",
    "This is the database for a project",
    interfaces[SystemUserContext, models.ProjectDatabase](nodeInterface),
    idField[SystemUserContext, models.ProjectDatabase] ::
      fields[SystemUserContext, models.ProjectDatabase](
      Field("name", StringType, resolve = _.value.name),
      Field("region", RegionType, resolve = _.value.region)
    )
  )
}
