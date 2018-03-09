package com.prisma.deploy

case class InternalDatabaseDefs() {
  import slick.jdbc.MySQLProfile.api._

  lazy val internalDatabaseRoot = database(root = true)
  lazy val internalDatabase     = database(root = false)

  def database(root: Boolean) = {
    val sqlInternalHost     = sys.env("SQL_INTERNAL_HOST")
    val sqlInternalPort     = sys.env("SQL_INTERNAL_PORT")
    val sqlInternalDatabase = sys.env("SQL_INTERNAL_DATABASE")
    val sqlInternalUser     = sys.env("SQL_INTERNAL_USER")
    val sqlInternalPassword = sys.env("SQL_INTERNAL_PASSWORD")
    val schemaPart          = if (root) "" else "/" + sqlInternalDatabase
    Database.forURL(
      url = s"jdbc:mariadb://$sqlInternalHost:$sqlInternalPort$schemaPart?autoReconnect=true&useSSL=false&serverTimeZone=UTC&usePipelineAuth=false",
      user = sqlInternalUser,
      password = sqlInternalPassword,
      driver = "org.mariadb.jdbc.Driver"
    )
  }
}
