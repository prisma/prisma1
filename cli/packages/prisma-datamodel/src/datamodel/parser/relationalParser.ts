import Parser from './parser'
import { IGQLField, IGQLType } from '../model'

/**
 * Parser implementation for related models. 
 */
export default class RelationalParser extends Parser {
  public static idFieldName = 'id'
  public static createdAtFieldName = 'createdAt'
  public static updatedAtFieldName = 'updatedAt'

  protected isIdField(field: any): boolean {
    return field.name.value === RelationalParser.idFieldName
  }
  protected isCreatedAtField(field: any): boolean {
    return field.name.value === RelationalParser.createdAtFieldName
  }
  protected isUpdatedAtField(field: any): boolean {
    return field.name.value === RelationalParser.updatedAtFieldName
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