package util

case class ConnectorConfig(
    provider: String,
    url: String
) {
  def capabilities = {
    provider match {
      case "sqlite"     => ConnectorCapabilities.sqliteNative
      case "postgresql" => ConnectorCapabilities.postgresPrototype
      case "mysql"      => ConnectorCapabilities.mysqlPrototype
    }

  }
}

object ConnectorConfig {
  lazy val instance: ConnectorConfig = {
    val filePath        = EnvVars.serverRoot + "/prisma-rs/connector_to_test"
    val connectorToTest = scala.io.Source.fromFile(filePath).mkString.lines.next().trim

    connectorToTest match {
      case "sqlite"                  => ConnectorConfig("sqlite", "file://$DB_FILE")
      case "postgres" | "postgresql" => ConnectorConfig("postgresql", s"postgresql://postgres:prisma@$postgresHost:5432/db?schema=$$DB")
      case "mysql"                   => ConnectorConfig("mysql", s"mysql://root:prisma@$mysqlHost:3306/$$DB")
      case x                         => sys.error(s"Connector $x is not supported yet.")
    }

  }

  lazy val postgresHost = {
    if (EnvVars.isBuildkite) {
      "test-db-postgres"
    } else {
      "127.0.0.1"
    }
  }

  lazy val mysqlHost = {
    if (EnvVars.isBuildkite) {
      "test-db-mysql"
    } else {
      "127.0.0.1"
    }
  }
}
