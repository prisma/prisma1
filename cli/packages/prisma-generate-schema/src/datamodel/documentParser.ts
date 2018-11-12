import Parser from './parser'

export const isIdDirectiveKey = 'id'
export const isCreatedAtDirectiveKey = 'createdAt'
export const isUpdatedAtDirectiveKey = 'updatedAt'
export const isEmbeddedDirectiveKey = 'embedded'

/**
 * Parser implementation for document models. 
 */
export default class DocumentParser extends Parser {
  public isIdField(field: any): boolean {
    return this.hasDirective(field, isIdDirectiveKey)
  }
  public isReadOnly(field: any): boolean {
    return this.hasDirective(field, isIdDirectiveKey) ||
    this.hasDirective(field, isCreatedAtDirectiveKey) ||
    this.hasDirective(field, isUpdatedAtDirectiveKey)
  }
  public isEmbedded(type: any): boolean {
    return this.hasDirective(type, isEmbeddedDirectiveKey)
  }

}