package com.prisma.api.connector.jdbc.database

import slick.jdbc.JdbcProfile

case class Databases(
    primary: SlickDatabase,
    replica: SlickDatabase
)

case class SlickDatabase(
    profile: JdbcProfile,
    database: JdbcProfile#Backend#Database
)
