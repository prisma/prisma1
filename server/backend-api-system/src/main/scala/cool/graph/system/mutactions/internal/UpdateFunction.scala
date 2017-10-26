package cool.graph.system.mutactions.internal

import akka.http.scaladsl.model.Uri
import cool.graph.shared.errors.UserInputErrors._
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors.{FunctionHasInvalidUrl, FunctionWithNameAlreadyExists, IllegalFunctionName, SchemaExtensionParseError}
import cool.graph.shared.models.{CustomMutationFunction, CustomQueryFunction, Function, FunctionDelivery, HttpFunction, Project}
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.FunctionTable
import cool.graph.{MutactionVerificationSuccess, SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateFunction(project: Project, newFunction: Function, oldFunction: Function) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {

    implicit val FunctionBindingMapper                  = FunctionTable.FunctionBindingMapper
    implicit val FunctionTypeMapper                     = FunctionTable.FunctionTypeMapper
    implicit val RequestPipelineMutationOperationMapper = FunctionTable.RequestPipelineMutationOperationMapper

    val functions = TableQuery[FunctionTable]

    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          functions.filter(_.id === newFunction.id).update(ModelToDbMapper.convertFunction(project, newFunction))
        )
      }
    }
  }

  override def verify(): Future[Try[MutactionVerificationSuccess]] = FunctionVerification.verifyFunction(newFunction, project)

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] =
    Some(UpdateFunction(project = project, newFunction = oldFunction, oldFunction = newFunction).execute)

}

object FunctionVerification {

  def verifyFunction(function: Function, project: Project): Future[Try[MutactionVerificationSuccess] with Product with Serializable] = {

    def differentFunctionWithSameTypeName(name: String, id: String): Boolean = {
      project.customMutationFunctions.exists(func => func.payloadType.name == name && func.id != id) ||
      project.customQueryFunctions.exists(func => func.payloadType.name == name && func.id != id)

    }

    def differentFunctionWithSameName: Boolean = {
      project.functions.exists(func => func.name.toLowerCase == function.name.toLowerCase && func.id != function.id)
    }

    val typeNameViolation = function match {
      case f: CustomMutationFunction if project.models.map(_.name).contains(f.payloadType.name)     => List(f.payloadType.name)
      case f: CustomQueryFunction if project.models.map(_.name).contains(f.payloadType.name)        => List(f.payloadType.name)
      case f: CustomMutationFunction if differentFunctionWithSameTypeName(f.payloadType.name, f.id) => List(f.payloadType.name)
      case f: CustomQueryFunction if differentFunctionWithSameTypeName(f.payloadType.name, f.id)    => List(f.payloadType.name)
      case _                                                                                        => List.empty
    }

    def hasInvalidUrl = function.delivery match {
      case x: HttpFunction => Try(Uri(x.url)).isFailure
      case _               => false
    }

    def getInvalidUrl(delivery: FunctionDelivery) = delivery.asInstanceOf[HttpFunction].url

    def projectHasNameConflict = function match {
      case x: CustomQueryFunction    => project.hasSchemaNameConflict(x.queryName, function.id)
      case x: CustomMutationFunction => project.hasSchemaNameConflict(x.mutationName, function.id)
      case _                         => false
    }

    Future.successful(() match {
      case _ if !NameConstraints.isValidFunctionName(function.name) => Failure(IllegalFunctionName(function.name))
      case _ if typeNameViolation.nonEmpty                          => Failure(FunctionHasInvalidPayloadName(name = function.name, payloadName = typeNameViolation.head))
      case _ if differentFunctionWithSameName                       => Failure(FunctionWithNameAlreadyExists(name = function.name))
      case _ if hasInvalidUrl                                       => Failure(FunctionHasInvalidUrl(name = function.name, url = getInvalidUrl(function.delivery)))
      case _ if projectHasNameConflict                              => Failure(SchemaExtensionParseError(function.name, "Operation name would conflict with existing schema"))
      case _                                                        => Success(MutactionVerificationSuccess())
    })
  }

}
