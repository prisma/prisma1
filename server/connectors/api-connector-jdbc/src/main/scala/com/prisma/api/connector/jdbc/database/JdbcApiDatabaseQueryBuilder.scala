package com.prisma.api.connector.jdbc.database

import scala.concurrent.ExecutionContext

case class JdbcApiDatabaseQueryBuilder(
    schemaName: String,
    slickDatabase: SlickDatabase
)(implicit ec: ExecutionContext)
    extends AllBuilders
    with AllQueries
