package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.persistence.CloudSecretPersistence
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL._

import scala.concurrent.{ExecutionContext, Future}

object CloudSecretTable {
  val cloudSecretTableName = "CloudSecret"
  val t                    = table(name(cloudSecretTableName))
  val secret               = field(name(cloudSecretTableName, "secret"))
}

case class JdbcCloudSecretPersistence(slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcPersistenceBase with CloudSecretPersistence {
  val sql      = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))
  val database = slickDatabase.database

  override def load(): Future[Option[String]] = {
    val query = sql
      .select(CloudSecretTable.secret)
      .from(CloudSecretTable.t)
      .limit(DSL.inline(1))

    database
      .run(
        queryToDBIO(query)(
          readResult = { rs =>
            if (rs.next()) {
              Some(rs.getString(CloudSecretTable.secret.getName))
            } else {
              None
            }
          }
        ))
  }

  override def update(secret: Option[String]): Future[_] = {
    secret match {
      case None    => deleteSecret()
      case Some(x) => upsertSecret(x)
    }
  }

  def deleteSecret(): Future[_] = {
    val query = sql.delete(CloudSecretTable.t)
    database.run(deleteToDBIO(query)())
  }

  def upsertSecret(secret: String): Future[_] = {
    val update = sql
      .update(CloudSecretTable.t)
      .set(CloudSecretTable.secret, secret)

    lazy val create = sql
      .insertInto(CloudSecretTable.t)
      .columns(CloudSecretTable.secret)
      .values(secret)

    database
      .run(updateToDBIO(update)(setParams = { pp =>
        pp.setString(secret)
      }))
      .flatMap { updatedCount: Int =>
        if (updatedCount == 0) {
          database.run(
            insertToDBIO(create)(
              setParams = pp => pp.setString(secret)
            ))
        } else {
          Future.unit
        }
      }
  }
}
