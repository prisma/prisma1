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

  protected getNormalizedFieldName(name: string, type: IGQLType) {
    if (name.toUpperCase() === name) {
      return name.toLowerCase()
    }

    const normalizedName = camelCase(name)

    // if there is already a field of in this type for the normalized name, don't rename.
    if (type.fields.some(t => t.name === normalizedName)) {
      return name
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
      const normalizedName = this.getNormalizedFieldName(field.name, parentType)
      this.assignName(field, normalizedName)
    }

    // Make embedded type names pretty
    if (typeof field.type !== 'string' && field.type.isEmbedded) {
      if (!field.type.databaseName) field.type.databaseName = field.type.name

      field.type.name = parentType.name + capitalize(singular(field.name))
    }
  }
}
