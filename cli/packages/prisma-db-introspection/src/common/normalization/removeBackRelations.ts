import { ISDL, IGQLField, IGQLType } from 'prisma-datamodel'
import { INormalizer } from './normalizer'

export class RemoveBackRelation implements INormalizer {
  protected baseModel: ISDL

  constructor(baseModel: ISDL) {
    this.baseModel = baseModel
  }

  public normalizeType(type: IGQLType, ref: IGQLType) {
    // Here, we explicitely need a classic for loop,
    // as we need to restart it in the case of
    // self-referencing relations. Otherwise
    // we might end up removing too many relations.
    for (let i = 0; i < type.fields.length; i++) {
      const field = type.fields[i]
      if (typeof field.type !== 'string' && field.relatedField !== null) {
        // Fid the reference field.
        const refField = ref.fields.find(x => x.name === field.name)

        if (refField === undefined || typeof refField.type === 'string') continue

        if (refField.type.name !== field.type.name) continue

        // If the reference field has no related field  we drop it.
        if (refField.relatedField === null) {
          const relatedType = field.type as IGQLType
          relatedType.fields = relatedType.fields.filter(x => x !== field.relatedField)
          field.relatedField = null
          // Restart search in case we modified our own type.
          if (relatedType === type) {
            i = 0
          }
        }
      }
    }
  }

  public normalize(model: ISDL) {
    for (const type of model.types) {
      // TODO: We should move all tooling for finding types or fields into some common class.
      const ref = this.baseModel.types.find(x => x.name === type.name || x.databaseName === type.name)
      if (ref !== undefined) {
        this.normalizeType(type, ref)
      }
    }
  }
}
