package cool.graph.shared.errors

import cool.graph.MutactionExecutionResult
import cool.graph.shared.errors.SystemErrors.SchemaError
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import spray.json.{JsObject, JsString, JsValue}

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

// errors caused by the system - should only appear in system!
// these errors are typically caused by our console or third party applications not using our api correctly
object SystemErrors {
  trait WithSchemaError {
    def schemaError: Option[SchemaError] = None
  }

  abstract class SystemApiError(message: String, errorCode: Int) extends UserFacingError(message, errorCode) with WithSchemaError
  case class SchemaError(`type`: String, description: String, field: Option[String])

  object SchemaError {
    def apply(`type`: String, field: String, description: String): SchemaError = {
      SchemaError(`type`, description, Some(field))
    }

    def apply(`type`: String, description: String): SchemaError = {
      SchemaError(`type`, description, None)
    }

    def global(description: String): SchemaError = {
      SchemaError("Global", description, None)
    }
  }

  case class ProjectPushError(description: String) extends Exception with WithSchemaError {
    override def schemaError: Option[SchemaError] = Some(SchemaError("Global", description = description))
  }

  case class InvalidProjectId(projectId: String) extends SystemApiError(s"No project with id '$projectId'", 4000)

  case class InvalidModelId(modelId: String) extends SystemApiError(s"No model with id '$modelId'", 4001)

  case class InvalidAuthProviderId(authProviderId: String) extends SystemApiError(s"No authProvider with id '$authProviderId'", 4002)

  case class InvalidFieldId(fieldId: String) extends SystemApiError(s"No field with id '$fieldId'", 4003)

  case class InvalidRelationFieldMirrorId(relationFieldMirrorId: String) extends SystemApiError(s"No field with id '$relationFieldMirrorId'", 4004)

  case class InvalidModelPermissionId(modelPermissionId: String) extends SystemApiError(s"No modelPermission with id '$modelPermissionId'", 4005)

  case class InvalidPermissionId(permissionId: String) extends SystemApiError(s"No permission with id '$permissionId'", 4006)

  case class InvalidAlgoliaSyncQueryId(algoliaSyncQueryId: String) extends SystemApiError(s"No algoliaSyncQuery with id '$algoliaSyncQueryId'", 4007)

  case class InvalidStateException(message: String)
      extends SystemApiError(s"Something unexpected happened and as a result your account is in an invalid state. Please contact support.'$message'", 4008)

  case class InvalidActionId(actionId: String) extends SystemApiError(s"No action with id '$actionId'", 4009)

  case class InvalidRelation(error: String) extends SystemApiError(s"The relation is invalid. Reason: $error", 4010)

  case class UnknownExecutionError(message: String, stacktrace: String)
      extends SystemApiError(s"Something unexpected happened in an Action: '$message' - '$stacktrace'", 4011)

  case class InvalidModel(reason: String) extends SystemApiError(s"Please supply a valid model. Reason: $reason", 4012)

  //  4013 is not in use at the moment

  case class FieldNotInModel(fieldName: String, modelName: String)
      extends SystemApiError(s"Field with the name '$fieldName' does not exist on the model '$modelName'", 4014)

  case class ModelPermissionNotInModel(modelPermissionId: String, modelName: String)
      extends SystemApiError(s"ModelPermission '$modelPermissionId' does not exist on the model '$modelName'", 4015)

  case class CannotUpdateSystemField(fieldName: String, modelName: String)
      extends SystemApiError(s"Field with the name '$fieldName' in model '$modelName' is a system field and cannot be updated", 4016) {

    override val schemaError = Some(SchemaError(modelName, fieldName, s"The field `$fieldName` is a system field and cannot be updated."))
  }

  case class SystemFieldCannotBeRemoved(fieldName: String)
      extends SystemApiError(s"Field with the name '$fieldName' is a system field and cannot be removed", 4017)

  case class SystemModelCannotBeRemoved(modelName: String)
      extends SystemApiError(s"Model with the name '$modelName' is a system model and cannot be removed", 4018)

  case class NoModelForField(fieldName: String) extends SystemApiError(s"No model found for field $fieldName", 4019)

