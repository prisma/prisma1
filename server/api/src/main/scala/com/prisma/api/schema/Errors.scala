package com.prisma.api.schema

import com.prisma.sangria.utils.ErrorWithCode
import com.prisma.api.database.mutactions.MutactionExecutionResult
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.shared.models.{Project, Relation}
import spray.json.JsValue

abstract class GeneralError(message: String) extends Exception with MutactionExecutionResult with ErrorWithCode {
  override def getMessage: String = message
}

abstract class UserFacingError(message: String, errorCode: Int, val functionError: Option[JsValue] = None) extends GeneralError(message) {
  val code: Int = errorCode
}

object CommonErrors {
  case class TimeoutExceeded()                       extends UserFacingError("The query took too long to process. Either try again later or try a simpler query.", 1000)
  case class InputCompletelyMalformed(input: String) extends UserFacingError(s"input could not be parsed: '$input'", 1001)

  case class QueriesNotAllowedForProject(projectId: String) extends UserFacingError(s"Queries are not allowed for the project with id '$projectId'", 1002)

  case class MutationsNotAllowedForProject(projectId: String)
      extends UserFacingError(s"The project '$projectId' is currently in read-only mode. Please try again in a few minutes", 1003)

  case class ThrottlerBufferFullException() extends UserFacingError("There are too many concurrent queries for this service.", 1004)
}

object APIErrors {
  abstract class ClientApiError(message: String, errorCode: Int) extends UserFacingError(message, errorCode)

  case class TooManyNodesRequested(maxCount: Int)
      extends ClientApiError(s"You requested $maxCount nodes. We will only return up to 1000 nodes per query.", 2041)

  case class GraphQLArgumentsException(reason: String) extends ClientApiError(reason, 3000)

  case class IdIsInvalid(id: String) extends ClientApiError(s"The given id '$id' is invalid.", 3001)

  case class DataItemDoesNotExist(model: String, uniqueField: String, value: String)
      extends ClientApiError(s"'$model' has no item with $uniqueField '$value'", 3002)

  object DataItemDoesNotExist {
    def apply(model: String, id: String): DataItemDoesNotExist = DataItemDoesNotExist(model, "id", id)
  }

  case class DataItemAlreadyExists(modelId: String, id: String) extends ClientApiError(s"'$modelId' already has an item with id '$id'", 3004)

  case class ExtraArguments(arguments: List[String], model: String)
      extends ClientApiError(s"The parameters $arguments were present in the argument list, but are not present in the model $model.", 3005)

  case class ValueTooLong(fieldName: String) extends ClientApiError(s"Value for field $fieldName is too long.", 3007)

  case class UniqueConstraintViolation(modelName: String, details: String)
      extends ClientApiError(s"A unique constraint would be violated on $modelName. Details: $details", 3010)

  case class NodeDoesNotExist(id: String)
      extends ClientApiError(
        s"You are referencing a node that does not exist. Please check your mutation to make sure you are only creating edges between existing nodes. Id if available: $id",
        3011
      )

  case class InvalidConnectionArguments()
      extends ClientApiError(
        s"Including a value for both first and last is not supported. See the spec for a discussion of why https://facebook.github.io/relay/graphql/connections.htm#sec-Pagination-algorithm",
        3014
      )

  case class InvalidToken()
      extends ClientApiError(s"Your token is invalid. It might have expired or you might be using a token from a different project.", 3015)

  case class ProjectNotFound(projectId: String) extends ClientApiError(s"Project not found: '$projectId'", 3016)

  case class ReadonlyField(fieldName: String) extends ClientApiError(s"The field $fieldName is read only.", 3019)

  case class FieldCannotBeNull(fieldName: String = "")
      extends ClientApiError(
        s"You are trying to set a required field to null. If you are using GraphQL arguments, make sure that you specify a value for all arguments. Fieldname if known: $fieldName",
        3020
      )

  case class VariablesParsingError(variables: String) extends ClientApiError(s"Variables could not be parsed as json: $variables", 3024)

  case class InvalidFirstArgument() extends ClientApiError(s"The 'first' argument must be non negative", 3026)

  case class InvalidLastArgument() extends ClientApiError(s"The 'last' argument must be non negative", 3027)

  case class InvalidSkipArgument() extends ClientApiError(s"The 'skip' argument must be non negative", 3028)

  case class RelationIsRequired(fieldName: String, typeName: String)
      extends ClientApiError(s"The field '$fieldName' on type '$typeName' is required. Performing this mutation would violate that constraint", 3032)

  case class FilterCannotBeNullOnToManyField(fieldName: String)
      extends ClientApiError(s"The field '$fieldName' is a toMany relation. This cannot be filtered by null.", 3033)

  case class ConstraintViolated(error: String) extends ClientApiError("The input value violated one or more constraints: " + error, 3035)

  case class InputInvalid(input: String, fieldName: String, fieldType: String)
      extends ClientApiError(s"The input value $input was not valid for field $fieldName of type $fieldType.", 3036)

  case class ValueNotAValidJson(fieldName: String, value: String)
      extends ClientApiError(s"The value in the field '$fieldName' is not a valid Json: '$value'", 3037)

  case class StoredValueForFieldNotValid(fieldName: String, modelName: String)
      extends ClientApiError(s"The value in the field '$fieldName' on the model '$modelName' ist not valid for that field.", 3038)

  case class NodeNotFoundForWhereError(where: NodeSelector)
      extends ClientApiError(s"No Node for the model ${where.model.name} with value ${where.fieldValueAsString} for ${where.field.name} found.", 3039)

  case class NullProvidedForWhereError(modelName: String)
      extends ClientApiError(s"You provided an invalid argument for the where selector on $modelName.", 3040)

  case class NodesNotConnectedError(parentInfo: ParentInfo, innerWhere: NodeSelector)
      extends ClientApiError(
        s"The relation ${parentInfo.relation.name} has no Node for the model ${parentInfo.model.name} with value `${parentInfo.where.fieldValueAsString}` for ${parentInfo.where.field.name} connected to a Node for the model ${innerWhere.model.name} with value `${innerWhere.fieldValueAsString}` for ${innerWhere.field.name}",
        3041
      )

  case class RequiredRelationWouldBeViolated(project: Project, relation: Relation)
      extends ClientApiError(
        s"The change you are trying to make would violate the required relation '${relation.id}' between ${relation.getModelA_!(project.schema).name} and ${relation.getModelB_!(project.schema).name}",
        3042
      )

  case class CascadingDeletePathLoops()
      extends ClientApiError(
        s"There was a loop in the path generated by the onDelete: Cascade directives on your schema when trying to do the delete.",
        3043
      )
}
