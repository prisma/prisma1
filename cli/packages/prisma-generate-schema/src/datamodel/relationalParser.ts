import Parser from './parser'

export const idFieldName = 'id'
export const createdAtFieldName = 'createdAt'
export const updatedAtFieldName = 'updatedAt'

/**
 * Parser implementation for related models. 
 */
export default class RelationalParser extends Parser {
  protected isIdField(field: any): boolean {
    return field.name.value === idFieldName
  }
  protected isReadOnly(field: any): boolean {
    return field.name.value === idFieldName ||
      field.name.value === createdAtFieldName ||
      field.name.value === updatedAtFieldName
  }
  protected isEmbedded(type: any): boolean {
    // Related models are never embedded
    return false
  }

}