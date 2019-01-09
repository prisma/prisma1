package com.prisma.api.connector.jdbc.database

import java.sql.ResultSet

import com.prisma.api.connector.jdbc.extensions.JdbcExtensions
import com.prisma.api.connector.{PrismaNode, PrismaNodeWithParent, RelationNode, ScalarListElement}
import com.prisma.gc_values.{IdGCValue, RootGCValue}
import com.prisma.shared.models._
import com.prisma.slick.ReadsResultSet

trait ResultSetReaders extends JdbcExtensions with QueryBuilderConstants {
  val readsAsUnit: ReadsResultSet[Unit] = ReadsResultSet(_ => ())

  def readStableModelIdentifier: ReadsResultSet[String] = ReadsResultSet(_.getString(1))

  def readNodeId(model: Model): ReadsResultSet[IdGCValue] = ReadsResultSet(_.getId(model))

  def readPrismaNodeWithParent(rf: RelationField, fields: List[ScalarField]): ReadsResultSet[PrismaNodeWithParent] = ReadsResultSet { rs =>
    val node     = readPrismaNode(rf.relatedModel_!, fields, rs)
    val parentId = rs.getParentId(parentModelAlias, rf.model.idField_!.typeIdentifier)
    PrismaNodeWithParent(parentId, node)
  }

  def readsPrismaNode(model: Model, fields: List[ScalarField]): ReadsResultSet[PrismaNode] = ReadsResultSet { rs =>
    readPrismaNode(model, fields, rs)
  }

  private def readPrismaNode(model: Model, fields: List[ScalarField], rs: ResultSet): PrismaNode = {
    val data = fields.toVector.map(field => field.name -> rs.getGcValue(field.dbName, field.typeIdentifier))
    PrismaNode(id = rs.getId(model), data = RootGCValue(data: _*), Some(model.name))
  }

  def readsScalarListField(field: ScalarField): ReadsResultSet[ScalarListElement] = ReadsResultSet { rs =>
    val nodeId   = rs.getString(nodeIdFieldName)
    val position = rs.getInt(positionFieldName)
    val value    = rs.getGcValue(valueFieldName, field.typeIdentifier)
    ScalarListElement(nodeId, position, value)
  }

  def readRelation(relation: Relation): ReadsResultSet[RelationNode] = ReadsResultSet { resultSet =>
    RelationNode(
      a = resultSet.getAsID("A", relation.modelA.idField_!.typeIdentifier),
      b = resultSet.getAsID("B", relation.modelB.idField_!.typeIdentifier)
    )
  }
}
