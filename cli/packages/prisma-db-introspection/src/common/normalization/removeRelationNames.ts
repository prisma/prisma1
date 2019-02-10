import { ISDL, IGQLField, IGQLType } from 'prisma-datamodel'
import { INormalizer } from './normalizer'

/**
 * We remove the relation name of type pairs which have exactly a single relation between them.
 * Except the relation name is explicitely declared in the base model. Then, we keep it.
 */
export class RemoveRelationName implements INormalizer {
  protected baseModel: ISDL | null

  constructor(baseModel: ISDL | null) {
    this.baseModel = baseModel
  }

  public normalizeType(type: IGQLType, ref: IGQLType | undefined) {
    for (const field of type.fields) {
      if (typeof field.type !== 'string' && field.relationName !== null) {
        // Hola, relation!
        const otherRelationOfSameTypeCount = type.fields.filter(x => x.type === field.type && x !== field).length
        const otherBackRelationCount = field.type.fields.filter(x => x.type == type && x !== field.relatedField).length

        if (otherRelationOfSameTypeCount > 0 || otherBackRelationCount > 0) continue // We need a relation name directive here

        if (ref !== undefined) {
          const refField = ref.fields.find(x => x.name === field.name)
          if (refField !== undefined && refField.relationName !== null) continue // Ref model has database name
        }

        // All checks passed, remove relation name
        if (field.relatedField !== null) field.relatedField.relationName = null
        field.relationName = null
      }
    }
  }

  public normalize(model: ISDL) {
    for (const type of model.types) {
      // TODO: We should move all tooling for finding types or fields into some common class.
      const ref =
        this.baseModel !== null
          ? this.baseModel.types.find(x => x.name === type.name || x.databaseName === type.name)
          : undefined
      this.normalizeType(type, ref)
    }
  }
}
