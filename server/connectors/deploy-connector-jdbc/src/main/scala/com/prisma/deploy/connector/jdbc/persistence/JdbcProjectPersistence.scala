package com.prisma.deploy.connector.jdbc.persistence

import java.sql.ResultSet

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.MissingBackRelations
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.deploy.connector.persistence.ProjectPersistence
import com.prisma.shared.models
import com.prisma.shared.models.{MigrationStatus, Project, ProjectManifestation, Schema}
import org.jooq.impl.DSL
import org.jooq.impl.DSL._
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ProjectTable {
  val projectTableName = "Project"
  val t                = table(name(projectTableName))
  val id               = field(name(projectTableName, "id"))
  val secrets          = field(name(projectTableName, "secrets"))
  val allowQueries     = field(name(projectTableName, "allowQueries"))
  val allowMutations   = field(name(projectTableName, "allowMutations"))
  val functions        = field(name(projectTableName, "functions"))
}

case class JdbcProjectPersistence(slickDatabase: SlickDatabase, dbConfig: DatabaseConfig) extends JdbcBase with ProjectPersistence {
  import com.prisma.shared.models.ProjectJsonFormatter._

  val projectManifestation: ProjectManifestation = ProjectManifestation(dbConfig.database, dbConfig.schema, dbConfig.connector)
  val pt                                         = ProjectTable
  val mt                                         = MigrationTable

  override def load(id: String): Future[Option[Project]] = {
    val query = sql
      .select(pt.id, pt.secrets, pt.allowQueries, pt.allowMutations, mt.schema, mt.functions, mt.revision)
      .from(pt.t)
      .join(mt.t)
      .on(mt.projectId.equal(pt.id))
      .and(pt.id.equal(placeHolder))
      .and(mt.status.equal(placeHolder))
      .orderBy(mt.revision.desc())
      .limit(DSL.inline(1))

    database.run(
      queryToDBIO(query)(
        setParams = { pp =>
          pp.setString(id)
          pp.setString(MigrationStatus.Success.toString)
        },
        readResult = { rs =>
          if (rs.next()) {
            Some(projectFromResultSet(rs))
          } else {
            None
          }
        }
      ))
  }

  override def loadAll(): Future[Seq[Project]] = {
    val revisionQuery = sql
      .selectDistinct(field(name("outer", "projectId")))
      .select(
        sql
          .select(DSL.max(mt.revision))
          .from(mt.t)
          .where(field(name("outer", "projectId")).eq(mt.projectId))
          .and(mt.status.equal(placeHolder))
          .asField("revision")
      )
      .from(mt.t.as("outer"))

    val query = sql
      .select(pt.id, pt.secrets, pt.allowQueries, pt.allowMutations, mt.schema, mt.functions, mt.revision)
      .from(pt.t)
      .join(revisionQuery.asTable("jt"))
      .on(field(name("jt", "projectId")).equal(pt.id))
      .join(mt.t)
      .on(pt.id.eq(mt.projectId))
      .and(mt.revision.eq(field(name("jt", "revision"))))

    database.run(
      queryToDBIO(query)(
        setParams = { pp =>
          pp.setString(MigrationStatus.Success.toString)
        },
        readResult = { rs =>
          val buffer = ArrayBuffer.empty[Project]
          while (rs.next()) {
            buffer += projectFromResultSet(rs)
          }

          buffer
        }
      ))
  }

  override def create(project: Project): Future[Unit] = {
    val secretsJson   = Json.toJson(project.secrets)
    val functionsJson = Json.toJson(project.functions)

    val query = sql
      .insertInto(pt.t)
      .columns(pt.id, pt.secrets, pt.allowQueries, pt.allowMutations, pt.functions)
      .values(placeHolder, placeHolder, placeHolder, placeHolder, placeHolder)

    database.run(insertToDBIO(query)(setParams = pp => {
      pp.setString(project.id)
      pp.setString(secretsJson.toString)
      pp.setBoolean(project.allowQueries)
      pp.setBoolean(project.allowMutations)
      pp.setString(functionsJson.toString)
    }))
  }

  override def update(project: Project): Future[_] = {
    val secrets   = Json.toJson(project.secrets)
    val functions = Json.toJson(project.functions)
    val query = sql
      .update(pt.t)
      .set(pt.secrets, placeHolder)
      .set(pt.functions, placeHolder)
      .set(pt.allowQueries, placeHolder)
      .set(pt.allowMutations, placeHolder)
      .where(pt.id.equal(placeHolder))

    database.run(updateToDBIO(query)(setParams = { pp =>
      pp.setString(secrets.toString)
      pp.setString(functions.toString)
      pp.setBoolean(project.allowQueries)
      pp.setBoolean(project.allowMutations)
      pp.setString(project.id)
    }))
  }

  override def delete(project: String): Future[Unit] = {
    val query = sql
      .delete(pt.t)
      .where(pt.id.equal(placeHolder))

    database.run(deleteToDBIO(query)(setParams = { pp =>
      pp.setString(project)
    }))
  }

  private def convertSchema(schema: JsValue): Schema = {
    val schemaWithMissingBackRelations = schema.as[Schema]
    MissingBackRelations.add(schemaWithMissingBackRelations)
  }

  private def projectFromResultSet(rs: ResultSet): Project = {
    val schema    = convertSchema(Json.parse(rs.getString(mt.schema.getName)))
    val secrets   = Json.parse(rs.getString(pt.secrets.getName)).as[Vector[String]]
    val functions = Json.parse(rs.getString(mt.functions.getName)).as[List[models.Function]]

    Project(
      id = rs.getString(pt.id.getName),
      revision = rs.getInt(mt.revision.getName),
      schema = schema,
      secrets = secrets,
      allowQueries = rs.getBoolean(pt.allowQueries.getName),
      allowMutations = rs.getBoolean(pt.allowMutations.getName),
      functions = functions,
      manifestation = projectManifestation
    )
  }
}