  case class IsNotScalar(typeIdentifier: String)
      extends SystemApiError(s"You can only create scalar fields and '$typeIdentifier' is not a scalar value. Did you intend to create a relation?", 4020)

  case class InvalidSecret() extends SystemApiError(s"Provided secret is not correct", 4021)

  case class InvalidRelationId(relationId: String) extends SystemApiError(s"No relation with id '$relationId'", 4022)

  case class InvalidClientId(clientId: String) extends SystemApiError(s"No client with id '$clientId'", 4023)

  case class CantDeleteLastProject() extends SystemApiError("You cannot delete the last project in your account.", 4024)

  case class CantDeleteRelationField(fieldName: String) extends SystemApiError(s"You cannot delete a field that is part of a relation: '$fieldName'", 4025)

  case class CantDeleteProtectedProject(projectId: String) extends SystemApiError(s"You cannot delete a protected project: '$projectId'", 4026)

  case class InvalidSeatEmail(email: String) extends SystemApiError(s"No seat with email '$email'", 4027)

  case class InvalidPatForProject(projectId: String) extends SystemApiError(s"The provided pat is not valid for project '$projectId'", 4028)

  case class InvalidActionTriggerMutationModelId(actiontriggermutationmodelId: String)
      extends SystemApiError(s"No actiontriggermutationmodel with id '$actiontriggermutationmodelId'", 4029)

  case class InvalidActionTriggerMutationRelationId(actiontriggermutationmodelId: String)
      extends SystemApiError(s"No actiontriggermutationrelation with id '$actiontriggermutationmodelId'", 4030)

  case class InvalidIntegrationId(integrationId: String) extends SystemApiError(s"No Integration with id '$integrationId'", 4031)

  case class InvalidSeatId(seatId: String) extends SystemApiError(s"No Seat with id '$seatId'", 4032)

  case class InvalidProjectName(name: String) extends SystemApiError(s"No Project with name '$name'", 4033)

  case class RelationPermissionNotInModel(relationPermissionId: String, relationName: String)
      extends SystemApiError(s"RelationPermission '$relationPermissionId' does not exist on the relation '$relationName'", 4034)

  case class InvalidRelationPermissionId(relationPermissionId: String) extends SystemApiError(s"No relationPermission with id '$relationPermissionId'", 4035)

  case class InvalidPackageDefinitionId(packageDefinitionId: String) extends SystemApiError(s"No PackageDefinition with id '$packageDefinitionId'", 4036)

  case class InvalidEnumId(id: String) extends SystemApiError(s"No Enum with id '$id'", 4037)

  case class InvalidFunctionId(id: String) extends SystemApiError(s"No Function with id '$id'", 4038)

  case class InvalidPackageName(packageName: String) extends SystemApiError(s"No Package with name '$packageName'", 4039)

  case class InvalidEnumName(enumName: String) extends SystemApiError(s"An Enum with the name '$enumName' already exists.", 4040)

  case class InvalidProjectDatabase(projectDatabaseIdOrRegion: String)
      extends SystemApiError(s"A ProjectDatabase with the id or region '$projectDatabaseIdOrRegion' does not exist.", 4041)
  case class InvalidFieldConstraintId(constraintId: String) extends SystemApiError(s"A Constraint with the id '$constraintId' does not exist.", 4041)

  case class DuplicateFieldConstraint(constraintType: String, fieldId: String)
      extends SystemApiError(s"A Constraint with the type '$constraintType' already exists for the field with the id: $fieldId.", 4042)

  case class FieldConstraintTypeNotCompatibleWithField(constraintType: String, fieldId: String, fieldType: String)
      extends SystemApiError(s"A Constraint with the type '$constraintType' is not possible on the field with the type: $fieldType and the id: $fieldId.", 4043)

  case class ListFieldConstraintOnlyOnListFields(fieldId: String)
      extends SystemApiError(s"The field with the id: '$fieldId' is not a list field and therefore cannot take a List constraint", 4044)

