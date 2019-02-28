package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.database.NoAction

object MongoAnyMutactionInterpreter extends MongoMutactionInterpreter[DeployMutaction] {
  override def execute(mutaction: DeployMutaction) = {
    mutaction match {
      case x: TruncateProject       => TruncateProjectInterpreter.execute(x)
      case x: CreateColumn          => CreateColumnInterpreter.execute(x)
      case x: UpdateColumn          => UpdateColumnInterpreter.execute(x)
      case x: DeleteColumn          => DeleteColumnInterpreter.execute(x)
      case x: CreateScalarListTable => NoAction.unit
      case x: UpdateScalarListTable => NoAction.unit
      case x: DeleteScalarListTable => NoAction.unit
      case x: CreateModelTable      => CreateModelInterpreter.execute(x)
      case x: RenameTable           => RenameModelInterpreter.execute(x)
      case x: DeleteModelTable      => DeleteModelInterpreter.execute(x)
      case x: CreateRelationTable   => CreateRelationInterpreter.execute(x)
      case x: UpdateRelationTable   => NoAction.unit
      case x: DeleteRelationTable   => DeleteRelationInterpreter.execute(x)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter.execute(x)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter.execute(x)
      case x: UpdateInlineRelation  => NoAction.unit
    }
  }

  override def rollback(mutaction: DeployMutaction) = {
    mutaction match {
      case x: TruncateProject       => TruncateProjectInterpreter.rollback(x)
      case x: CreateColumn          => CreateColumnInterpreter.execute(x)
      case x: UpdateColumn          => UpdateColumnInterpreter.execute(x)
      case x: DeleteColumn          => DeleteColumnInterpreter.execute(x)
      case x: CreateScalarListTable => NoAction.unit
      case x: UpdateScalarListTable => NoAction.unit
      case x: DeleteScalarListTable => NoAction.unit
      case x: CreateModelTable      => CreateModelInterpreter.rollback(x)
      case x: RenameTable           => RenameModelInterpreter.rollback(x)
      case x: DeleteModelTable      => DeleteModelInterpreter.rollback(x)
      case x: CreateRelationTable   => CreateRelationInterpreter.execute(x)
      case x: UpdateRelationTable   => NoAction.unit
      case x: DeleteRelationTable   => DeleteRelationInterpreter.execute(x)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter.rollback(x)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter.rollback(x)
      case x: UpdateInlineRelation  => NoAction.unit
    }
  }
}
