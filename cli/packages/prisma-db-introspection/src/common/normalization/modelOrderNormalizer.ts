import {
  IGQLType,
  IGQLField,
  ISDL,
  capitalize,
  plural,
  toposort,
} from 'prisma-datamodel'
import { INormalizer, Normalizer } from './normalizer'

export default class ModelOrderNormalizer extends Normalizer {
  public normalize(model: ISDL) {
    const baseTypes = this.baseModel !== null ? this.baseModel.types : []

    model.types = model.types.sort((a, b) =>
      this.typeOrderComparer(baseTypes, a, b),
    )

    super.normalize(model)
  }

  public normalizeType(type: IGQLType, model: ISDL) {
    // Take base ields into account, otherwise Alphabetically is fallback.
    const baseFields = this.baseType ? this.baseType.fields : []

    // Zip with index, sort, extract result.
    type.fields = type.fields
      .map((field, index) => [field, index] as [IGQLField, number])
      .sort((a, b) => this.fieldOrderComparer(baseFields, a, b))
      .map(tuple => tuple[0] as IGQLField)
  }

  protected typeOrderComparer(
    ref: IGQLType[],
    a: IGQLType,
    b: IGQLType,
  ): number {
    // Should we also compare for enum?
    const aIndexInRef = ref.findIndex(
      x => x.name === a.name || x.databaseName === a.name,
    )
    const bIndexInRef = ref.findIndex(
      x => x.name === b.name || x.databaseName === b.name,
    )

    // If both types or fields are present in the reference,
    // compare by index. Else, append to back and compare by name.
    if (aIndexInRef === -1 && bIndexInRef === -1) {
      return a.name > b.name ? 1 : -1
    } else if (aIndexInRef === -1) {
      return 1
    } else if (bIndexInRef === -1) {
      return -1
    } else {
      return aIndexInRef > bIndexInRef ? 1 : -1
    }
  }

  protected fieldOrderComparer(
    ref: IGQLField[],
    aTuple: [IGQLField, number],
    bTuple: [IGQLField, number],
  ): number {
    const [a, aIndex] = aTuple
    const [b, bIndex] = bTuple

    const aIndexInRef = ref.findIndex(
      x => x.name === a.name || x.databaseName === a.name,
    )
    const bIndexInRef = ref.findIndex(
      x => x.name === b.name || x.databaseName === b.name,
    )

    // If both types or fields are present in the reference,
    // compare by index. Else, append to back and compare by name.
    // Id's always get prepended, even if the ID field is new.
    if (aIndexInRef === -1 && a.isId) {
      return -1
    } else if (bIndexInRef === -1 && b.isId) {
      return 1
    } else if (aIndexInRef === -1 && bIndexInRef === -1) {
      const aName = a.name.toLowerCase()
      const bName = b.name.toLowerCase()
      if (aName === bName) {
        if (a.relatedField !== null && b.relatedField !== null) {
          // This is to avoid flaky tests. The sort algorithm is
          // not stable and equality would swap fields back and forth
          // randomly, which causes trouble with related fields.
          return a.relatedField.name.toLowerCase() <
            b.relatedField.name.toLowerCase()
            ? -1
            : 1
        } else {
          // If everything else seems equal,
          // we utilize the zipped index to force
          // stable ordering.
          return aIndex < bIndex ? -1 : 1
        }
      } else {
        return aName < bName ? -1 : 1
      }
    } else if (aIndexInRef === -1) {
      return 1
    } else if (bIndexInRef === -1) {
      return -1
    } else {
      return aIndexInRef < bIndexInRef ? -1 : 1
    }
  }

  // This is never called.
  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    throw new Error('Method not implemented.')
  }
}
