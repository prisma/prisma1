package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.TopLevelDatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.gc_values.IdGCValue
import slick.dbio._

import scala.concurrent.ExecutionContext

object ParameterLimit {
  //Postgres has a limit of 32767 parameters but when updating scalar lists we set three parameters per item in the group
  val groupSize = 10000
}

case class ResetDataInterpreter(mutaction: ResetData) extends TopLevelDatabaseMutactionInterpreter {
  def dbioAction(mutationBuilder: JdbcActionsBuilder) = {
    mutationBuilder.truncateTables(mutaction.project).andThen(unitResult)
  }
}
