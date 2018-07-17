package com.prisma.api.connector.jdbc.database

// format: off
trait AllActions
  extends NodeActions
    with RelationActions
    with ScalarListActions
    with ValidationActions
    with RelayIdActions
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
    schemaName: String,
    slickDatabase: SlickDatabase
) extends AllActions
    with AllQueries
