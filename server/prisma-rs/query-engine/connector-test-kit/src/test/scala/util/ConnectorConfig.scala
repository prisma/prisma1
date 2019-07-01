package util

case class ConnectorConfig(
    provider: String,
    url: String
)

object ConnectorConfig {
  def load: ConnectorConfig = {
    ConnectorConfig("sqlite", "file://$DB_FILE")
  }
}
