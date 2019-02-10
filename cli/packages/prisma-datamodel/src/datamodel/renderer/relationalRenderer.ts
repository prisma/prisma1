import Renderer from './renderer'
import { ISDL, IGQLType, IDirectiveInfo, IGQLField } from '../model'
import GQLAssert from '../../util/gqlAssert'
/**
 * Renderer implementation for relational models, model version 2
 * https://www.notion.so/Migrate-current-datamodel-to-v2-485aad4b77814af2831411a8d5f5abc1
 */
export default class RelationalRendererV2 extends Renderer {
  // The default behavior already equals to v2 for all directives.
  protected renderType(type: IGQLType): string {
    if (type.isEmbedded) {
      GQLAssert.raise('Embedded types are not supported in relational models.')
    }

    return super.renderType(type)
  }
}
