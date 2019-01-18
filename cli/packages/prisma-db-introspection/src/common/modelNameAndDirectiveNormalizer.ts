import { IGQLType, IGQLField, ISDL } from 'prisma-datamodel'

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
 
  private findBaseByName<T extends IGQLField | IGQLType>(baseObjs: Array<T>, obj: T) {
    const [baseCandidate] = baseObjs.filter(base => {
      if(base.databaseName) {
        return base.databaseName === obj.name
      } else {
        return base.name === obj.name
      }
    })
        
    return baseCandidate || null
  }


  private findBaseById(baseObjs: IGQLField[], obj: IGQLField) {
    const [baseCandidate] = baseObjs.filter(baseObj => baseObj.isId && obj.isId)
        
    return baseCandidate || null
  }


  private assignProperties<T extends IGQLField | IGQLType>(baseObj: T, obj: T) {
    if(baseObj.databaseName) {
      obj.name = baseObj.name
      obj.databaseName = baseObj.databaseName
    }
  }

  private assignTypeProperties(baseObj: IGQLType | null, obj: IGQLType) {
    if(baseObj === null) {
      return
    }
    this.assignProperties(baseObj, obj)
  }

  private assignFieldProperties(baseObj: IGQLField | null, obj: IGQLField) {
    if(baseObj === null) {
      return
    }
    this.assignProperties(baseObj, obj)

    obj.isId = obj.isId || baseObj.isId
    obj.isCreatedAt = obj.isCreatedAt || baseObj.isCreatedAt
    obj.isUpdatedAt = obj.isUpdatedAt || baseObj.isUpdatedAt
  }

  protected normalizeType(type: IGQLType) {
    this.baseType = this.baseModel === null ? null : this.findBaseByName(this.baseModel.types, type)
    this.assignTypeProperties(this.baseType, type)
    super.normalizeType(type)
  }

  protected normalizeField(field: IGQLField, parentType: IGQLType) {
    let baseField: IGQLField | null = null
    if(this.baseType !== null) {
      baseField = this.findBaseByName(this.baseType.fields, field)
      if(baseField === null) {
        // Fallback - if no name match, we check if the field is an ID.
        baseField = this.findBaseById(this.baseType.fields, field)
        // Special handling for ID fields. We just assign the name.
        if(baseField !== null) {
          this.assignName(field, baseField.name)
        }
      }
    }

    this.assignFieldProperties(baseField, field)
    super.normalizeField(field, parentType)
  }
}