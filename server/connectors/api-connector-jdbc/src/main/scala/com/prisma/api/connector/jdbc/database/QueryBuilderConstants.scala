package com.prisma.api.connector.jdbc.database

trait QueryBuilderConstants {
  val topLevelAlias       = "Alias"
  val relationTableAlias  = "RelationTable"
  val intDummy            = 1
  val stringDummy         = ""
  val aSideAlias          = "__Relation__A"
  val bSideAlias          = "__Relation__B"
  val rowNumberAlias      = "prismaRowNumberAlias"
  val baseTableAlias      = "prismaBaseTableAlias"
  val rowNumberTableAlias = "prismaRowNumberTableAlias"
  val nodeIdFieldName     = "nodeId"
  val positionFieldName   = "position"
  val valueFieldName      = "value"
  val placeHolder         = "?"
  val relayTableName      = "_RelayId"
  val updatedAtField      = "updatedAt"
  val createdAtField      = "createdAt"
}
