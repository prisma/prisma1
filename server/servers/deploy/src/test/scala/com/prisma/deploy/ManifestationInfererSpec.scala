package com.prisma.deploy

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.deploy.connector.postgresql.PostgresDeployConnector
import com.prisma.deploy.specutils.DeployTestDependencies
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Future

class ManifestationInfererSpec extends FlatSpec with Matchers with BeforeAndAfterAll with AwaitUtils {
  implicit lazy val system       = ActorSystem()
  implicit lazy val materializer = ActorMaterializer()
  lazy val testDependencies      = new DeployTestDependencies() // TODO: shutdown properly

  import system.dispatcher
  //
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    setupSchema().await()
  }

  def setupSchema(): Future[Unit] = {
    val connector = testDependencies.deployPersistencePlugin.asInstanceOf[PostgresDeployConnector]
    val chinookSql =
      scala.io.Source.fromFile("/Users/marcusboehm/R/github.com/graphcool/prisma/server/servers/deploy/src/test/resources/chinook_postgres.sql").mkString
    Future {
      val session   = connector.internalDatabase.createSession()
      val statement = session.createStatement()
      statement.execute(chinookSql)
      session.close()
    }
  }

  "lala" should "bla" in {
    true should be(true)
  }
}

//class ManifestationInfererSpec extends ManifestationInfererSpecBase {
//  override def setupSchema() = ???
//}
