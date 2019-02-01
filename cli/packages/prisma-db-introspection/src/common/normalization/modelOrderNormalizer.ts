import { IGQLType, IGQLField, ISDL, capitalize, plural, toposort } from 'prisma-datamodel'
import { INormalizer } from './normalizer';

export default class ModelOrderNormalizer implements INormalizer {
  private baseModel: ISDL

  public constructor(baseModel: ISDL) {
    this.baseModel = baseModel
  }

  public normalize(model: ISDL): ISDL {
    model.types = model.types.sort((a, b) => this.orderComparer(this.baseModel.types, a, b))

    for(const type of model.types) {
      const baseType = this.baseModel.types.find(x => x.name === type.name)

      if(baseType === undefined) {
        // Alphabetically is fallback.
        type.fields = type.fields.sort((a, b) => this.orderComparer([], a, b))
      } else {
        type.fields = type.fields.sort((a, b) => this.orderComparer(baseType.fields, a, b))
      }
    }

    return model
  }

  protected orderComparer<T extends IGQLField | IGQLType>(ref: T[], a: T, b: T): number {
    // Should we also compare for enum?
    const ia = ref.findIndex(x => x.name === a.name || x.databaseName === a.name)
    const ib = ref.findIndex(x => x.name === b.name || x.databaseName === b.name)

    // If both types or fields are present in the reference, 
    // compare by index. Else, append to back and compare by name.
    if(ia === -1 && ib === -1) {
      return a.name > b.name ? 1 : -1
    } else if(ia === -1) {
      return 1
    } else if (ib === -1) {
      return -1
    } else {
      return ia > ib ? 1 : -1
    }
  }
}