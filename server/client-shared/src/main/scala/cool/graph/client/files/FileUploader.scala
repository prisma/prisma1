package cool.graph.client.files

import java.io.ByteArrayInputStream
import java.net.URLEncoder

import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.services.s3.{AmazonS3}
import com.amazonaws.services.s3.internal.Mimetypes
import com.amazonaws.services.s3.model._
import com.amazonaws.util.IOUtils
import cool.graph.cuid.Cuid
import cool.graph.shared.models.Project
import scaldi.{Injectable, Injector}

import scala.concurrent.duration._

case class FileUploadResponse(size: Long, fileSecret: String, fileName: String, contentType: String)

class FileUploader(project: Project)(implicit inj: Injector) extends Injectable {

  val s3 = inject[AmazonS3]("s3-fileupload")
  implicit val materializer =
    inject[ActorMaterializer](identified by "actorMaterializer")

  val bucketName = sys.env.getOrElse("FILEUPLOAD_S3_BUCKET", "dev.files.graph.cool")

  def uploadFile(metadata: FileInfo, byteSource: Source[ByteString, Any]): FileUploadResponse = {
    val fileSecret = Cuid.createCuid()
    val key        = s"${project.id}/${fileSecret}"

    val stream = byteSource.runWith(
      StreamConverters.asInputStream(600.seconds)
    )
    val byteArray = IOUtils.toByteArray(stream)

    val meta = getObjectMetaData(metadata.fileName)
    meta.setContentLength(byteArray.length.toLong)

    val request = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(byteArray), meta)
    request.setCannedAcl(CannedAccessControlList.PublicRead)

    s3.putObject(request)

    val contentType = Mimetypes.getInstance.getMimetype(metadata.fileName)

    FileUploadResponse(size = byteArray.length.toLong, fileSecret = fileSecret, fileName = metadata.fileName, contentType = contentType)
  }

  def getObjectMetaData(fileName: String): ObjectMetadata = {
    val contentType     = Mimetypes.getInstance.getMimetype(fileName)
    val meta            = new ObjectMetadata()
    val encodedFilename = URLEncoder.encode(fileName, "UTF-8")

    // note: we can probably do better than urlencoding the filename
    // see RFC 6266: https://tools.ietf.org/html/rfc6266#section-4.3
    meta.setHeader("content-disposition", s"""filename="${encodedFilename}"; filename*="UTF-8''${encodedFilename}"""")
    meta.setContentType(contentType)

    meta
  }

  def setFilename(project: Project, fileSecret: String, newName: String): CopyObjectResult = {
    val key = s"${project.id}/${fileSecret}"

    val request = new CopyObjectRequest(bucketName, key, bucketName, key)
    request.setNewObjectMetadata(getObjectMetaData(newName))
    request.setCannedAccessControlList(CannedAccessControlList.PublicRead)

    s3.copyObject(request)
  }

  def deleteFile(project: Project, fileSecret: String): Unit = {
    val key = s"${project.id}/${fileSecret}"

    val request = new DeleteObjectRequest(bucketName, key)

    s3.deleteObject(request)
  }

}
