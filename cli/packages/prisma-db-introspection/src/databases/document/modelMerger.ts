import { IGQLField, IGQLType, IComment, TypeIdentifier, TypeIdentifiers, capitalize } from 'prisma-datamodel'
import { Data } from './data'
import { isArray, isRegExp } from 'util'
import { ObjectID } from 'bson';

const ObjectTypeIdentifier = 'EmbeddedObject'

const MongoIdName = '_id'

const UnsupportedTypeErrorKey = 'UnsupportedType'
const UnsupportedArrayTypeErrorKey = 'UnsupportedArrayType'

type InternalType = TypeIdentifier | 'EmbeddedObject'


interface TypeInfo {
  type: InternalType | null,
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

class UnsupportedArrayTypeError extends Error {
  public invalidType: string

  constructor(public message: string, invalidType: string) {
    super(message);
    this.name = UnsupportedArrayTypeErrorKey
    this.invalidType = invalidType
  }
}

interface FieldInfo {
  name: string,
  types: InternalType[]
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

  public static ErrorType = '<Unknown>'

  constructor(name: string, isEmbedded: boolean = false) {
    this.name = name
    this.isEmbedded = isEmbedded
    this.fields = {}
    this.embeddedTypes = {}
  }

  public analyze(data: Data) {
    for(const fieldName of Object.keys(data)) {
      this.analyzeField(fieldName, data[fieldName])
    }
  }

  /**
   * Gets the top level type only.
   */
  public getTopLevelType() : IGQLType {
    const fields = Object.keys(this.fields).map(key => this.toIGQLField(this.fields[key]))

    return {
      fields: fields,
      isEmbedded: this.isEmbedded,
      name: this.name,
      isEnum: false // No enum in mongo
    }
  }

  /**
   * Gets the type with embedded types attached recursivley.
   */
  public getType() : { type: IGQLType, embedded: IGQLType[]} {

    const type = this.getTopLevelType()
    const allEmbedded: IGQLType[] = []

    for(const embeddedFieldName of Object.keys(this.embeddedTypes)) {
      const embedded = this.embeddedTypes[embeddedFieldName].getType()
      allEmbedded.push(embedded.type, ...embedded.embedded)

      for(const field of type.fields) {
        if(field.name === embeddedFieldName) {
          field.type = embedded.type
        }
      }
    }

    return { type: type, embedded: allEmbedded }
  }

  // TODO: If we get more types to summarize, we should have all summarization code (e.g. for arrays) 
  // in one place.
  private summarizeTypeList(typeCandidates: InternalType[]) {
    // Our float/int decision is based soley on the data itself.
    // If we have at least one float, integer is always a misguess.
    if(typeCandidates.indexOf(TypeIdentifiers.float) >= 0) {
      typeCandidates = typeCandidates.filter(x => x !== TypeIdentifiers.integer)
    }

    return typeCandidates
  }

  private toIGQLField(info: FieldInfo) {
    let type = ModelMerger.ErrorType
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

    let typeCandidates = this.summarizeTypeList([...info.types])

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

    // https://www.prisma.io/docs/releases-and-maintenance/releases-and-beta-access/mongodb-preview-b6o5/#directives
    // TODO: we might want to include directives, as soon as we start changing names. 
    return {
      name: info.name,
      type: type,
      isId: info.name === MongoIdName,
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

  private initField(name: string) {
    this.fields[name] = this.fields[name] || {
      invalidTypes: [],
      isArray: [],
      name: name,
      types: []
    }
  }

  private analyzeField(name: string, value: any) {
   
    try {
      const typeInfo = this.inferType(value)

      // Recursive embedding case. 
      if(typeInfo.type === ObjectTypeIdentifier) {
        this.embeddedTypes[name] = this.embeddedTypes[name] || new ModelMerger(capitalize(name), true)
        this.embeddedTypes[name].analyze(value)
      } 

      this.initField(name)
      this.fields[name] = this.mergeField(this.fields[name], typeInfo)
  
    } catch(err) {
      if(err.name === UnsupportedTypeErrorKey) {
        this.initField(name)
        this.fields[name].invalidTypes.push(err.invalidType)
      } else if(err.name == UnsupportedArrayTypeErrorKey) {
        this.initField(name)
        this.fields[name] = this.mergeField(this.fields[name], {
          isArray: true,
          type: null
        })
        this.fields[name].invalidTypes.push(err.invalidType)
      } else {
        throw err
      }
    }
  }

  private mergeField(field: FieldInfo, info: TypeInfo): FieldInfo {
    let types = field.types

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

  private inferArrayType(array: any[]) {
    var type: InternalType | null = null
    
    for(const item of array) {
      const itemType = this.inferType(item)

      if(itemType.isArray) {
        throw new UnsupportedArrayTypeError('Received a nested array while analyzing data. This is not supported yet.', 'ArrayArray')
      }

      if(type === null) {
        type = itemType.type
      } else if(type === TypeIdentifiers.integer && itemType.type === TypeIdentifiers.float ||
                type === TypeIdentifiers.float && itemType.type === TypeIdentifiers.integer) {
        // Special case: We treat int as a case of float, if needed.
        type = TypeIdentifiers.float
      } else if(type !== itemType.type) {
        throw new UnsupportedArrayTypeError('Mixed arrays are not supported.', `Array of ${type} and ${itemType.type}`)
      }
    }

    return type
  }

  // TODO: There are more BSON types exported by the mongo client lib we could add here.
  private inferType(value: any): TypeInfo  {
    // Maybe an array, which would otherwise be identified as object.
    if (Array.isArray(value)) {
      let arrayType: TypeInfo = { type: this.inferArrayType(value), isArray: false }
      return { type: arrayType.type, isArray: true }
    }

    // Base types
    switch(typeof(value)) {
      case "number": return { type: value % 1 === 0 ? TypeIdentifiers.integer : TypeIdentifiers.float, isArray: false }
      case "boolean": return { type: TypeIdentifiers.boolean, isArray: false }
      case "string": return { type: TypeIdentifiers.string, isArray: false }
      case "object": 
        if(value instanceof ObjectID) {
          return { type: TypeIdentifiers.string, isArray: false }
        } else {
          return { type: ObjectTypeIdentifier, isArray: false }
        }
      default: break
    }

    throw new UnsupportedTypeError('Received an unsupported type:', typeof value)
  }
}