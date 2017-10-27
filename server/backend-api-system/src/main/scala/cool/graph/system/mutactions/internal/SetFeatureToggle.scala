package cool.graph.system.mutactions.internal

import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.FeatureToggleTable
import cool.graph.shared.models.{FeatureToggle, Project}
import cool.graph.{MutactionVerificationSuccess, SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

case class SetFeatureToggle(project: Project, featureToggle: FeatureToggle) extends SystemSqlMutaction {
  val featureToggles: TableQuery[FeatureToggleTable] = TableQuery[FeatureToggleTable]

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val insertOrUpdate = featureToggles
      .filter(ft => ft.projectId === project.id && ft.name === featureToggle.name)
      .result
      .headOption
      .flatMap {
        case Some(featureToggleRow) =>
          featureToggles.update(
            featureToggleRow.copy(
              isEnabled = featureToggle.isEnabled
            )
          )
        case None =>
          featureToggles += cool.graph.system.database.tables.FeatureToggle(
            id = featureToggle.id,
            projectId = project.id,
            name = featureToggle.name,
            isEnabled = featureToggle.isEnabled
          )
      }
      .transactionally

    Future.successful(
      SystemSqlStatementResult(
        DBIO.seq(insertOrUpdate)
      )
    )
  }

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    // FIXME: just able to set toggles in projects one has access to?
    Future.successful(Success(MutactionVerificationSuccess()))
  }
}
