package cool.graph.system.database.tables

import cool.graph.shared.models.{IntegrationName, IntegrationType}
import slick.jdbc.MySQLProfile.api._

case class Integration(
    id: String,
    isEnabled: Boolean,
    integrationType: IntegrationType.Value,
    name: IntegrationName.Value,
    projectId: String
)

object IntegrationTable {
  implicit val integrationTypeMapper =
    MappedColumnType.base[IntegrationType.Value, String](
      e => e.toString,
      s => IntegrationType.withName(s)
    )

  implicit val integrationNameMapper =
    MappedColumnType.base[IntegrationName.Value, String](
      e => e.toString,
      s => IntegrationName.withName(s)
    )
}

class IntegrationTable(tag: Tag) extends Table[Integration](tag, "Integration") {
  import IntegrationTable._

  def id              = column[String]("id", O.PrimaryKey)
  def isEnabled       = column[Boolean]("isEnabled")
  def integrationType = column[IntegrationType.Value]("integrationType") // TODO adjust db naming
  def name            = column[IntegrationName.Value]("name")
  def projectId       = column[String]("projectId")

  def * = (id, isEnabled, integrationType, name, projectId) <> ((Integration.apply _).tupled, Integration.unapply)
}
