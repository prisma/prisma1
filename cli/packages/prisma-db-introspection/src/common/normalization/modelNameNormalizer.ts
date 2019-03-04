import { singular } from 'pluralize'
import {
  IGQLType,
  IGQLField,
  ISDL,
  capitalize,
  plural,
  toposort,
  isTypeIdentifier,
} from 'prisma-datamodel'
import { INormalizer } from './normalizer'
import * as uppercamelcase from 'uppercamelcase'
import { groupBy, uniqBy } from 'lodash'

export default class ModelNameNormalizer implements INormalizer {
  public normalize(model: ISDL) {
    // We need to sort types according to topological order for name normalization.
    // Otherwise embedded type naming might break as embedded types depend on
    // their parent type.
    for (const type of toposort(model.types)) {
      this.normalizeType(type, model)
    }

    this.fixConflicts(model)
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

  protected getNormalizedName(name: string, model: ISDL) {
    if (name.toUpperCase() === name) {
      return name
    }

    const normalizedName = uppercamelcase(singular(name))

    // if there is a naming conflict with a known scalar type, use the default name
    if (isTypeIdentifier(normalizedName) || isTypeIdentifier(singular(name))) {
      return name
    }

    // if there is already a table in the database with the exact name we're generating - let's just not do it
    if (model.types.some(t => (t.databaseName || t.name) === normalizedName)) {
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
      this.assignName(type, this.getNormalizedName(type.name, model))
    }

    for (const field of type.fields) {
      this.normalizeField(field, type, model)
    }
  }

  protected fixConflicts(model: ISDL) {
    const groupedTypesByName = groupBy(model.types, t => t.name)

    for (const types of Object.values(groupedTypesByName)) {
      if (types.length > 1) {
        for (const type of types) {
          if (type.databaseName) {
            type.name = uppercamelcase(type.databaseName)
          }
        }

        const uniqueTypes = uniqBy(types, t => t.name)

        // if there still are duplicates, default to the database name
        if (uniqueTypes.length < types.length) {
          for (const type of types) {
            if (type.databaseName) {
              type.name = type.databaseName
              type.databaseName = null
            }
          }
        }
      }
    }
  }

  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    // Make embedded type names pretty
    if (typeof field.type !== 'string' && field.type.isEmbedded) {
      if (!field.type.databaseName) field.type.databaseName = field.type.name

      field.type.name = parentType.name + capitalize(singular(field.name))
    }
  }
}
