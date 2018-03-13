package com.prisma.api.connector.mysql.impl

import com.prisma.api.connector.AddDataItemToManyRelationByPath
import com.prisma.api.connector.mysql.DatabaseMutactionInterpreter
import com.prisma.api.database.DatabaseMutationBuilder

object AddDataItemToManyRelationByPathInterpreter extends DatabaseMutactionInterpreter[AddDataItemToManyRelationByPath] {
  override def action(mutaction: AddDataItemToManyRelationByPath) = {
    DatabaseMutationBuilder.createRelationRowByPath(mutaction.project.id, mutaction.path)
  }
}
