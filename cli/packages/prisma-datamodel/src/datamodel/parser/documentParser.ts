import Parser from './parser'
import { DirectiveKeys } from '../directives'
/**
 * Parser implementation for document models. 
 */
export default class DocumentParser extends Parser {
  public isIdField(field: any): boolean {
    return this.hasDirective(field, DirectiveKeys.isId)
  }
  public isEmbedded(type: any): boolean {
    return this.hasDirective(type, DirectiveKeys.isEmbedded)
  }
  protected isCreatedAtField(field: any): boolean {
    return this.hasDirective(field, DirectiveKeys.isCreatedAt) 
  }
  protected isUpdatedAtField(field: any): boolean {
    return this.hasDirective(field, DirectiveKeys.isUpdatedAt)
  }
}