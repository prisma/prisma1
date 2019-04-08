import { ISDL, IGQLField, IGQLType } from 'prisma-datamodel'
import { INormalizer, Normalizer } from './normalizer'

export class RemoveDuplicateBackRelation extends Normalizer {
  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    if (
      typeof field.type !== 'string' &&
      field.relatedField !== null &&
      !field.comments.some(c => c.isError)
    ) {
      for (const other of parentType.fields) {
        if (
          typeof other.type !== 'string' &&
          other.relatedField !== null &&
          !other.comments.some(c => c.isError) &&
          field !== other &&
          field.name === other.name
        ) {
          // We found a related field with a conflicting name
          // Mark both as error'd.
          other.comments.push({
            isError: true,
            text: `Could not auto-generate backwards relation field, field name would be ambiguous.`,
          })

          // TODO: Might want to intro multi-line comments.
          other.comments.push({
            isError: true,
            text: `Please specify the name of this field and the name of the relation manually.`,
          })

          other.comments.push({
            isError: true,
            text: `It references ${other.type.name}.${
              other.relatedField.name
            }.`,
          })

          field.comments.push({
            isError: true,
            text: `Could not auto-generate backwards relation field, field name would be ambiguous.`,
          })

          field.comments.push({
            isError: true,
            text: `Please specify the name of this field and the name of the relation manually.`,
          })

          field.comments.push({
            isError: true,
            text: `It references ${field.type.name}.${
              field.relatedField.name
            }.`,
          })

          // Do not mark a field as error'd multiple times.
          break
        }
      }
    }
  }
}
