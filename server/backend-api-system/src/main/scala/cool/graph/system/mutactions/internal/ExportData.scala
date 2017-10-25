package cool.graph.system.mutactions.internal

import java.io.{BufferedWriter, ByteArrayInputStream, ByteArrayOutputStream, OutputStreamWriter}
import java.nio.charset.Charset
import java.util.zip.{ZipEntry, ZipOutputStream}
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{CannedAccessControlList, ObjectMetadata, PutObjectRequest}
import cool.graph.JsonFormats._
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.cuid.Cuid
import cool.graph.shared.errors.UserInputErrors.TooManyNodesToExportData
import cool.graph.shared.models._
import scaldi.{Injectable, Injector}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

case class ExportData(project: Project, resolver: DataResolver)(implicit inj: Injector) extends Mutaction with Injectable {

  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")
  val MODEL_SIZE_LIMIT                         = 10000
  val key                                      = s"${Cuid.createCuid()}.zip"
  def getUrl                                   = s"https://s3-eu-west-1.amazonaws.com/${sys.env.getOrElse("DATA_EXPORT_S3_BUCKET", "")}/$key"

  def generateJsonForModel(model: Model): Future[String] = {
    val relationIds = model.fields.filter(_.isRelation).map(_.relation.get.id)

    Future
      .sequence(relationIds.map(relationId => {
        resolver
          .resolveByModel(Model(id = relationId, name = relationId, isSystem = false))
          .map(x => (relationId, x.items.map(_.userData)))
      }))
      .flatMap((relations: Seq[(Id, Seq[Map[String, Option[Any]]])]) => {
        resolver
          .resolveByModel(model)
          .map(resolverResult => {
            resolverResult.items.map(dataItem => {
              val relationValues = model.fields
                .filter(_.isRelation)
                .map(relationField => {
                  relationField.name -> relations
                    .find(_._1 == relationField.relation.get.id)
                    .get
                    ._2
                    .flatMap(x => {
                      x match {
                        case y if y("A").contains(dataItem.id) => y("B")
                        case y if y("B").contains(dataItem.id) => y("A")
                        case _                                 => None
                      }
                    })
                })

              val scalarValues: Map[String, Any] =
                dataItem.userData.mapValues(_.orNull)
              scalarValues + ("id" -> dataItem.id) ++ relationValues
            })
          })
          .map((data: Seq[Map[String, Any]]) => {
            data.toJson(writer = new SeqAnyJsonWriter()).prettyPrint
          })
      })
  }

  def zipInMemory(modelNameAndJson: List[(String, String)]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(out)

    val writer = new BufferedWriter(
      new OutputStreamWriter(zip, Charset.forName("utf-8"))
    )

    modelNameAndJson.foreach(nameAndJson => {
      zip.putNextEntry(new ZipEntry(nameAndJson._1))

      writer.write(nameAndJson._2.toCharArray)
      writer.flush()

      zip.closeEntry()
    })

    writer.close()
    zip.close()
    out.toByteArray
  }

  def uploadBytes(bytes: Array[Byte]) = {
    val s3: AmazonS3         = inject[AmazonS3]("export-data-s3")
    val bucketName: String   = sys.env.getOrElse("DATA_EXPORT_S3_BUCKET", "")
    val meta: ObjectMetadata = getObjectMetaData(key)

    meta.setContentLength(bytes.length.toLong)

    val request = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(bytes), meta)
    request.setCannedAcl(CannedAccessControlList.PublicRead)

    s3.putObject(request)
  }

  def getObjectMetaData(fileName: String): ObjectMetadata = {
    val contentType = "application/octet-stream"
    val meta        = new ObjectMetadata()

    meta.setHeader("content-disposition", s"""filename="$fileName"""")
    meta.setContentType(contentType)
    meta
  }

  override def execute: Future[MutactionExecutionSuccess] = {
    Future
      .sequence(project.models.map(model => generateJsonForModel(model).map(json => (model, json))))
      .map(x => {
        val modelNameAndJsonList = x.map(y => (s"${y._1.name}.json", y._2))
        val zipBytes             = zipInMemory(modelNameAndJsonList)

        uploadBytes(zipBytes)
        MutactionExecutionSuccess()
      })
  }

  override def verify: Future[Try[MutactionVerificationSuccess]] = {

    def verifyResultSizeLimitIsNotExceeded(modelCounts: List[Int]) = {
      modelCounts.map { modelCount =>
        if (modelCount > MODEL_SIZE_LIMIT) throw TooManyNodesToExportData(MODEL_SIZE_LIMIT)
      }
    }

    Future.sequence(project.models.map(model => { resolver.itemCountForModel(model) })).map(verifyResultSizeLimitIsNotExceeded)

    Future.successful(Success(MutactionVerificationSuccess()))
  }
}
