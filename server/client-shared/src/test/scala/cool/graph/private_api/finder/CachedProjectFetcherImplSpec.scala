package cool.graph.private_api.finder

import cool.graph.akkautil.SingleThreadedActorSystem
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.client.finder.{CachedProjectFetcherImpl, RefreshableProjectFetcher}
import cool.graph.messagebus.Conversions
import cool.graph.messagebus.pubsub.Only
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.testkits.DummyPubSubSubscriber
import cool.graph.shared.models.{Project, ProjectDatabase, ProjectWithClientId, Region}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Awaitable, Future}

class CachedProjectFetcherImplSpec extends FlatSpec with Matchers with ScalaFutures {
  implicit val system                     = SingleThreadedActorSystem("cacheSpec")
  implicit val bugsnagger: BugSnaggerImpl = BugSnaggerImpl("")
  implicit val unmarshaller               = Conversions.Unmarshallers.ToString
  implicit val marshaller                 = Conversions.Marshallers.FromString

  val database = ProjectDatabase(id = "test", region = Region.EU_WEST_1, name = "client1", isDefaultForRegion = true)
  val project = Project(id = "", ownerId = "", name = s"Test Project", alias = None, projectDatabase = database)
  val rabbitUri                        = sys.env.getOrElse("RABBITMQ_URI", sys.error("RABBITMQ_URI env var required but not found"))
  val projectFetcher                   = new ProjectFetcherMock(project)
  val pubSub: RabbitAkkaPubSub[String] = RabbitAkkaPubSub[String](rabbitUri, "project-schema-invalidation", durable = true)

  "it" should "work" in {

    val dummyPubSub: DummyPubSubSubscriber[String] = DummyPubSubSubscriber.standalone[String]

    val projectFetcher = new RefreshableProjectFetcher {
      override def fetchRefreshed(projectIdOrAlias: String) = Future.successful(None)
      override def fetch(projectIdOrAlias: String)          = Future.successful(None)
    }

    val cachedProjectFetcher = CachedProjectFetcherImpl(
      projectFetcher = projectFetcher,
      projectSchemaInvalidationSubscriber = dummyPubSub
    )
    val result = await(cachedProjectFetcher.fetch("does-not-matter"))
  }

  "Changing the alias of a project" should "remove it from the alias cache" in {

    val cachedProjectFetcher = CachedProjectFetcherImpl(projectFetcher = projectFetcher, projectSchemaInvalidationSubscriber = pubSub)

    projectFetcher.setAlias(firstAlias = Some("FirstAlias"), secondAlias = None)
    //fetch first one with id and alias
    cachedProjectFetcher.fetch("FirstOne")

    //fetch second one with id and alias
    cachedProjectFetcher.fetch("SecondOne")

    //Flush first one from both caches by invalidating schema
    projectFetcher.setAlias(firstAlias = None, secondAlias = None)
    pubSub.publish(Only("FirstOne"), "FirstOne")

    Thread.sleep(2000)

    //fetch second time with alias -> this should not find anything now
    cachedProjectFetcher.fetch("FirstAlias").futureValue should be(None)
  }

  "Changing the alias of a project and reusing it on another project" should "return the new project upon fetch" in {

    val cachedProjectFetcher = CachedProjectFetcherImpl(projectFetcher = projectFetcher, projectSchemaInvalidationSubscriber = pubSub)

    projectFetcher.setAlias(firstAlias = Some("FirstAlias"), secondAlias = None)
    //fetch first one with id and alias
    cachedProjectFetcher.fetch("FirstOne")

    //fetch second one with id and alias
    cachedProjectFetcher.fetch("SecondOne")

    //Flush both from both caches by invalidating schema
    projectFetcher.setAlias(firstAlias = None, secondAlias = Some("FirstAlias"))
    pubSub.publish(Only("FirstOne"), "FirstOne")
    pubSub.publish(Only("SecondOne"), "SecondOne")

    Thread.sleep(1000)

    //fetch second time with alias -> this should not find anything now since project needs to be found once by id first
    val fetchByAlias = cachedProjectFetcher.fetch("FirstAlias").futureValue
    fetchByAlias should be(None)

    Thread.sleep(1000)
    //load alias cache by loading by id first once
    val fetchById = cachedProjectFetcher.fetch("SecondOne").futureValue
    fetchById.get.project.id should be("SecondOne")

    Thread.sleep(1000)
    // this should now find the SecondOne
    val fetchByAliasAgain = cachedProjectFetcher.fetch("FirstAlias").futureValue
    fetchByAliasAgain.get.project.id should be("SecondOne")
  }

  import scala.concurrent.duration._
  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, 5.seconds)

  class ProjectFetcherMock(project: Project) extends RefreshableProjectFetcher {
    var firstProject: Option[ProjectWithClientId]  = _
    var secondProject: Option[ProjectWithClientId] = _

    override def fetchRefreshed(projectIdOrAlias: String) = projectIdOrAlias match {
      case "FirstOne"  => Future.successful(firstProject)
      case "SecondOne" => Future.successful(secondProject)
      case _           => Future.successful(None)
    }

    override def fetch(projectIdOrAlias: String) = projectIdOrAlias match {
      case "FirstOne"  => Future.successful(firstProject)
      case "SecondOne" => Future.successful(secondProject)
      case _           => Future.successful(None)
    }

    def setAlias(firstAlias: Option[String], secondAlias: Option[String]) = {
      firstProject = Some(ProjectWithClientId(project.copy(id = "FirstOne", alias = firstAlias), clientId = ""))
      secondProject = Some(ProjectWithClientId(project.copy(id = "SecondOne", alias = secondAlias), clientId = ""))
    }
  }
}