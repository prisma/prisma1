package com.prisma.deploy.connector.mysql.impls.mutactions

import com.prisma.deploy.connector._

object MySqlAnyMutactionInterpreter extends SqlMutactionInterpreter[DeployMutaction] {
  override def execute(mutaction: DeployMutaction) = {
    mutaction match {
      case x: CreateProject         => CreateProjectInterpreter.execute(x)
      case x: TruncateProject       => TruncateProjectInterpreter.execute(x)
      case x: DeleteProject         => DeleteProjectInterpreter.execute(x)
      case x: CreateColumn          => CreateColumnInterpreter.execute(x)
      case x: UpdateColumn          => UpdateColumnInterpreter.execute(x)
      case x: DeleteColumn          => DeleteColumnInterpreter.execute(x)
      case x: CreateScalarListTable => CreateScalarListInterpreter.execute(x)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter.execute(x)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter.execute(x)
      case x: CreateModelTable      => CreateModelInterpreter.execute(x)
      case x: RenameTable           => RenameModelInterpreter.execute(x)
      case x: DeleteModelTable      => DeleteModelInterpreter.execute(x)
      case x: CreateRelationTable   => CreateRelationInterpreter.execute(x)
      case x: DeleteRelationTable   => DeleteRelationInterpreter.execute(x)
    }
  }

  override def rollback(mutaction: DeployMutaction) = {
    mutaction match {
      case x: CreateProject         => CreateProjectInterpreter.rollback(x)
      case x: TruncateProject       => TruncateProjectInterpreter.rollback(x)
      case x: DeleteProject         => DeleteProjectInterpreter.rollback(x)
      case x: CreateColumn          => CreateColumnInterpreter.rollback(x)
      case x: UpdateColumn          => UpdateColumnInterpreter.rollback(x)
      case x: DeleteColumn          => DeleteColumnInterpreter.rollback(x)
      case x: CreateScalarListTable => CreateScalarListInterpreter.rollback(x)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter.rollback(x)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter.rollback(x)
      case x: CreateModelTable      => CreateModelInterpreter.rollback(x)
      case x: RenameTable           => RenameModelInterpreter.rollback(x)
      case x: DeleteModelTable      => DeleteModelInterpreter.rollback(x)
      case x: CreateRelationTable   => CreateRelationInterpreter.rollback(x)
      case x: DeleteRelationTable   => DeleteRelationInterpreter.rollback(x)
    }
  }
}
