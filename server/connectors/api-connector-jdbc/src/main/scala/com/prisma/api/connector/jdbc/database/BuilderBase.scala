package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.jdbc.extensions.{JdbcExtensions, JooqExtensions, SlickExtensions}
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.shared.models.Manifestations.EmbeddedRelationLink
import com.prisma.shared.models._
import com.prisma.shared.models.RelationSide.Value
import com.prisma.slick.ResultSetExtensions
import org.jooq.Field
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL.{field, name, table}

import scala.collection.JavaConverters._

trait BuilderBase extends JooqExtensions with JdbcExtensions with SlickExtensions with ResultSetReaders with QueryBuilderConstants with ResultSetExtensions {
  def project: Project
  val slickDatabase: SlickDatabase

  val isMySql    = slickDatabase.isMySql
  val isPostgres = slickDatabase.isPostgres
  val isSQLite   = slickDatabase.isSQLite
  val sql        = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))

  def idField(model: Model)                                                  = field(name(project.dbName, model.dbName, model.dbNameOfIdField_!))
  def modelTable(model: Model)                                               = table(name(project.dbName, model.dbName))
  def relationTable(relation: Relation)                                      = table(name(project.dbName, relation.relationTableName))
  def scalarListTable(field: ScalarField)                                    = table(name(project.dbName, scalarListTableName(field)))
  def modelColumn(fieldModel: com.prisma.shared.models.Field): Field[AnyRef] = field(name(project.dbName, fieldModel.model.dbName, fieldModel.dbName))
  def modelIdColumn(model: Model)                                            = field(name(project.dbName, model.dbName, model.dbNameOfIdField_!))
  def modelIdColumn(alias: String, model: Model)                             = field(name(alias, model.idField_!.dbName))
  def relationColumn(rf: RelationField)                                      = field(name(project.dbName, rf.relation.relationTableName, rf.relation.columnForRelationSide(rf.relationSide)))
  def relationIdColumn(relation: Relation)                                   = field(name(project.dbName, relation.relationTableName, relation.idColumn_!))
  def inlineRelationColumn(relation: Relation, mani: EmbeddedRelationLink)   = field(name(project.dbName, relation.relationTableName, mani.referencingColumn))
  def scalarListColumn(scalarField: ScalarField, column: String)             = field(name(project.dbName, scalarListTableName(scalarField), column))
  def column(table: String, column: String)                                  = field(name(project.dbName, table, column))
  def aliasColumn(column: String)                                            = field(name(topLevelAlias, column))
  def aliasColumn(scalarField: ScalarField)                                  = field(name(topLevelAlias, scalarField.dbName))
  def aliasColumn(relationField: RelationField)                              = field(name(topLevelAlias, relationField.dbName))
  def placeHolders(vector: Iterable[Any])                                    = vector.toList.map(_ => placeHolder).asJava
  private def scalarListTableName(field: ScalarField)                        = field.model.dbName + "_" + field.dbName
}
