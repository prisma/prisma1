package com.prisma.shared.models

import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class ModelsSpec extends FlatSpec with Matchers with DeploySpecBase {
  "a related field" should "be found when the related fields have the same name" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Model1 {
        |  id: ID! @id
        |  field: Model2 @relation(link: INLINE)
        |}
        |
        |type Model2 {
        |  id: ID! @id
        |  field: Model1
        |}
      """.stripMargin
    }

    project.schema.allRelationFields.foreach { rf =>
      rf.relatedField // let's see whether this blows up
    }
  }
}
