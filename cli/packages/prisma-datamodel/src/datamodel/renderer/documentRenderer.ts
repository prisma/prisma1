import Renderer from './renderer'
import { ISDL, IGQLType, IDirectiveInfo, IGQLField } from '../model'

/**
 * Renderer implementation for document models.
 */
export default class DocumentRenderer extends Renderer {
  // No explicit database name direcitve for mongo _id fields
  protected shouldCreateDatabaseNameFieldDirective(field: IGQLField) {
    return (
      super.shouldCreateDatabaseNameFieldDirective(field) &&
      !(field.isId && field.databaseName === '_id')
    )
  }
}
