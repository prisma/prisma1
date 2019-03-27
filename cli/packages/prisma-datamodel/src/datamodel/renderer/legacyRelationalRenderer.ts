import Renderer from './renderer'
import { LegacyRelationalReservedFields } from '../legacyFields'
import {
  ISDL,
  IGQLType,
  IDirectiveInfo,
  IGQLField,
  cloneSchema,
} from '../model'
import GQLAssert from '../../util/gqlAssert'
import { TypeIdentifiers } from '../scalar'
import { DirectiveKeys } from '../directives'
/**
 * Renderer implementation for relational models.
 */
export default class RelationalRenderer extends Renderer {
  // The legacy renderer changes field names on rendering,
  // so we force it to operate on a copy.
  public render(input: ISDL, sortBeforeRendering: boolean = false): string {
    return super.render(cloneSchema(input), sortBeforeRendering)
  }

  // Special case for postgres. We never render id, createdAt, isCreatedAt directive.
  protected shouldCreateIsIdFieldDirective(field: IGQLField) {
    return false
  }

  protected shouldCreateCreatedAtFieldDirective(field: IGQLField) {
    return false
  }

  protected shouldCreateUpdatedAtFieldDirective(field: IGQLField) {
    return false
  }

  protected shouldCreateSequenceFieldDirective(field: IGQLField) {
    return false
  }

  protected shouldCreateScalarListDirective(field: IGQLField) {
    return false
  }

  protected shouldCreateIsUniqueFieldDirective(field: IGQLField) {
    return field.isUnique || field.isId
  }

  // Avoid embedded types
  protected renderType(type: IGQLType): string {
    if (type.isEmbedded) {
      GQLAssert.raise('Embedded types are not supported in relational models.')
    }

    return super.renderType(type)
  }

  // Use PG specific table/column refs
  protected createDatabaseNameTypeDirective(type: IGQLType) {
    return {
      name: 'pgTable',
      arguments: {
        name: this.renderValue(TypeIdentifiers.string, type.databaseName),
      },
    }
  }

  protected createDatabaseNameFieldDirective(field: IGQLField) {
    return {
      name: 'pgColumn',
      arguments: {
        name: this.renderValue(TypeIdentifiers.string, field.databaseName),
      },
    }
  }

  // Assert some basic rules
  protected renderField(field: IGQLField): string {
    if (
      field.isId &&
      field.name !== LegacyRelationalReservedFields.idFieldName
    ) {
      field.databaseName = field.databaseName || field.name
      field.name = LegacyRelationalReservedFields.idFieldName
    }
    if (
      field.isCreatedAt &&
      field.name !== LegacyRelationalReservedFields.createdAtFieldName
    ) {
      field.databaseName = field.databaseName || field.name
      field.name = LegacyRelationalReservedFields.createdAtFieldName
    }
    if (
      field.isUpdatedAt &&
      field.name !== LegacyRelationalReservedFields.updatedAtFieldName
    ) {
      field.databaseName = field.databaseName || field.name
      field.name = LegacyRelationalReservedFields.updatedAtFieldName
    }

    return super.renderField(field)
  }

  // Remove @relation(link: TABLE) directive.
  protected renderDirectives(directives: IDirectiveInfo[]) {
    return super.renderDirectives(
      directives.filter(
        dir =>
          dir.name !== DirectiveKeys.relation || dir.arguments.link !== 'TABLE',
      ),
    )
  }
}
