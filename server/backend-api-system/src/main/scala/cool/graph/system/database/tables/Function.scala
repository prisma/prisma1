package cool.graph.system.database.tables

import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.shared.models.{FunctionBinding, FunctionType, RequestPipelineOperation}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

case class Function(
    id: String,
    projectId: String,
    name: String,
    binding: FunctionBinding,
    functionType: FunctionType,
    isActive: Boolean,
    requestPipelineMutationModelId: Option[String],
    requestPipelineMutationOperation: Option[RequestPipelineOperation],
    serversideSubscriptionQuery: Option[String],
    serversideSubscriptionQueryFilePath: Option[String],
    lambdaArn: Option[String],
    webhookUrl: Option[String],
    webhookHeaders: Option[String],
    inlineCode: Option[String],
    inlineCodeFilePath: Option[String],
    auth0Id: Option[String],
    schema: Option[String],
    schemaFilePath: Option[String]
)

class FunctionTable(tag: Tag) extends Table[Function](tag, "Function") {

  implicit val FunctionBindingMapper                  = FunctionTable.FunctionBindingMapper
  implicit val FunctionTypeMapper                     = FunctionTable.FunctionTypeMapper
  implicit val RequestPipelineMutationOperationMapper = FunctionTable.RequestPipelineMutationOperationMapper

  def id: Rep[String]                                                         = column[String]("id", O.PrimaryKey)
  def projectId: Rep[String]                                                  = column[String]("projectId")
  def name: Rep[String]                                                       = column[String]("name")
  def binding: Rep[FunctionBinding]                                           = column[FunctionBinding]("binding")
  def functionType: Rep[FunctionType]                                         = column[FunctionType]("type")
  def isActive: Rep[Boolean]                                                  = column[Boolean]("isActive")
  def requestPipelineMutationModelId: Rep[Option[String]]                     = column[Option[String]]("requestPipelineMutationModelId")
  def requestPipelineMutationOperation: Rep[Option[RequestPipelineOperation]] = column[Option[RequestPipelineOperation]]("requestPipelineMutationOperation")
  def serversideSubscriptionQuery: Rep[Option[String]]                        = column[Option[String]]("serversideSubscriptionQuery")
  def serversideSubscriptionQueryFilePath: Rep[Option[String]]                = column[Option[String]]("serversideSubscriptionQueryFilePath")
  def lambdaArn: Rep[Option[String]]                                          = column[Option[String]]("lambdaArn")
  def webhookUrl: Rep[Option[String]]                                         = column[Option[String]]("webhookUrl")
  def webhookHeaders: Rep[Option[String]]                                     = column[Option[String]]("webhookHeaders")
  def inlineCode: Rep[Option[String]]                                         = column[Option[String]]("inlineCode")
  def inlineCodeFilePath: Rep[Option[String]]                                 = column[Option[String]]("inlineCodeFilePath")
  def auth0Id: Rep[Option[String]]                                            = column[Option[String]]("auth0Id")
  def schema: Rep[Option[String]]                                             = column[Option[String]]("schema")
  def schemaFilePath: Rep[Option[String]]                                     = column[Option[String]]("schemaFilePath")

  def * : ProvenShape[Function] =
    (id,
     projectId,
     name,
     binding,
     functionType,
     isActive,
     requestPipelineMutationModelId,
     requestPipelineMutationOperation,
     serversideSubscriptionQuery,
     serversideSubscriptionQueryFilePath,
     lambdaArn,
     webhookUrl,
     webhookHeaders,
     inlineCode,
     inlineCodeFilePath,
     auth0Id,
     schema,
     schemaFilePath) <> ((Function.apply _).tupled, Function.unapply)
}

object FunctionTable {
  implicit val FunctionBindingMapper: JdbcType[FunctionBinding] with BaseTypedType[FunctionBinding] =
    MappedColumnType.base[FunctionBinding, String](
      e => e.toString,
      s => FunctionBinding.withName(s)
    )

  implicit val FunctionTypeMapper: JdbcType[FunctionType] with BaseTypedType[FunctionType] =
    MappedColumnType.base[FunctionType, String](
      e => e.toString,
      s => FunctionType.withName(s)
    )

  implicit val RequestPipelineMutationOperationMapper: JdbcType[RequestPipelineOperation] with BaseTypedType[RequestPipelineOperation] =
    MappedColumnType.base[RequestPipelineOperation, String](
      e => e.toString,
      s => RequestPipelineOperation.withName(s)
    )
}
