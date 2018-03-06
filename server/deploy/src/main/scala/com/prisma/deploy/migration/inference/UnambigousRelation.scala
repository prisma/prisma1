package com.prisma.deploy.migration.inference

import com.prisma.deploy.schema.UpdatedRelationAmbigous
import com.prisma.shared.models.{Relation, Schema}

object UnambigousRelation {

  def unambiguousRelationThatConnectsModels(baseSchema: Schema, previousModelAName: String, previousModelBName: String): Option[Relation] = {
    val candidates = baseSchema.getRelationsThatConnectModels(previousModelAName, previousModelBName)
    if (candidates.size > 1)
      throw UpdatedRelationAmbigous(
        s"There is a relation ambiguity during the migration. Please first name the old relation on your schema. The ambiguity is on a relation between $previousModelAName and $previousModelBName.")
    candidates.headOption
  }
}
