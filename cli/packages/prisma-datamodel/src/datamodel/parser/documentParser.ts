import Parser from './parser'
import { DirectiveKeys } from '../directives'
/**
 * Parser implementation for document models. 
 */
export default class DocumentParser extends Parser {
  public isIdField(field: any): boolean {
    return this.hasDirective(field, DirectiveKeys.isId)
  }
  public isReadOnly(field: any): boolean {
    return this.hasDirective(field, DirectiveKeys.isId) ||
    this.hasDirective(field, DirectiveKeys.isCreatedAt) ||
    this.hasDirective(field, DirectiveKeys.isUpdatedAt)
  }
  public isEmbedded(type: any): boolean {
    return this.hasDirective(type, DirectiveKeys.isEmbedded)
  }

}