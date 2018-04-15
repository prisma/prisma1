package com.prisma.deploy.connector.mysql

case class InternalDatabaseDefs() {
  import slick.jdbc.PostgresProfile.api._

  lazy val internalDatabaseRoot = database(root = true)
  lazy val internalDatabase     = database(root = false)

  def database(root: Boolean) = {
    val postgresInternalHost     = sys.env("POSTGRES_INTERNAL_HOST")
    val postgresInternalPort     = sys.env("POSTGRES_INTERNAL_PORT")
    val postgresInternalDatabase = sys.env("POSTGRES_INTERNAL_DATABASE")
    val postgresInternalUser     = sys.env("POSTGRES_INTERNAL_USER")
    val postgresInternalPassword = sys.env("POSTGRES_INTERNAL_PASSWORD")
    val schemaPart               = if (root) "" else "/" + postgresInternalDatabase
    Database.forURL(
      url = s"jdbc:postgresql://$postgresInternalHost:$postgresInternalPort$schemaPart?autoReconnect=true&useSSL=false&serverTimeZone=UTC&usePipelineAuth=false",
      user = postgresInternalUser,
      password = postgresInternalPassword,
      driver = "org.postgresql.Driver"
    )
  }
}
