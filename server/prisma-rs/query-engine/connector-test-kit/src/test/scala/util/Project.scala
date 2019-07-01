package util

case class Project(
    id: String,
    dataModel: String,
) {
  val dataSourceConfig: String = {
    val config = ConnectorConfig.load
    s"""
           |datasource test {
           |  provider = "${config.provider}"
           |  url = "${config.url}"
           |}
    """.stripMargin.replaceAllLiterally("$DB_FILE", s"${EnvVars.serverRoot}/db/$id.db")
  }

  val dataModelWithDataSourceConfig = {
    dataSourceConfig + "\n" + dataModel
  }
}
