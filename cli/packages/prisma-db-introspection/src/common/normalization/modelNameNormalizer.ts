import { singular } from 'pluralize'
import {
  IGQLType,
  IGQLField,
  ISDL,
  capitalize,
  plural,
  toposort,
  isTypeIdentifier,
  camelCase,
} from 'prisma-datamodel'
import { INormalizer } from './normalizer'
import { groupBy, uniqBy } from 'lodash'

export default class ModelNameNormalizer implements INormalizer {
  public normalize(model: ISDL) {
    // We need to sort types according to topological order for name normalization.
    // Otherwise embedded type naming might break as embedded types depend on
    // their parent type.
    for (const type of toposort(model.types)) {
      this.normalizeType(type, model)
    }
  }

  protected assignName(obj: IGQLType | IGQLField, newName: string) {
    if (obj.databaseName === null) {
      // If name was already changed, we don't touch it.
      if (newName !== obj.name) {
        // If name is already conforming to prisma, skip.
        obj.databaseName = obj.name
        obj.name = newName
      }
    }
  }

  protected getNormalizedTypeName(name: string, model: ISDL) {
    if (name.toUpperCase() === name) {
      return name
    }

    const normalizedName = capitalize(camelCase(singular(name)))

    // if there is a naming conflict with a known scalar type, use the default name
    if (isTypeIdentifier(normalizedName) || isTypeIdentifier(singular(name))) {
      return name
    }

    // if there is already a table in the database with the exact name we're generating - let's just not do it
    if (model.types.some(t => t.name === normalizedName)) {
      return name
    }

    return normalizedName
  }

  protected normalizeType(
    type: IGQLType,
    model: ISDL,
    forceNoRename: boolean = false,
  ) {
    if (!forceNoRename) {
      this.assignName(type, this.getNormalizedTypeName(type.name, model))
    }

    for (const field of type.fields) {
      this.normalizeField(field, type, model)
    }
  }

  protected getNormalizedFieldName(
    name: string,
    parentType: IGQLType,
    field: IGQLField,
  ) {
    // For all-uppercase field names, we do not normalize.
    if (name.toUpperCase() === name) {
      return name.toLowerCase()
    }

    // Trim _id from related fields.
    if (typeof field.type !== 'string' && name.toLowerCase().endsWith('_id')) {
      name = name.substring(0, name.length - 3)
    }

    // Follow prisma conventions.
    const normalizedName = camelCase(name)

    // If there is already a field in this type for the normalized name, don't rename.
    if (parentType.fields.some(f => f.name === normalizedName && f !== field)) {
      return null
    }

    return normalizedName
  }

  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    // Make field names pretty
    if (!parentType.isEnum && !field.isId) {
      let normalizedName = this.getNormalizedFieldName(
        field.name,
        parentType,
        field,
      )
      if (normalizedName === null) {
        field.comments.push({
          isError: false,
          text:
            'Field name normalization failed because of a conflicting field name.',
        })
      } else {
        this.assignName(field, normalizedName)
      }
    }

    // Make embedded type names pretty
    if (typeof field.type !== 'string' && field.type.isEmbedded) {
      if (!field.type.databaseName) field.type.databaseName = field.type.name

      field.type.name = parentType.name + capitalize(singular(field.name))
    }
  }
}
