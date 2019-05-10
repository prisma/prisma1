package com.prisma.api.connector.jdbc.database

trait QueryBuilderConstants {
  val topLevelAlias       = "Alias"
  val relationTableAlias  = "RelationTable"
  val intDummy            = 1
  val relatedModelAlias   = "__RelatedModel__"
  val parentModelAlias    = "__ParentModel__"
  val rowNumberAlias      = "prismaRowNumberAlias"
  val baseTableAlias      = "prismaBaseTableAlias"
  val rowNumberTableAlias = "prismaRowNumberTableAlias"
  val nodeIdFieldName     = "nodeId"
  val positionFieldName   = "position"
  val valueFieldName      = "value"
  val placeHolder         = "?"
}
