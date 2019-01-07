package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.mongo.database.MongoDeployDatabaseMutationBuilder._
import com.prisma.deploy.connector.mongo.database.NoAction
import com.prisma.deploy.connector.mongo.impl.DeployMongoAction
import com.prisma.deploy.connector.{CreateRelationTable, DeleteRelationTable, UpdateRelationTable}
import com.prisma.shared.models.Relation

import scala.concurrent.Future

object CreateRelationInterpreter extends MongoMutactionInterpreter[CreateRelationTable] {
  override def execute(mutaction: CreateRelationTable)  = Indexhelper.add(mutaction.relation)
  override def rollback(mutaction: CreateRelationTable) = Indexhelper.remove(mutaction.relation)
}

object DeleteRelationInterpreter extends MongoMutactionInterpreter[DeleteRelationTable] {
  override def execute(mutaction: DeleteRelationTable)  = NoAction.unit //gets dropped with collection
  override def rollback(mutaction: DeleteRelationTable) = NoAction.unit //Indexhelper.add(mutaction.relation)
}

//Fixme again: Index Renaming does not work -> see Rename Model
object UpdateRelationInterpreter extends MongoMutactionInterpreter[UpdateRelationTable] {
  override def execute(mutaction: UpdateRelationTable)  = NoAction.unit
  override def rollback(mutaction: UpdateRelationTable) = NoAction.unit
}

//Fixme add relationindexes for ids on embedded types
//embedded types need their path upwards to set the relation index

object Indexhelper {
  def add(relation: Relation) = DeployMongoAction { database =>
    (relation.isInlineRelation, relation.modelAField.relationIsInlinedInParent) match {
      case (true, true) if !relation.modelB.isEmbedded  => addRelationIndex(database, relation.modelAField.model.dbName, relation.modelAField.dbName)
      case (true, false) if !relation.modelA.isEmbedded => addRelationIndex(database, relation.modelBField.model.dbName, relation.modelBField.dbName)
      case (_, _)                                       => Future.successful(())
    }
  }

  def remove(relation: Relation) = DeployMongoAction { database =>
    (relation.isInlineRelation, relation.modelAField.relationIsInlinedInParent) match {
      case (true, true) if !relation.modelB.isEmbedded  => removeRelationIndex(database, relation.modelAField.model.dbName, relation.modelAField.dbName)
      case (true, false) if !relation.modelA.isEmbedded => removeRelationIndex(database, relation.modelBField.model.dbName, relation.modelBField.dbName)
      case (_, _)                                       => Future.successful(())
    }
  }
}
