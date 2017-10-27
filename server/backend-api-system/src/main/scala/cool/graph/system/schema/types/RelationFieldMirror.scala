package cool.graph.system.schema.types

import sangria.schema._
import sangria.relay._

import cool.graph.shared.models
import cool.graph.system.SystemUserContext

object RelationFieldMirror {
  lazy val Type: ObjectType[SystemUserContext, models.RelationFieldMirror] =
    ObjectType(
      "RelationFieldMirror",
      "This is a relation field mirror",
      interfaces[SystemUserContext, models.RelationFieldMirror](nodeInterface),
      idField[SystemUserContext, models.RelationFieldMirror] ::
        fields[SystemUserContext, models.RelationFieldMirror](
        Field("fieldId", IDType, resolve = ctx => ctx.value.fieldId),
        Field("relationId", IDType, resolve = ctx => ctx.value.relationId)
      )
    )
}
