package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph._
import cool.graph.client.files.FileUploader
import cool.graph.shared.models.{Model, Project}
import scaldi._

import scala.concurrent.Future

case class S3DeleteFIle(model: Model, project: Project, fileSecret: String)(implicit inj: Injector) extends Mutaction with Injectable with LazyLogging {

  override def execute: Future[MutactionExecutionResult] = {

    val uploader = new FileUploader(project)

    uploader.deleteFile(project, fileSecret)

    Future.successful(MutactionExecutionSuccess())
  }
}
