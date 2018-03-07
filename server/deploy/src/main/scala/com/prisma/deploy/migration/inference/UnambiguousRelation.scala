package com.prisma.deploy.migration.inference

import com.prisma.deploy.schema.UpdatedRelationAmbigous
import com.prisma.shared.models.{Relation, Schema}

object UnambiguousRelation {

  def unambiguousRelationThatConnectsModels(schema: Schema, modelAName: String, modelBName: String): Option[Relation] = {
    val candidates = schema.getRelationsThatConnectModels(modelAName, modelBName)
    if (candidates.size > 1)
      throw UpdatedRelationAmbigous(
        s"There is a relation ambiguity during the migration. Please first name the old relation on your schema. The ambiguity is on a relation between $modelAName and $modelBName.")
    candidates.headOption
  }
}
