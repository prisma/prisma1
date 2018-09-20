package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mongo.database.NoAction

object MongoAnyMutactionInterpreter extends MongoMutactionInterpreter[DeployMutaction] {
  override def execute(mutaction: DeployMutaction) = {
    mutaction match {
      case x: CreateProject         => CreateProjectInterpreter.execute(x)
      case x: TruncateProject       => TruncateProjectInterpreter.execute(x)
      case x: DeleteProject         => DeleteProjectInterpreter.execute(x)
      case x: CreateColumn          => NoAction.unit
      case x: UpdateColumn          => NoAction.unit
      case x: DeleteColumn          => NoAction.unit
      case x: CreateScalarListTable => NoAction.unit
      case x: UpdateScalarListTable => NoAction.unit
      case x: DeleteScalarListTable => NoAction.unit
      case x: CreateModelTable      => CreateModelInterpreter.execute(x)
      case x: RenameTable           => RenameModelInterpreter.execute(x)
      case x: DeleteModelTable      => DeleteModelInterpreter.execute(x)
      case x: CreateRelationTable   => CreateRelationInterpreter.execute(x)
      case x: DeleteRelationTable   => DeleteRelationInterpreter.execute(x)
      case x: CreateInlineRelation  => sys.error("Not supported on mongo")
    }
  }

  override def rollback(mutaction: DeployMutaction) = {
    mutaction match {
      case x: CreateProject         => CreateProjectInterpreter.rollback(x)
      case x: TruncateProject       => TruncateProjectInterpreter.rollback(x)
      case x: DeleteProject         => DeleteProjectInterpreter.rollback(x)
      case x: CreateColumn          => NoAction.unit
      case x: UpdateColumn          => NoAction.unit
      case x: DeleteColumn          => NoAction.unit
      case x: CreateScalarListTable => NoAction.unit
      case x: UpdateScalarListTable => NoAction.unit
      case x: DeleteScalarListTable => NoAction.unit
      case x: CreateModelTable      => CreateModelInterpreter.rollback(x)
      case x: RenameTable           => RenameModelInterpreter.rollback(x)
      case x: DeleteModelTable      => DeleteModelInterpreter.rollback(x)
      case x: CreateRelationTable   => CreateRelationInterpreter.rollback(x)
      case x: DeleteRelationTable   => DeleteRelationInterpreter.rollback(x)
      case x: CreateInlineRelation  => sys.error("Not supported on mongo")
    }
  }
}
