package cool.graph.system.mutations

import cool.graph.shared.models.{ProjectDatabase, Region}
import cool.graph.system.database.finder.ProjectDatabaseFinder
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object DefaultProjectDatabase {
  def blocking(internalDatabase: DatabaseDef): ProjectDatabase = {
    Await.result(this(internalDatabase), 5.seconds)
  }

  private def apply(internalDatabase: DatabaseDef): Future[ProjectDatabase] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    lazy val fallbackForTests: Future[ProjectDatabase] = {
      val region = Region.EU_WEST_1
      ProjectDatabaseFinder
        .defaultForRegion(region)(internalDatabase)
        .map(_.getOrElse(sys.error(s"no default db found for region $region")))
    }

    fallbackForTests
  }
}
