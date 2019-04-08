import { ISDL, IGQLType, IGQLField } from 'prisma-datamodel'

export interface INormalizer {
  normalize(model: ISDL)
}

export abstract class Normalizer {
  protected baseModel: ISDL | null
  protected baseType: IGQLType | null
  protected baseFields: IGQLField[] | null
  protected baseField: IGQLField | null

  public constructor(baseModel: ISDL | null) {
    this.baseModel = baseModel || null
    this.baseType = null
    this.baseFields = null
    this.baseField = null
  }

  public normalize(model: ISDL) {
    this.normalizeTypes(model.types, model)
  }

  protected normalizeTypes(types: IGQLType[], model: ISDL) {
    for (const type of types) {
      this.findAndSetBaseType(type)
      this.normalizeType(type, model)
    }
  }

  protected normalizeType(type: IGQLType, parentModel: ISDL) {
    this.normalizeFields(type.fields, type, parentModel)
  }

  protected normalizeFields(
    fields: IGQLField[],
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    for (const field of fields) {
      this.findAndSetBaseField(field)
      this.normalizeField(field, parentType, parentModel)
    }
  }

  protected abstract normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  )

  protected findAndSetBaseType(type: IGQLType) {
    if (this.baseModel === null) {
      this.baseType === null
      this.baseFields = null
    } else {
      this.baseType = this.findBaseByName(this.baseModel.types, type)
      if (this.baseType !== null) {
        this.baseFields = [...this.baseType.fields]
      }
    }
  }

  protected findAndSetBaseField(field: IGQLField) {
    if (this.baseFields === null) {
      this.baseField === null
    } else {
      this.baseField = this.findBaseByName(this.baseFields, field)
      // Only use each base field once, otherwise
      // this code would not work with duplicate relations.
      this.baseFields = this.baseFields.filter(f => this.baseField !== f)
    }
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
}
