import { IGQLType, IGQLField, ISDL, TypeIdentifiers } from 'prisma-datamodel'

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

  private findBaseByRelation(baseObjs: Array<IGQLField>, obj: IGQLField) {
    if(typeof obj.type === 'string') {
      return null
    }

    const fieldType = obj.type

    const [baseCandidate] = baseObjs.filter(base => {
      return typeof base.type !== 'string' &&
             base.type.name === fieldType.name &&
             base.relationName === obj.relationName
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

  private assignFieldProperties(baseObj: IGQLField | null, obj: IGQLField, parentModel: ISDL) {
    if(baseObj === null) {
      return
    }
    this.assignProperties(baseObj, obj)

    obj.isId = obj.isId || baseObj.isId
    obj.isCreatedAt = obj.isCreatedAt || baseObj.isCreatedAt
    obj.isUpdatedAt = obj.isUpdatedAt || baseObj.isUpdatedAt
    obj.defaultValue = obj.defaultValue || baseObj.defaultValue

    // We found an enum type field shadowed by prisma.
    if(obj.type === TypeIdentifiers.string && typeof baseObj.type !== 'string' && baseObj.type.isEnum) {
      const baseEnumType = baseObj.type
      // Attempt to find the enum type
      const candidateEnum = parentModel.types.find(x => x.isEnum && x.name === baseEnumType.name)

      if(candidateEnum !== undefined) {
        obj.type = candidateEnum
      }
    }
  }

  protected normalizeType(type: IGQLType, parentModel: ISDL) {
    if(this.baseModel === null) {
      this.baseType === null
    } else {
      this.baseType = this.findBaseByName(this.baseModel.types, type)
    }
    this.assignTypeProperties(this.baseType, type)
    super.normalizeType(type, parentModel, this.baseType !== null)
  }

  protected normalizeField(field: IGQLField, parentType: IGQLType, parentModel: ISDL) {
    let baseField: IGQLField | null = null
    if(this.baseType !== null) {
      baseField = this.findBaseByName(this.baseType.fields, field)

      if(baseField !== null) {
        this.assignName(field, baseField.name)
        this.assignFieldProperties(baseField, field, parentModel)
      } else {
        // Fallback to ID.
        baseField = this.findBaseById(this.baseType.fields, field)
        
        if(baseField !== null) {
          this.assignFieldProperties(baseField, field, parentModel)
        }
        else {
          // Fallback to relation.
          baseField =  this.findBaseByRelation(this.baseType.fields, field)
          if(baseField !== null) {
            // Hard-override name.
            field.name = baseField.name
            field.databaseName = baseField.databaseName 
            this.assignFieldProperties(baseField, field, parentModel)
          }
        }
      } 
      

      if(baseField !== null) {
        this.assignName(field, baseField.name)
        this.assignFieldProperties(baseField, field, parentModel)
      }
    }
    super.normalizeField(field, parentType, parentModel)
  }
}