  case class UpdatingTheFieldWouldViolateConstraint(fieldId: String, constraintId: String)
      extends SystemApiError(s"Updating the field with the id: '$fieldId' would violate the constraint with the id: $constraintId", 4045)

  case class InvalidFunctionName(name: String) extends SystemApiError(s"No Function with name '$name'", 4046)

  case class InvalidRequestPipelineOperation(operation: String)
      extends SystemApiError(s"RequestPipeline Operation has to be create, update or delete. You provided '$operation'", 4047)

  case class InvalidFunctionType(typename: String) extends SystemApiError(s"The function type was invalid. You provided '$typename'", 4048)

  case class InvalidFunctionHeader(header: String) extends SystemApiError(s"The function header was invalid. You provided '$header'", 4049)

  case class InvalidPredefinedFieldFormat(fieldName: String, underlying: String)
      extends SystemApiError(s"The field $fieldName is a predefined but hidden type and has to have a specific format to be exposed. $underlying", 4050)

  case class InvalidSeatClientId(clientId: String) extends SystemApiError(s"No Seat with clientId '$clientId' found on the project.", 4051)

  case class OnlyOwnerOfProjectCanTransferOwnership() extends SystemApiError(s"Only the owner of a project can transfer ownership.", 4052)

  case class NewOwnerOfAProjectNeedsAClientId()
      extends SystemApiError(
        s"The collaborator you are trying to make an owner has not joined graph.cool yet. Please ask him to register before transferring the ownership.",
        4053)

  case class EmailAlreadyIsTheProjectOwner(email: String) extends SystemApiError(s"The project is already owned by the seat with the email: '$email'", 4054)

}

// errors caused by user input - these errors should not appear in simple or relay!
object UserInputErrors {
  import SystemErrors.SystemApiError

  case class InvalidRootTokenId(rootTokenId: String) extends SystemApiError(s"No Permanent Auth Token with id '$rootTokenId'", 2000)

  case class InvalidSession() extends SystemApiError("No valid session", 2001)

  case class ModelWithNameAlreadyExists(name: String) extends SystemApiError(s"A model with the name '$name' already exists in your project", 2002)

  case class ProjectWithNameAlreadyExists(name: String) extends SystemApiError(s"A project with the name '$name' already exists in your account", 2003)

  case class ChangedIsListAndNoMigrationValue(fieldName: String)
      extends SystemApiError(s"'$fieldName' is changed to or from a list scalar type and you did not specify a migrationValue.", 2004)

  case class InvalidPassword() extends SystemApiError(s"The password is not correct", 2005)

  case class InvalidResetPasswordToken(token: String) extends SystemApiError(s"That reset password token is not valid. Maybe you used it already?", 2006)

  case class RequiredAndNoMigrationValue(modelName: String, fieldName: String)
      extends SystemApiError(s"'$fieldName' is required and you did not specify a migrationValue.", 2007) {

    override val schemaError = Some {
      SchemaError(
        modelName,
        fieldName,
        s"""The field `$fieldName` must specify the `@migrationValue` directive, because its type was changed or it became required: `@migrationValue(value: "42")`"""
      )
    }
  }

  case class InvalidName(name: String)                   extends SystemApiError(InvalidNames.default(name), 2008)
  case class InvalidNameMustStartUppercase(name: String) extends SystemApiError(InvalidNames.mustStartUppercase(name), 2008)
  object InvalidNames {
    def mustStartUppercase(name: String): String = s"'${default(name)} It must begin with an uppercase letter. It may contain letters and numbers."
    def default(name: String): String            = s"'$name' is not a valid name."
  }

  case class FieldAreadyExists(name: String) extends SystemApiError(s"A field with the name '$name' already exists", 2009)

  case class MissingEnumValues() extends SystemApiError("You must provide an enumValues argument when specifying the 'Enum' typeIdentifier", 2010)

  case class InvalidValueForScalarType(value: String, typeIdentifier: TypeIdentifier)
      extends SystemApiError(s"'$value' is not a valid value for type '$typeIdentifier'", 2011)

  case class InvalidUserPath(modelName: String) extends SystemApiError(s"Not a valid user path for model $modelName.", 2012)

