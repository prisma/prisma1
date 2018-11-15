package com.prisma.deploy.migration.validation

import org.scalatest.{Matchers, WordSpecLike}

class DbDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "@db should work on fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String @db(name: "some_columns")
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").scalarField_!("field")
    field.columnName should be(Some("some_columns"))
  }

  "@db should work on types" in {
    val dataModelString =
      """
        |type Model @db(name:"some_table") {
        |  id: ID! @id
        |  field: String
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val model     = dataModel.type_!("Model")
    model.tableName should equal(Some("some_table"))
  }
}
