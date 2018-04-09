package com.prisma.deploy.migration.inference

import com.prisma.shared.models.{Relation, Schema}

object UnambiguousRelation {

  def unambiguousRelationThatConnectsModels(schema: Schema, modelAName: String, modelBName: String): Option[Relation] = {
    val candidates = schema.getRelationsThatConnectModels(modelAName, modelBName)
    if (candidates.size > 1) None else candidates.headOption
  }
}
