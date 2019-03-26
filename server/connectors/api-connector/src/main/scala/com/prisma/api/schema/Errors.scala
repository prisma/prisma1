package com.prisma.api.schema

import java.sql.SQLException

import com.prisma.api.connector.NodeSelector
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.{Model, Relation}

abstract class UserFacingError(val message: String, val code: Int) extends Exception(message)

object CommonErrors {
  case class TimeoutExceeded()                       extends UserFacingError("The query took too long to process. Either try again later or try a simpler query.", 1000)
  case class InputCompletelyMalformed(input: String) extends UserFacingError(s"input could not be parsed: '$input'", 1001)

  case class QueriesNotAllowedForProject(projectId: String) extends UserFacingError(s"Queries are not allowed for the project with id '$projectId'", 1002)

  case class MutationsNotAllowedForProject(projectId: String)
      extends UserFacingError(s"The project '$projectId' is currently in read-only mode. Please try again in a few minutes", 1003)

  case class ThrottlerBufferFull() extends UserFacingError("There are too many concurrent queries for this service.", 1004)
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

  // todo: this error may not be required anymore. It is just used in creates and i cannot see how this can happen within a create.
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

  case class AuthFailure() extends ClientApiError(s"Your token is invalid. It might have expired or you might be using a token from a different project.", 3015)

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

  case class NodeNotFoundForWhereErrorNative(modelName: String, value: GCValue, fieldName: String)
      extends ClientApiError(s"No Node for the model $modelName with value $value for $fieldName found.", 3039)

  case class NodeNotFoundForWhereError(where: NodeSelector)
    extends ClientApiError(s"No Node for the model ${where.model.name} with value ${where.value} for ${where.field.name} found.", 3039)

  case class NullProvidedForWhereError(modelName: String)
      extends ClientApiError(s"You provided an invalid argument for the where selector on $modelName.", 3040)

  case class NodesNotConnectedError(relation: Relation, parent: Model, parentWhere: Option[NodeSelector], child: Model, childWhere: Option[NodeSelector])
      extends ClientApiError(pathErrorMessage(relation, parent, parentWhere, child, childWhere), 3041)

  case class RequiredRelationWouldBeViolated(relation: Relation)
      extends ClientApiError(
        s"The change you are trying to make would violate the required relation '${relation.name}' between ${relation.modelA.name} and ${relation.modelB.name}",
        3042
      )

  case class MongoConflictingUpdates(model: String, override val message: String)
      extends ClientApiError(
        s"You have several updates affecting the same area of the document underlying $model. MongoMessage: $message",
        3043
      )

  case class MongoInvalidObjectId(id: String)
      extends ClientApiError(
        s"You provided an ID that was not a valid MongoObjectId: $id",
        3044
      )

  case class ExecuteRawError(e: SQLException) extends ClientApiError(e.getMessage, e.getErrorCode)

  def pathErrorMessage(relation: Relation, parent: Model, parentWhere: Option[NodeSelector], child: Model, childWhere: Option[NodeSelector]) = {
    (parentWhere, childWhere) match {
      case (Some(parentWhere), Some(childWhere)) =>
        s"The relation ${relation.name} has no node for the model ${parent.name} with the value '${parentWhere.value}' for the field '${parentWhere.field.name}' connected to a node for the model ${child.name} with the value '${childWhere.value}' for the field '${childWhere.field.name}'"
      case (Some(parentWhere), None) =>
        s"The relation ${relation.name} has no node for the model ${parent.name} with the value '${parentWhere.value}' for the field '${parentWhere.field.name}' connected to a node for the model ${child.name} on your mutation path."
      case (None, Some(childWhere)) =>
        s"The relation ${relation.name} has no node for the model ${parent.name} connected to a Node for the model ${child.name} with the value '${childWhere.value}' for the field '${childWhere.field.name}' on your mutation path."
      case (None, None) =>
        s"The relation ${relation.name} has no node for the model ${parent.name} connected to a Node for the model ${child.name} on your mutation path."
    }
  }
}
