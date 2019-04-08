import { ISDL, IGQLField, IGQLType, capitalize } from 'prisma-datamodel'
import { INormalizer } from './normalizer'

export class NameAmbiguousBackRelation implements INormalizer {
  public normalizeType(type: IGQLType) {
    for (const field of type.fields) {
      if (
        // Make a check for all unnamed relations.
        typeof field.type !== 'string' &&
        field.relatedField !== null &&
        field.relationName === null
      ) {
        for (const other of type.fields) {
          if (
            // Any other relation makes our relation ambiguous
            // regardless of the other relation is named or not.
            field.type === other.type &&
            field.relatedField !== other &&
            field !== other
          ) {
            const relationName =
              `${type.name}${capitalize(field.name)}To` +
              `${field.type.name}${capitalize(field.relatedField.name)}`

            field.relationName = relationName
            field.relatedField.relationName = relationName
          }
        }
      }
    }
  }

  public normalize(model: ISDL) {
    for (const type of model.types) {
      this.normalizeType(type)
    }
  }
}
