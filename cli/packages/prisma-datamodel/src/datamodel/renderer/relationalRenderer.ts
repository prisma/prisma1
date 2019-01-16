import Renderer from './renderer'
import { idFieldName, createdAtFieldName, updatedAtFieldName } from "../parser/relationalParser"
import { ISDL, IGQLType, IDirectiveInfo, IGQLField } from '../model'
import GQLAssert from '../../util/gqlAssert'
/**
 * Renderer implementation for relational models. 
 */
export default class RelationalRenderer extends Renderer {

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

  protected shouldCreateIsUniqueFieldDirective(field: IGQLField) {
    return field.isUnique
  }

  protected renderType(type: IGQLType): string {
    if(type.isEmbedded) {
      GQLAssert.raise('Embedded types are not supported in relational models.')
    }

    return super.renderType(type)
  }

  protected renderField(field: IGQLField): string {
    if(field.isId && field.name !== idFieldName) {
      GQLAssert.raise(`ID musst be called "${idFieldName}" in relational models.`)
    }
    if(field.isCreatedAt && field.name !== createdAtFieldName) {
      GQLAssert.raise(`ID musst be called "${createdAtFieldName}" in relational models.`)
    }
    if(field.isUpdatedAt && field.name !== updatedAtFieldName) {
      GQLAssert.raise(`ID musst be called "${updatedAtFieldName}" in relational models.`)
    }

    return super.renderField(field)
  }
}