package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.jdbc.extensions.{JdbcExtensions, JooqExtensions, SlickExtensions}
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.shared.models.Manifestations.EmbeddedRelationLink
import com.prisma.shared.models._
import com.prisma.slick.ResultSetExtensions
import org.jooq.Field
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL.{field, name, table}

import scala.collection.JavaConverters._

trait BuilderBase extends JooqExtensions with JdbcExtensions with SlickExtensions with ResultSetReaders with QueryBuilderConstants with ResultSetExtensions {
  def schemaName: String
  val slickDatabase: SlickDatabase

  val isMySql    = slickDatabase.isMySql
  val isPostgres = slickDatabase.isPostgres
  val sql        = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))

  private val relayIdTableName                                               = "_RelayId"
  val relayIdColumn                                                          = field(name(schemaName, relayIdTableName, "id"))
  val relayStableIdentifierColumn                                            = field(name(schemaName, relayIdTableName, "stableModelIdentifier"))
  val relayTable                                                             = table(name(schemaName, relayIdTableName))
  def idField(model: Model)                                                  = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def modelTable(model: Model)                                               = table(name(schemaName, model.dbName))
  def relationTable(relation: Relation)                                      = table(name(schemaName, relation.relationTableName))
  def scalarListTable(field: ScalarField)                                    = table(name(schemaName, scalarListTableName(field)))
  def modelColumn(fieldModel: com.prisma.shared.models.Field): Field[AnyRef] = field(name(schemaName, fieldModel.model.dbName, fieldModel.dbName))
  def modelIdColumn(model: Model)                                            = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def modelIdColumn(alias: String, model: Model)                             = field(name(alias, model.idField_!.dbName))
  def relationColumn(relation: Relation, side: RelationSide.Value)           = field(name(schemaName, relation.relationTableName, relation.columnForRelationSide(side)))
  def relationIdColumn(relation: Relation)                                   = field(name(schemaName, relation.relationTableName, relation.idColumn_!))
  def inlineRelationColumn(relation: Relation, mani: EmbeddedRelationLink)   = field(name(schemaName, relation.relationTableName, mani.referencingColumn))
  def scalarListColumn(scalarField: ScalarField, column: String)             = field(name(schemaName, scalarListTableName(scalarField), column))
  def column(table: String, column: String)                                  = field(name(schemaName, table, column))
  def aliasColumn(column: String)                                            = field(name(topLevelAlias, column))
  def aliasColumn(scalarField: ScalarField)                                  = field(name(topLevelAlias, scalarField.dbName))
  def placeHolders(vector: Iterable[Any])                                    = vector.toList.map(_ => placeHolder).asJava
  private def scalarListTableName(field: ScalarField)                        = field.model.dbName + "_" + field.dbName
}
