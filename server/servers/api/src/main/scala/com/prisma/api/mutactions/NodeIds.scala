package com.prisma.api.mutactions

import java.util.UUID

import com.prisma.gc_values.{CuidGCValue, IdGcValue, UuidGCValue}
import com.prisma.shared.models.{Model, TypeIdentifier}
import cool.graph.cuid.Cuid

object NodeIds {
  def createNodeIdForModel(model: Model): IdGcValue = {
    model.idField_!.typeIdentifier match {
      case TypeIdentifier.UUID => UuidGCValue(UUID.randomUUID()) // todo: decide whether this is our best choice
      case TypeIdentifier.Cuid => CuidGCValue(Cuid.createCuid())
      case x                   => sys.error(s"Id field had type identifier $x. This should never happen.")
    }
  }
}
