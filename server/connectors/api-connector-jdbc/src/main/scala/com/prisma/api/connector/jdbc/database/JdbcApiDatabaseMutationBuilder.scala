package com.prisma.api.connector.jdbc.database

trait AllActions extends NodeActions with RelationActions with ScalarListActions with ValidationActions with RelayIdActions with ImportActions with MiscActions
trait AllQueries extends NodeQueries

case class JdbcApiDatabaseMutationBuilder(
    schemaName: String,
    slickDatabase: SlickDatabase
) extends AllActions
    with AllQueries