  case class FailedLoginException() extends SystemApiError("Wrong user data", 2013)

  case class EdgesAlreadyExist()
      extends SystemApiError(s"You cannot change the models of a relation that contains edges. Either remove all edges or create a new relation", 2014)

  case class NotFoundException(reason: String) extends SystemApiError(reason, 2015)

  case class OneToManyRelationSameModelSameField()
      extends SystemApiError(s"Cannot create a one-to-many relation between the same model using the same field", 2016)

  case class ClientEmailInUse() extends SystemApiError(s"That email is already in use", 2017)

  case class CouldNotActivateIntegration(name: String, reason: String) extends SystemApiError(s"Could not activate integration: $name. '$reason'", 2018)

  case class CouldNotDeactivateIntegration(name: String, reason: String) extends SystemApiError(s"Could not deactivate integration: $name. '$reason'", 2019)

  case class RelationNameAlreadyExists(name: String) extends SystemApiError(s"A relation with that name already exists: $name.", 2020)

  case class EnumValueInUse() extends SystemApiError(s"The Enum value you are removing is in use. Please provide a migration Value.", 2021) {
    override val schemaError = Some {
      SchemaError.global(
        s"An enum type is used in a non-list enum field on a type that has nodes and therefore can't be removed. Please provide a migrationValue.")
    }
  }

  case class CantRemoveEnumValueWhenNodesExist(modelName: String, fieldName: String)
      extends SystemApiError(
        s"It is not possible to remove an enum value for a List field when there are existing data nodes. Please provide a migration Value for $fieldName on $modelName.",
        2022
      ) {
    override val schemaError = Some {
      SchemaError(
        modelName,
        fieldName,
        s"The type `$modelName` has nodes and therefore the enum values associated with `$fieldName` can't be removed. Please provide a migrationValue."
      )
    }
  }

  case class ActionInputIsInconsistent(message: String) extends SystemApiError(s"The input you provided for the action is invalid: $message", 2023)

  case class ExistingDuplicateDataPreventsUniqueIndex(fieldName: String)
      extends SystemApiError(s"The field '$fieldName' contains duplicate data. Please remove duplicates before enabling the unique constraint", 2024)

  case class DefaultValueIsNotValidEnum(value: String)
      extends SystemApiError(s"The specified default value '$value' is not a valid Enum Value for this field.", 2025)

  case class DuplicateEmailFromMultipleProviders(email: String)
      extends SystemApiError(
        s"It looks like you previously signed up with a different provider with the same email ($email). Please sign in with the same provider again.",
        2026)

  case class RequiredSearchProviderAlgoliaNotPresent()
      extends SystemApiError(s"You must enable the Algolia integration before you add queries to sync data. Please enable this integration first.", 2027)

  case class AlgoliaCredentialsDontHaveRequiredPermissions()
      extends SystemApiError(
        s"Please check that the Application ID and API Key is correct. You can find both on the API Keys page in the Algolia web interface. You must create a new API Key and enable 'Add records' and 'Delete records'. Make sure that you are not using the Admin API Key, as Algolia doesn't allow it to be used here.",
        2028
      )

  case class ProjectAlreadyHasSearchProviderAlgolia()
      extends SystemApiError(s"This project already has an Algolia integration. Try setup a sync query for a new modal using the existing integration.", 2029)

  case class ObjectDoesNotExistInCurrentProject(message: String) extends SystemApiError(s"The referenced object does not exist in this project: $message", 2030)

  case class RelationChangedFromListToSingleAndNodesPresent(fieldName: String)
      extends SystemApiError(
        s"'$fieldName' is a relation field. Changing it from a to-many to a to-one field is not allowed when there are already nodes in the relation.",
        2031)

  case class TooManyNodesToExportData(maxCount: Int)
      extends SystemApiError(s"One of your models had more than $maxCount nodes. Please contact support to get a manual data export.", 2032)

  case class InvalidProjectAlias(alias: String) extends SystemApiError(s"'$alias' is not a valid project alias", 2033)

