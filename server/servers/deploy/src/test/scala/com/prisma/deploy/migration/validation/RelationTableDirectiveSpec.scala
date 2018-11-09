package com.prisma.deploy.migration.validation

import org.scalatest.{Matchers, WordSpecLike}

class RelationTableDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "@relationTable must be detected" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val dataModel         = validate(dataModelString)
    val relationTableType = dataModel.type_!("ModelToModelRelation")
    relationTableType.isRelationTable should be(true)
  }
}
