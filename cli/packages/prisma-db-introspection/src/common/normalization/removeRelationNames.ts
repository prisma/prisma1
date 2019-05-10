import { ISDL, IGQLField, IGQLType } from 'prisma-datamodel'
import { INormalizer, Normalizer } from './normalizer'

/**
 * We remove the relation name of type pairs which have exactly a single relation between them.
 * Except the relation name is explicitely declared in the base model. Then, we keep it.
 */
export class RemoveRelationName extends Normalizer {
  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    if (typeof field.type !== 'string' && field.relationName !== null) {
      // Hola, relation!
      const otherRelationOfSameTypeCount = parentType.fields.filter(
        x => x.type === field.type && x !== field,
      ).length
      const otherBackRelationCount = field.type.fields.filter(
        x => x.type == parentType && x !== field.relatedField,
      ).length

      // We need a relation name directive in this case
      if (otherRelationOfSameTypeCount > 0 || otherBackRelationCount > 0) return

      // Ref model has database name, don't remove
      if (this.baseField !== null && this.baseField.relationName !== null)
        return

      // All checks passed, remove relation name
      if (field.relatedField !== null) field.relatedField.relationName = null
      field.relationName = null
    }
  }
}
