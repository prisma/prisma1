import {
  IGQLType,
  IGQLField,
  ISDL,
  TypeIdentifiers,
  IdStrategy,
  cloneType,
} from 'prisma-datamodel'

import ModelNameNormalizer from './modelNameNormalizer'

export default class ModelNameAndDirectiveNormalizer extends ModelNameNormalizer {
  private baseModel: ISDL | null
  private baseType: IGQLType | null

  constructor(baseModel: ISDL | null) {
    super()
    this.baseModel = baseModel
    this.baseType = null
  }

  // https://github.com/prisma/prisma/issues/3725
  public normalize(model: ISDL) {
    super.normalize(model)
  }

  private findBaseByName<T extends IGQLField | IGQLType>(
    baseObjs: Array<T>,
    obj: T,
  ) {
    const [baseCandidate] = baseObjs.filter(base => {
      if (base.databaseName) {
        return base.databaseName === obj.name
      } else {
        return base.name === obj.name
      }
    })

    return baseCandidate || null
  }

  private findBaseByRelation(baseObjs: Array<IGQLField>, obj: IGQLField) {
    if (typeof obj.type === 'string') {
      return null
    }

    const fieldType = obj.type

    const [baseCandidate] = baseObjs.filter(base => {
      return (
        typeof base.type !== 'string' &&
        base.type.name === fieldType.name &&
        // Only check the relation name if is it set on base. It might have been omitted.
        (base.relationName === null ||
          obj.relationName === null ||
          base.relationName === obj.relationName)
      )
    })

    return baseCandidate || null
  }

  private findBaseById(baseObjs: IGQLField[], obj: IGQLField) {
    const [baseCandidate] = baseObjs.filter(baseObj => baseObj.isId && obj.isId)

    return baseCandidate || null
  }

  private assignProperties<T extends IGQLField | IGQLType>(baseObj: T, obj: T) {
    if (baseObj.databaseName) {
      obj.name = baseObj.name
      obj.databaseName = baseObj.databaseName
    }
  }

  private assignTypeProperties(baseObj: IGQLType | null, obj: IGQLType) {
    if (baseObj === null) {
      return
    }
    this.assignProperties(baseObj, obj)
  }

  private assignFieldProperties(
    baseObj: IGQLField | null,
    obj: IGQLField,
    parentModel: ISDL,
  ) {
    if (baseObj === null) {
      return
    }
    this.assignProperties(baseObj, obj)

    obj.isId = obj.isId || baseObj.isId
    obj.isCreatedAt = obj.isCreatedAt || baseObj.isCreatedAt
    obj.isUpdatedAt = obj.isUpdatedAt || baseObj.isUpdatedAt
    obj.defaultValue = obj.defaultValue || baseObj.defaultValue
    obj.associatedSequence =
      obj.associatedSequence || baseObj.associatedSequence

    // Overwrite strategy, if database has none, prisma might have some.
    if (obj.idStrategy === IdStrategy.None) {
      obj.idStrategy = baseObj.idStrategy
    }

    if (baseObj.associatedSequence) {
      obj.associatedSequence = { ...baseObj.associatedSequence }
    }

    // Override JSON mapped to string.
    if (
      obj.type === TypeIdentifiers.string &&
      baseObj.type === TypeIdentifiers.json
    ) {
      obj.type = TypeIdentifiers.json
    }

    if (
      obj.type === TypeIdentifiers.string &&
      typeof baseObj.type !== 'string' &&
      baseObj.type.isEnum
    ) {
      // We found an enum type field shadowed by prisma.
      const baseEnumType = baseObj.type
      // Attempt to find the enum type
      const candidateEnum = parentModel.types.find(
        x => x.isEnum && x.name === baseEnumType.name,
      )

      if (candidateEnum !== undefined) {
        obj.type = candidateEnum
      }
    }
  }

  protected normalizeType(type: IGQLType, parentModel: ISDL) {
    if (this.baseModel === null) {
      this.baseType === null
    } else {
      this.baseType = this.findBaseByName(this.baseModel.types, type)
      // We mutate the base type in the normalizeType call,
      // therefore we need to clone here.
      if (this.baseType !== null) {
        this.baseType = cloneType(this.baseType)
      }
    }
    this.assignTypeProperties(this.baseType, type)
    super.normalizeType(type, parentModel, this.baseType !== null)
  }

  // TODO: This method could use some refactoring.
  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    let baseField: IGQLField | null = null
    if (this.baseType !== null) {
      baseField = this.findBaseByName(this.baseType.fields, field)

      if (baseField !== null) {
        // Only use this base field once.
        this.baseType.fields = this.baseType.fields.filter(f => f !== baseField)
        this.assignName(field, baseField.name)
        this.assignFieldProperties(baseField, field, parentModel)
      } else {
        // Fallback to ID.
        baseField = this.findBaseById(this.baseType.fields, field)

        if (baseField !== null) {
          // Only use this base field once.
          this.baseType.fields = this.baseType.fields.filter(
            f => f !== baseField,
          )
          this.assignFieldProperties(baseField, field, parentModel)
        } else {
          // Fallback to relation.
          baseField = this.findBaseByRelation(this.baseType.fields, field)
          if (baseField !== null) {
            // Only use this base field once.
            this.baseType.fields = this.baseType.fields.filter(
              f => f !== baseField,
            )

            // Hard-override name. Relation names are usually auto-generated.
            field.name = baseField.name
            field.databaseName = baseField.databaseName
            if (
              baseField.relationName === null ||
              field.relationName === null
            ) {
              // Remove relation name if it is unset in ref model,
              // Set relation name if set on ref model but not for us.
              field.relationName = baseField.relationName
            }

            // If this is a self-referencing field with a back connection on the same type, we copy
            // the name of the related field as well. Otherwise, we always
            // end up overwriting or name with the first ocurrence in the reference.
            if (
              field.type == parentType &&
              baseField.relatedField !== null &&
              field.relatedField !== null
            ) {
              field.relatedField.name = baseField.relatedField.name
              field.relatedField.relationName =
                baseField.relatedField.relationName
            }
            this.assignFieldProperties(baseField, field, parentModel)
          }
        }
      }

      if (baseField !== null) {
        this.assignName(field, baseField.name)
        this.assignFieldProperties(baseField, field, parentModel)
      }
    }
    super.normalizeField(field, parentType, parentModel)
  }
}
