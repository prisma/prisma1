import {
  IGQLType,
  IGQLField,
  ISDL,
  TypeIdentifiers,
  IdStrategy,
} from 'prisma-datamodel'

import ModelNameNormalizer from './modelNameNormalizer'

export default class ModelNameAndDirectiveNormalizer extends ModelNameNormalizer {
  constructor(baseModel: ISDL | null) {
    super(baseModel)
  }

  private findAndSetBaseByRelation(obj: IGQLField) {
    if (this.baseFields === null || typeof obj.type === 'string') {
      return null
    }

    const fieldType = obj.type

    const [baseCandidate] = this.baseFields.filter(base => {
      return (
        typeof base.type !== 'string' &&
        base.type.name === fieldType.name &&
        // Only check the relation name if is it set on base. It might have been omitted.
        (base.relationName === null ||
          obj.relationName === null ||
          base.relationName === obj.relationName)
      )
    })

    if (baseCandidate !== undefined) {
      // Use each base field only once, see base class.
      this.baseField = baseCandidate
      this.baseFields = this.baseFields.filter(f => this.baseField !== f)
      return this.baseField
    } else {
      return null
    }
  }

  private findAndSetBaseById(obj: IGQLField) {
    if (this.baseFields === null) {
      return null
    }

    const [baseCandidate] = this.baseFields.filter(
      baseObj => baseObj.isId && obj.isId,
    )

    if (baseCandidate !== undefined) {
      // Use each base field only once, see base class.
      this.baseField = baseCandidate
      this.baseFields = this.baseFields.filter(f => this.baseField !== f)
      return this.baseField
    } else {
      return null
    }
  }

  private assignTypeProperties(baseObj: IGQLType | null, obj: IGQLType) {
    if (baseObj === null) {
      return
    }

    if (baseObj.databaseName) {
      obj.name = baseObj.name
      obj.databaseName = baseObj.databaseName
    }
  }

  /**
   * Assigns all properties from a base model field
   * to the current field, handling several special cases.
   * @param baseObj The base (reference) field.
   * @param obj The current field.
   * @param parentModel The whole model, for finding enums.
   */
  private assignFieldProperties(
    baseObj: IGQLField | null,
    obj: IGQLField,
    parentModel: ISDL,
  ) {
    if (baseObj === null) {
      return
    }

    obj.isId = obj.isId || baseObj.isId

    // In case the field is an ID field, we rename
    // even if there is no database name. This case
    // can happen with mongo, and prisma will be able to remap it.
    if (baseObj.databaseName || obj.isId) {
      obj.name = baseObj.name
      obj.databaseName = baseObj.databaseName
    }

    obj.isCreatedAt = obj.isCreatedAt || baseObj.isCreatedAt
    obj.isUpdatedAt = obj.isUpdatedAt || baseObj.isUpdatedAt
    obj.defaultValue = obj.defaultValue || baseObj.defaultValue
    obj.associatedSequence =
      obj.associatedSequence || baseObj.associatedSequence
    obj.relationName = obj.relationName || baseObj.relationName

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
    this.assignTypeProperties(this.baseType, type)
    super.normalizeType(type, parentModel, this.baseType !== null)
  }

  /**
   * Normalizes a field, handling several special cases.
   * @param baseObj The base (reference) field.
   * @param obj The current field.
   * @param parentModel The whole model, for finding enums.
   */
  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    if (this.baseField !== null) {
      // Base case, we identified a field by name.
      this.assignName(field, this.baseField.name)
      this.assignFieldProperties(this.baseField, field, parentModel)
    } else {
      // Fallback to finding a base field by ID.
      this.baseField = this.findAndSetBaseById(field)
      if (this.baseField !== null) {
        this.assignFieldProperties(this.baseField, field, parentModel)
      } else {
        // Fallback to finding a base field by relation type
        // Here, we have a few special cases when overwriting properties.
        this.baseField = this.findAndSetBaseByRelation(field)
        if (this.baseField !== null) {
          // Hard-override name. Relation names are usually auto-generated.
          field.name = this.baseField.name
          field.databaseName = this.baseField.databaseName
          if (
            this.baseField.relationName === null ||
            field.relationName === null
          ) {
            // Remove relation name if it is unset in ref model,
            // Set relation name if set on ref model but not for us.
            field.relationName = this.baseField.relationName
          }

          // If this is a self-referencing field with a back connection on the same type, we copy
          // the name of the related field as well. Otherwise, we always
          // end up overwriting our name with the name of the
          // first field of same type in the reference model.
          if (
            field.type == parentType &&
            this.baseField.relatedField !== null &&
            field.relatedField !== null
          ) {
            field.relatedField.name = this.baseField.relatedField.name
            field.relatedField.relationName = this.baseField.relatedField.relationName
          }
          this.assignFieldProperties(this.baseField, field, parentModel)
        } else {
          // If there is absolutely no base field,
          // Use our name normalization.
          super.normalizeField(field, parentType, parentModel)
        }
      }
    }
  }
}
