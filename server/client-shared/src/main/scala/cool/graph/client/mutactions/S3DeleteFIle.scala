package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph._
import cool.graph.client.ClientInjector
import cool.graph.client.files.FileUploader
import cool.graph.shared.models.{Model, Project}

import scala.concurrent.Future

case class S3DeleteFile(model: Model, project: Project, fileSecret: String)(implicit injector: ClientInjector) extends Mutaction with LazyLogging {

  override def execute: Future[MutactionExecutionResult] = {

    val uploader = new FileUploader(project)

    uploader.deleteFile(project, fileSecret)

    Future.successful(MutactionExecutionSuccess())
  }
}