  case class ProjectWithAliasAlreadyExists(alias: String)
      extends SystemApiError(s"A project with the alias '$alias' already exists. Aliases are globally unique. Please try something else.", 2034)

  case class ProjectAliasEqualsAnExistingId(alias: String)
      extends SystemApiError(s"A project with the id '$alias' already exists. You cannot set the alias to that of an existing project id!.", 2035)

  case class EmailIsNotGraphcoolUser(email: String)
      extends SystemApiError(s"No Graphcool user exists with the email '$email'. Please ask your collaborator to create a Graphcool account.", 2036)

  case class CollaboratorProjectWithNameAlreadyExists(name: String)
      extends SystemApiError(s"A project with the name '$name' already exists in collaborators account", 2037)

  case class StripeError(message: String) extends SystemApiError(message, 2038)

  case class InvalidSchema(message: String) extends SystemApiError(s"The schema is invalid: $message", 2040)

  case class TooManyNodesRequested(maxCount: Int)
      extends SystemApiError(s"You requested $maxCount nodes. We will only return up to 1000 nodes per query.", 2041)

  case class MigrationValueIsNotValidEnum(value: String)
      extends SystemApiError(s"The specified migration value '$value' is not a valid Enum Value for this field.", 2042)

  case class ListRelationsCannotBeRequired(fieldName: String)
      extends SystemApiError(s"The field '$fieldName' is a list relation and can not be required.", 2043)

  case class EnumIsReferencedByField(fieldName: String, typeName: String)
      extends SystemApiError(s"The field '$fieldName' on type '$typeName' is still referencing this enum.", 2044)

  case class NoEnumSelectedAlthoughSetToEnumType(fieldName: String)
      extends SystemApiError(s"The field type for field '$fieldName' is set to enum. You must also select an existing enum.", 2045)

  case class TypeAlreadyExists(name: String) extends SystemApiError(s"A type with the name '$name' already exists in your project", 2046)

  case class SettingRelationRequiredButNodesExist(fieldName: String)
      extends SystemApiError(s"'$fieldName' is required but there are already nodes present without that relation.", 2047)

  case class ServerSideSubscriptionQueryIsInvalid(error: String, functionName: String)
      extends SystemApiError(s"The supplied query for the server side subscription `$functionName` is invalid. $error", 2048)

  case class InvalidMigrationValueForEnum(modelName: String, fieldName: String, migrationValue: String)
      extends SystemApiError(s"You supplied an enum migrationValue that is not appropriate for model: $modelName field: $fieldName value: $migrationValue",
                             2049) {
    override val schemaError = Some {
      SchemaError(modelName, fieldName, s"The provided migrationValue `$migrationValue` has the wrong List status for field `$fieldName` on type `$modelName`.")
    }
  }

  case class CantRenameSystemModels(name: String) extends SystemApiError(s"You tried renaming a system model. This is not possible. modelName: $name", 2050)

  case class TypeChangeRequiresMigrationValue(fieldName: String) extends SystemApiError(s"The type change on '$fieldName' requires a migrationValue.", 2051)

  case class AddingRequiredRelationButNodesExistForModel(modelName: String, fieldName: String)
      extends SystemApiError(s"You are adding a required relation to '$modelName' but there are already items.", 2052) {

    override val schemaError = Some {
      SchemaError(
        modelName,
        fieldName,
        s"The relation field `$fieldName` cannot be made required, because there are already instances of the enclosing type that violate this constraint."
      )
    }
  }

  case class SchemaExtensionParseError(functionName: String, message: String)
      extends SystemApiError(s"Schema Extension Error for function '$functionName': $message", 2053)

  case class FunctionWithNameAlreadyExists(name: String) extends SystemApiError(s"A function with the name '$name' already exists in your project", 2054)

  case class SameRequestPipeLineFunctionAlreadyExists(modelName: String, operation: String, binding: String)
      extends SystemApiError(
        s"A Request Pipeline Function for type $modelName, the trigger '$operation' and the step '$binding' already exists in your project.",
        2055)

  case class FunctionHasInvalidUrl(name: String, url: String) extends SystemApiError(s"Function with name '$name' has invalid url: '$url'", 2056)

