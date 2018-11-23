import { IGQLField, IGQLType, IComment } from 'prisma-datamodel'
import { Data } from './data'
import { TypeIdentifier, TypeIdentifiers } from '../../../../prisma-datamodel/dist/src/datamodel/scalar'
import { isArray, isRegExp } from 'util'

const ObjectTypeIdentifier = 'Object'
const InvalidTypeIdentifier = 'Object'

const MongoIdType = '_id'

const UnsupportedTypeErrorKey = 'UnsupportedType'

interface TypeInfo {
  type: TypeIdentifier | null | 'Object',
  isArray: boolean
}

interface Embedding {
  type: ModelMerger, 
  isArray: boolean
}

class UnsupportedTypeError extends Error {

  public invalidType: string

  constructor(public message: string, invalidType: string) {
    super(message);
    this.name = UnsupportedTypeErrorKey
    this.invalidType = invalidType
  }
}

interface FieldInfo {
  name: string,
  types: TypeIdentifier[]
  isArray: boolean[]
  invalidTypes: string[]
}
/**
 * Infers structure of a datatype from a set of data samples. 
 */
export class ModelMerger {
  private fields: { [fieldName: string]: FieldInfo }
  private embeddedTypes: { [typeName: string]: ModelMerger }

  public name: string
  public isEmbedded: boolean

  constructor(name: string, isEmbedded: boolean = false) {
    this.name = name
    this.isEmbedded = isEmbedded
    this.fields = {}
  }

  public analyze(data: Data) {
    for(const fieldName of Object.keys(data)) {
      this.analyzeField(fieldName, data[fieldName])
    }
  }

  public getType() : IGQLType {
    const fields = Object.keys(this.fields).map(key => this.toIGQLField(this.fields[key]))

    return {
      fields: fields,
      isEmbedded: this.isEmbedded,
      name: this.name,
      isEnum: false // No enum in mongo
    }
  }

  private toIGQLField(info: FieldInfo) {
    let type = '<Unknown>'
    let isArray = false
    let isRequired = false
    const comments: IComment[] = []

    if(info.isArray.length > 1) {
      comments.push({
        isError: true,
        text: 'Datatype inconsistency: Sometimes is array, and sometimes not.'
      })
    } else if(info.isArray.length === 1) {
      isArray = info.isArray[0]
    }

    let typeCandidates = [...info.types]

    // Our float/int decision is based soley on the data itself.
    // If we have at least one float, integer is always a misguess.
    if(typeCandidates.indexOf(TypeIdentifiers.float) >= 0) {
      typeCandidates = typeCandidates.filter(x => x !== TypeIdentifiers.integer)
    }

    if(typeCandidates.length > 1) {
      comments.push({
        isError: true,
        text: 'Datatype inconsistency. Conflicting types found: ' + typeCandidates.join(', ')
      })
    }

    if(typeCandidates.length === 1) {
      type = typeCandidates[0]
    } else {
      comments.push({
        isError: true,
        text: 'No type information found for field.'
      })
    }

    // TODO: we might want to include directives, as soon as we start changing names. 
    return {
      name: info.name,
      type: type,
      isId: name === MongoIdType,
      isList: isArray,
      isReadOnly: false,
      isRequired: isRequired,
      isUnique: false, // Never unique in Mongo. 
      relationName: null,
      relatedField: null,
      defaultValue: null,
      comments: comments
    } as IGQLField
  }

  public getEmbeddedTypes() : IGQLType[] {
    return Object.keys(this.embeddedTypes).map(key => this.embeddedTypes[key].getType())
  }

  private analyzeField(name: string, value: any) {
    this.fields[name] = {
      invalidTypes: [],
      isArray: [],
      name: name,
      types: []
    }
    try {
      const typeInfo = this.inferType(value)

      // Recursive embedding case. 
      if(typeInfo.type === ObjectTypeIdentifier) {
        this.embeddedTypes[name] = this.embeddedTypes[name] || new ModelMerger(name, true)
        this.embeddedTypes[name].analyze(value)
      } else {
        if(!this.fields[name]) {
          this.fields[name] = this.mergeField(this.fields[name], typeInfo)
        }
      }
    } catch(err) {
      if(err.name === UnsupportedTypeErrorKey) {
        this.fields[name].invalidTypes.push(err.invalidType)
      }
    }
  }

  private mergeField(field: FieldInfo, info: TypeInfo): FieldInfo {
    let types = field.types

    if(info.type === ObjectTypeIdentifier) {
      throw new Error('Cannot merge embedded document into field list: ' + field.name)
    }

    if(info.type !== null) {
      types = this.merge(field.types, info.type)
    }

    return {
      invalidTypes: field.invalidTypes,
      isArray: this.merge(field.isArray, info.isArray),
      name: field.name,
      types: types
    }
  }

  private merge<T>(target: Array<T>, value: T) {
    if(target.indexOf(value) < 0) {
      target.push(value)
    }
    return target
  }

  private inferType(value: any): TypeInfo  {
    // Base types
    switch(typeof(value)) {
      case "number": return { type: value % 1 === 0 ? TypeIdentifiers.integer : TypeIdentifiers.float, isArray: false }
      case "boolean": return { type: TypeIdentifiers.boolean, isArray: false }
      case "string": return { type: TypeIdentifiers.boolean, isArray: false }
      case "object": return { type: ObjectTypeIdentifier, isArray: false }
      default: break
    }

    // Maybe an array?
    if (Array.isArray(value)) {
      let arrayType: TypeInfo = { type: null, isArray: false }
      if(value.length > 0) {
        arrayType = this.inferType(value[0])
        if(arrayType.isArray) {
          throw new UnsupportedTypeError('Received a nested array while analyzing data. This is not supported yet.', 'ArrayArray')
        }
      }
      return { type: arrayType.type, isArray: true }
    }

    throw new UnsupportedTypeError('Received a nested array while analyzing data. This is not supported yet.', typeof value)
  }
}