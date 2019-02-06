import { RelationalRendererV2, IGQLField } from 'prisma-datamodel'

export class TmpRelationalRendererV2 extends RelationalRendererV2 {
  shouldCreateCreatedAtFieldDirective(field: IGQLField) {
    return field.name === 'createdAt'
  }
  shouldCreateUpdatedAtFieldDirective(field: IGQLField) {
    return field.name === 'updatedAt'
  }
}
