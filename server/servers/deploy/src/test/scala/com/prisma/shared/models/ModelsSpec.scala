package com.prisma.shared.models

import com.prisma.deploy.connector.MissingBackRelations
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class ModelsSpec extends FlatSpec with Matchers {
  "a related field" should "be found when the related fields have the same name" in {
    val project = SchemaDsl() { builder =>
      val model1 = builder.model("Model1")
      val model2 = builder.model("Model2")
      model1.oneToOneRelation("field", "field", model2)
    }
    val withBackRelations = MissingBackRelations.add(project.schema) // should not blow up

    withBackRelations.allRelationFields.foreach { rf =>
      rf.relatedField // let's see whether this blows up
    }
  }
}
