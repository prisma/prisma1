package com.prisma.deploy.migration.validation

import org.scalatest.{Matchers, WordSpecLike}

class LinkTableDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "it must be detected" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @linkTable {
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val dataModel         = validate(dataModelString)
    val relationTableType = dataModel.type_!("ModelToModelRelation")
    relationTableType.isRelationTable should be(true)
  }
}
