package cool.graph.api.schema

trait ApiError extends Exception {
  def message: String
  def errorCode: Int
}

abstract class AbstractApiError(val message: String, val errorCode: Int) extends ApiError

case class InvalidProjectId(projectId: String) extends AbstractApiError(s"No service with id '$projectId'", 4000)

import cool.graph.api.database.mutactions.MutactionExecutionResult
import cool.graph.api.mutations.NodeSelector
import spray.json.JsValue

abstract class GeneralError(message: String) extends Exception with MutactionExecutionResult {
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
}

// errors caused by the client when using the relay/simple API- should only appear in relay/simple/shared!
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

  case class IdIsMissing() extends ClientApiError(s"An Id argument was expected, but not found.", 3003)

  case class DataItemAlreadyExists(modelId: String, id: String) extends ClientApiError(s"'$modelId' already has an item with id '$id'", 3004)

  case class ExtraArguments(arguments: List[String], model: String)
      extends ClientApiError(s"The parameters $arguments were present in the argument list, but are not present in the model $model.", 3005)

  case class InvalidValue(valueName: String) extends ClientApiError(s"Please supply a valid value for $valueName.", 3006)

  case class ValueTooLong(fieldName: String) extends ClientApiError(s"Value for field $fieldName is too long.", 3007)

  case class InsufficientPermissions(reason: String) extends ClientApiError(reason, 3008)

  case class RelationAlreadyFull(relationId: String, field1: String, field2: String)
      extends ClientApiError(s"'$relationId' is already connecting fields '$field1' and '$field2'", 3009)

  case class UniqueConstraintViolation(modelName: String, details: String)
      extends ClientApiError(s"A unique constraint would be violated on $modelName. Details: $details", 3010)

  case class NodeDoesNotExist(id: String)
      extends ClientApiError(
        s"You are referencing a node that does not exist. Please check your mutation to make sure you are only creating edges between existing nodes. Id if available: $id",
        3011
      )

  case class ItemAlreadyInRelation() extends ClientApiError(s"An edge already exists between the two nodes.", 3012)

  case class NodeNotFoundError(id: String) extends ClientApiError(s"Node with id $id not found", 3013)

  // todo: throw in simple
  case class InvalidConnectionArguments()
      extends ClientApiError(
        s"Including a value for both first and last is not supported. See the spec for a discussion of why https://facebook.github.io/relay/graphql/connections.htm#sec-Pagination-algorithm",
        3014
      )

  case class InvalidToken()
      extends ClientApiError(s"Your token is invalid. It might have expired or you might be using a token from a different project.", 3015)

  case class ProjectNotFound(projectId: String) extends ClientApiError(s"Project not found: '$projectId'", 3016)

  case class InvalidSigninData() extends ClientApiError("Your signin credentials are incorrect. Please try again", 3018)

  case class ReadonlyField(fieldName: String) extends ClientApiError(s"The field $fieldName is read only.", 3019)

  case class FieldCannotBeNull(fieldName: String = "")
      extends ClientApiError(
        s"You are trying to set a required field to null. If you are using GraphQL arguments, make sure that you specify a value for all arguments. Fieldname if known: $fieldName",
        3020
      )

  case class CannotCreateUserWhenSignedIn() extends ClientApiError(s"It is not possible to create a user when you are already signed in.", 3021)

  case class CannotSignInCredentialsInvalid() extends ClientApiError(s"No user found with that information", 3022)

  case class CannotSignUpUserWithCredentialsExist() extends ClientApiError(s"User already exists with that information", 3023)

  case class VariablesParsingError(variables: String) extends ClientApiError(s"Variables could not be parsed as json: $variables", 3024)

  case class Auth0IdTokenIsInvalid()
      extends ClientApiError(s"The provided idToken is invalid. Please see https://auth0.com/docs/tokens/id_token for how to obtain a valid idToken", 3025)

  case class InvalidFirstArgument() extends ClientApiError(s"The 'first' argument must be non negative", 3026)

  case class InvalidLastArgument() extends ClientApiError(s"The 'last' argument must be non negative", 3027)

  case class InvalidSkipArgument() extends ClientApiError(s"The 'skip' argument must be non negative", 3028)

  case class UnsuccessfulSynchronousMutationCallback() extends ClientApiError(s"A Synchronous Mutation Callback failed", 3029)

  case class InvalidAuthProviderData(message: String) extends ClientApiError(s"provided authProvider fields is invalid: '$message'", 3030)

  case class GenericServerlessFunctionError(functionName: String, message: String)
      extends ClientApiError(s"The function '$functionName' returned an error: '$message'", 3031)

  case class RelationIsRequired(fieldName: String, typeName: String)
      extends ClientApiError(s"The field '$fieldName' on type '$typeName' is required. Performing this mutation would violate the constraint", 3032)

  case class FilterCannotBeNullOnToManyField(fieldName: String)
      extends ClientApiError(s"The field '$fieldName' is a toMany relation. This cannot be filtered by null.", 3033)

  case class UnhandledFunctionError(functionName: String, requestId: String)
      extends ClientApiError(s"The function '$functionName' returned an unhandled error. Please check the logs for requestId '$requestId'", 3034)

  case class ConstraintViolated(error: String) extends ClientApiError("The input value violated one or more constraints: " + error, 3035)

  case class InputInvalid(input: String, fieldName: String, fieldType: String)
      extends ClientApiError(s"The input value $input was not valid for field $fieldName of type $fieldType.", 3036)

  case class ValueNotAValidJson(fieldName: String, value: String)
      extends ClientApiError(s"The value in the field '$fieldName' is not a valid Json: '$value'", 3037)

  case class StoredValueForFieldNotValid(fieldName: String, modelName: String)
      extends ClientApiError(s"The value in the field '$fieldName' on the model '$modelName' ist not valid for that field.", 3038)

  case class NodeNotFoundForWhereError(where: NodeSelector)
      extends ClientApiError(s"No Node for the model ${where.model.name} with value ${where.fieldValueAsString} for ${where.fieldName} found.", 3039)

  case class NullProvidedForWhereError(modelName: String)
      extends ClientApiError(s"You provided an invalid argument for the unique selector on $modelName.", 3040)

}
