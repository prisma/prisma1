import Parser from './parser'
import { IGQLField, IGQLType } from '../model'
import { LegacyRelationalReservedFields } from '../legacyFields'
import { DirectiveKeys } from '../directives'

/**
 * Parser implementation for related models.
 */
export default class RelationalParser extends Parser {
  /**
   * Postgres fallback for reserved field names.
   */
  protected isIdField(field: any): boolean {
    return (
      this.hasDirective(field, DirectiveKeys.isId) ||
      field.name.value === LegacyRelationalReservedFields.idFieldName
    )
  }
  protected isCreatedAtField(field: any): boolean {
    return (
      this.hasDirective(field, DirectiveKeys.isCreatedAt) ||
      field.name.value === LegacyRelationalReservedFields.createdAtFieldName
    )
  }
  protected isUpdatedAtField(field: any): boolean {
    return (
      this.hasDirective(field, DirectiveKeys.isUpdatedAt) ||
      field.name.value === LegacyRelationalReservedFields.updatedAtFieldName
    )
  }
  protected isEmbedded(type: any): boolean {
    // Related models are never embedded
    return false
  }

  /**
   * Postgres fallback for db directive, which is not known in postgres.
   */
  protected getDatabaseFieldName(field: IGQLField): string | null {
    return super.getDatabaseFieldName(field) || this.getPgColumnName(field)
  }

  protected getDatabaseTypeName(type: IGQLType): string | null {
    return super.getDatabaseTypeName(type) || this.getPgTableName(type)
  }

  protected getPgColumnName(field: IGQLField): string | null {
    const directive = this.getDirectiveByName(field, 'pgColumn')
    return this.getDirectiveArgument(directive, 'name')
  }

  protected getPgTableName(type: IGQLType): string | null {
    const directive = this.getDirectiveByName(type, 'pgTable')
    return this.getDirectiveArgument(directive, 'name')
  }
}
