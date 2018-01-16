package com.prisma.shared.errors

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier

// errors caused by user input - these errors should not appear in simple or relay!
object UserInputErrors {

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

  case class InvalidName(name: String, entityType: String)                   extends SystemApiError(InvalidNames.default(name, entityType), 2008)
  case class InvalidNameMustStartUppercase(name: String, entityType: String) extends SystemApiError(InvalidNames.mustStartUppercase(name, entityType), 2008)
  object InvalidNames {
    def mustStartUppercase(name: String, entityType: String): String =
      s"'${default(name, entityType)} It must begin with an uppercase letter. It may contain letters and numbers."
    def default(name: String, entityType: String): String = s"'$name' is not a valid name for a$entityType."
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

  case class ResolverPayloadIsRequired(resolverName: String)
      extends SystemApiError(s"The payloadType for the resolver `$resolverName` is not nullable, but the resolver returned null.", 2063)

  case class ResolverFunctionHasDuplicateSchemaFilePath(name: String, path: String)
      extends SystemApiError(s"The Resolver Function with name '$name' has the path: '$path'. This schemaFilePath is already in use.", 2064)

  case class FunctionHasInvalidPayloadName(name: String, payloadName: String)
      extends SystemApiError(s"Function with name '$name' has invalid payloadName: '$payloadName'", 2065)

  case class QueryPermissionParseError(ruleName: String, message: String)
      extends SystemApiError(s"Query Permission Error for permission '$ruleName': $message", 2066)

  case class ModelOrRelationForPermissionDoesNotExist(name: String)
      extends SystemApiError(s"Did not find the type or relation you provided a permission for: '$name'", 2066)
}