  case class EnumValueUsedAsDefaultValue(value: String, fieldName: String)
      extends SystemApiError(s"The enumValue '$value' can't be removed. It is used as DefaultValue on field: '$fieldName'", 2057)

  case class PermissionQueryIsInvalid(error: String, permissionNameOrId: String)
      extends SystemApiError(s"The supplied query for the permission `$permissionNameOrId` is invalid. $error", 2058)

  case class RootTokenNameAlreadyInUse(rootTokenName: String) extends SystemApiError(s"There is already a RootToken with the name `$rootTokenName`.", 2059)

  case class IllegalFunctionName(name: String) extends SystemApiError(s"The function name does not match the naming rule. Name: '$name'", 2060)

  case class ProjectEjectFailure(message: String) extends SystemApiError(s"The project could not be ejected because $message", 2061)

  case class InvalidRootTokenName(name: String) extends SystemApiError(s"No RootToken with the name: $name", 2062)

  case class ResolverPayloadIsRequired() extends SystemApiError(s"The payloadType for the resolver is not nullable.", 2063)

  case class ResolverFunctionHasDuplicateSchemaFilePath(name: String, path: String)
      extends SystemApiError(s"The Resolver Function with name '$name' has the path: '$path'. This schemaFilePath is already in use.", 2064)

  case class FunctionHasInvalidPayloadName(name: String, payloadName: String)
      extends SystemApiError(s"Function with name '$name' has invalid payloadName: '$payloadName'", 2065)

  case class QueryPermissionParseError(ruleName: String, message: String)
      extends SystemApiError(s"Query Permission Error for permission '$ruleName': $message", 2066)

  case class ModelOrRelationForPermissionDoesNotExist(name: String)
      extends SystemApiError(s"Did not find the type or relation you provided a permission for: '$name'", 2066)
}

// errors caused by the client when using the relay/simple API- should only appear in relay/simple/shared!
object UserAPIErrors {
  abstract class ClientApiError(message: String, errorCode: Int) extends UserFacingError(message, errorCode)

  case class GraphQLArgumentsException(reason: String) extends ClientApiError(reason, 3000)

  case class IdIsInvalid(id: String) extends ClientApiError(s"The given id '$id' is invalid.", 3001)

  case class DataItemDoesNotExist(modelId: String, id: String) extends ClientApiError(s"'$modelId' has no item with id '$id'", 3002)

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

}

object RequestPipelineErrors {
  abstract class RequestPipelineError(message: String, errorCode: Int, functionError: Option[JsValue] = None)
      extends UserFacingError(message, errorCode, functionError)

  case class UnhandledFunctionError(executionId: String)
      extends RequestPipelineError(s"""A function returned an unhandled error. Please check the logs for executionId '$executionId'""", 5000)

  case class FunctionReturnedErrorMessage(error: String) extends RequestPipelineError(s"""function execution error: $error""", 5001, Some(JsString(error)))

  case class FunctionReturnedErrorObject(errorObject: JsObject) extends RequestPipelineError(s"""function execution error""", 5002, Some(errorObject))

  case class FunctionReturnedInvalidBody(executionId: String)
      extends RequestPipelineError(
        s"""A function returned an invalid body. You can refer to the docs for the expected shape. Please check the logs for executionId '$executionId'""",
        5003
      )

  case class JsonObjectDoesNotMatchGraphQLType(fieldName: String, expectedFieldType: String, json: String)
      extends RequestPipelineError(
        s"Returned Json Object does not match the GraphQL type. The field '$fieldName' should be of type $expectedFieldType \n\n Json: $json\n\n",
        5004)

  case class FunctionWebhookURLWasNotValid(executionId: String)
      extends RequestPipelineError(s"""A function webhook url was not valid. Please check the logs for executionId '$executionId'""", 5005)

  case class ReturnedDataWasNotAnObject() extends RequestPipelineError(s"""The return value should include a 'data' field of type object""", 5006)

  case class DataDoesNotMatchPayloadType() extends RequestPipelineError(s"""The value of the data object did not match the specified payloadType.""", 5007)

}
