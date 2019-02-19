import {
  IGQLType,
  IGQLField,
  ISDL,
  capitalize,
  plural,
  toposort,
} from 'prisma-datamodel'
import { INormalizer } from './normalizer'

export default class ModelOrderNormalizer implements INormalizer {
  private baseModel: ISDL | null

  public constructor(baseModel: ISDL | null) {
    this.baseModel = baseModel
  }

  public normalize(model: ISDL): ISDL {
    const baseTypes = this.baseModel !== null ? this.baseModel.types : []

    model.types = model.types.sort((a, b) =>
      this.typeOrderComparer(baseTypes, a, b),
    )

    for (const type of model.types) {
      const baseType =
        this.baseModel === null
          ? undefined
          : this.baseModel.types.find(x => x.name === type.name)

      if (baseType === undefined) {
        // Alphabetically is fallback.
        type.fields = type.fields.sort((a, b) =>
          this.fieldOrderComparer([], a, b),
        )
      } else {
        type.fields = type.fields.sort((a, b) =>
          this.fieldOrderComparer(baseType.fields, a, b),
        )
      }
    }

    return model
  }

  protected typeOrderComparer(
    ref: IGQLType[],
    a: IGQLType,
    b: IGQLType,
  ): number {
    // Should we also compare for enum?
    const ia = ref.findIndex(
      x => x.name === a.name || x.databaseName === a.name,
    )
    const ib = ref.findIndex(
      x => x.name === b.name || x.databaseName === b.name,
    )

    // If both types or fields are present in the reference,
    // compare by index. Else, append to back and compare by name.
    if (ia === -1 && ib === -1) {
      return a.name > b.name ? 1 : -1
    } else if (ia === -1) {
      return 1
    } else if (ib === -1) {
      return -1
    } else {
      return ia > ib ? 1 : -1
    }
  }

  protected fieldOrderComparer(
    ref: IGQLField[],
    a: IGQLField,
    b: IGQLField,
  ): number {
    const ia = ref.findIndex(
      x => x.name === a.name || x.databaseName === a.name,
    )
    const ib = ref.findIndex(
      x => x.name === b.name || x.databaseName === b.name,
    )

    // If both types or fields are present in the reference,
    // compare by index. Else, append to back and compare by name.
    // Id's always get prepended, if the ID field is new.
    if (ia === -1 && a.isId) {
      return -1
    } else if (ib === -1 && b.isId) {
      return 1
    } else if (ia === -1 && ib === -1) {
      return a.name.toLowerCase() < b.name.toLowerCase() ? -1 : 1
    } else if (ia === -1) {
      return 1
    } else if (ib === -1) {
      return -1
    } else {
      return ia < ib ? -1 : 1
    }
  }
}
