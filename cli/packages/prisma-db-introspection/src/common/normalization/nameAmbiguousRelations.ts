import { ISDL, IGQLField, IGQLType, capitalize } from 'prisma-datamodel'
import { INormalizer, Normalizer } from './normalizer'

export class NameAmbiguousBackRelation extends Normalizer {
  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    if (
      // Make a check for all unnamed relations.
      typeof field.type !== 'string' &&
      field.relatedField !== null &&
      field.relationName === null
    ) {
      for (const other of parentType.fields) {
        if (
          // Any other relation makes our relation ambiguous
          // regardless of the other relation is named or not.
          field.type === other.type &&
          field.relatedField !== other &&
          field !== other
        ) {
          const relationName =
            `${parentType.name}${capitalize(field.name)}To` +
            `${field.type.name}${capitalize(field.relatedField.name)}`

          field.relationName = relationName
          field.relatedField.relationName = relationName
        }
      }
    }
  }
}
