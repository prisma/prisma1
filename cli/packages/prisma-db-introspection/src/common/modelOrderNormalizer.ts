import { singular } from 'pluralize'
import { IGQLType, IGQLField, ISDL, capitalize, plural, toposort } from 'prisma-datamodel'

export default class ModelOrderNormalizer {
  private baseModel: ISDL

  public constructor(baseModel: ISDL) {
    this.baseModel = baseModel
  }

  public normalize(model: ISDL) {
    model.types.sort((a, b) => this.orderComparer(this.baseModel.types, a, b))

    for(const type of model.types) {
      const baseType = this.baseModel.types.find(x => x.name === type.name)

      if(baseType === undefined)
        continue

      type.fields.sort((a, b) => this.orderComparer(baseType.fields, a, b))
    }
  }

  protected orderComparer<T extends IGQLField | IGQLType>(ref: T[], a: T, b: T): number {
    // Should we also compare for enum?
    const ia = ref.findIndex(x => x.name === a.name)
    const ib = ref.findIndex(x => x.name === b.name)

    // If both types or fields are present in the reference, 
    // compare by index. Else, compare by name.
    if(ia === -1 || ib === -1) {
      return a < b ? 1 : -1
    } else {
      return ia < ib ? 1 : -1
    }
  }
}