package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.shared.errors.UserAPIErrors.DataItemDoesNotExist
import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.client.files.FileUploader
import cool.graph.shared.models.{Model, Project}
import scaldi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class S3UpdateFileName(model: Model, project: Project, fileId: String, newName: String, resolver: DataResolver)(implicit inj: Injector)
    extends Mutaction
    with Injectable
    with LazyLogging {

  var fileSecret: Option[String] = None

  override def execute: Future[MutactionExecutionResult] = {

    val uploader = new FileUploader(project)

    uploader.setFilename(project, fileSecret.get, newName)

    Future.successful(MutactionExecutionSuccess())
  }

  override def verify(): Future[Try[MutactionVerificationSuccess] with Product with Serializable] = {
    resolver.resolveByUnique(model, "id", fileId) map {
      case None => Failure(DataItemDoesNotExist(model.id, fileId))
      case node =>
        fileSecret = node.get.getOption[String]("secret")

        Success(MutactionVerificationSuccess())
    }
  }
}
