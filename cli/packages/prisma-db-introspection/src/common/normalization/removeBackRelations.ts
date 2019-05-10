import { ISDL, IGQLField, IGQLType } from 'prisma-datamodel'
import { INormalizer, Normalizer } from './normalizer'

export class RemoveBackRelation extends Normalizer {
  public normalizeType(type: IGQLType, parentModel: ISDL) {
    // Here, we explicitely need a classic for loop,
    // as we need to restart it in the case of
    // self-referencing relations. Otherwise
    // we might end up removing too many relations.
    for (let i = 0; i < type.fields.length; i++) {
      const field = type.fields[i]
      if (
        typeof field.type !== 'string' &&
        field.relatedField !== null &&
        this.baseType !== null
      ) {
        this.baseField =
          this.baseType.fields.find(f => f.name === field.name) || null

        if (this.baseField === null || typeof this.baseField.type === 'string')
          continue

        if (this.baseField.type.name !== field.type.name) continue

        // If the reference field has no related field we drop the current field.
        if (this.baseField.relatedField === null) {
          const relatedType = field.type as IGQLType
          relatedType.fields = relatedType.fields.filter(
            x => x !== field.relatedField,
          )
          field.relatedField = null
          // Restart search in case we modified our own type.
          if (relatedType === type) {
            i = 0
          }
        }
      }
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
