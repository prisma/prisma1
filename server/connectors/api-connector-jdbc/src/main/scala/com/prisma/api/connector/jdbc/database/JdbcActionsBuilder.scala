package com.prisma.api.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.shared.models.Project

// format: off
trait AllActions
  extends NodeActions
    with RelationActions
    with ScalarListActions
    with ValidationActions
    with ImportActions
    with MiscActions

trait AllQueries
  extends NodeSingleQueries
    with NodeManyQueries
    with RelationQueries
    with ScalarListQueries
    with MiscQueries
// format: on

case class JdbcActionsBuilder(
    project: Project,
    slickDatabase: SlickDatabase
) extends AllActions
    with AllQueries
