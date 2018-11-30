import { IGQLField, IGQLType, IComment, TypeIdentifier, TypeIdentifiers, capitalize } from 'prisma-datamodel'
import { Data } from './data'
import { isArray, isRegExp } from 'util'
import { ObjectID } from 'bson' // TODO - remove dependency to mongo's BSON and subclass to abstract primitive type inferrer. 
import { SamplingStrategy, IDocumentConnector, IDataTypeInferrer, InternalType, ObjectTypeIdentifier, UnsupportedTypeErrorKey, UnsupportedArrayTypeErrorKey, TypeInfo } from './documentConnector'

const MongoIdName = '_id'

interface Embedding {
  type: ModelMerger, 
  isArray: boolean
}

interface FieldInfo {
  name: string,
  types: InternalType[]
  isArray: boolean[]
  isRelationCandidate: boolean
  invalidTypes: string[]
}


export class ModelSampler<InternalCollectionType> implements ModelSampler<InternalCollectionType> {
  private samplingStrategy: SamplingStrategy

  public static ErrorType = '<Unknown>'

  constructor(samplingStrategy: SamplingStrategy = SamplingStrategy.One) {
    this.samplingStrategy = samplingStrategy
  }

  public async sample(connector: IDocumentConnector<InternalCollectionType>, schemaName: string, primitiveResolver: IDataTypeInferrer) {
    let types: IGQLType[] = []

    const allCollections = await connector.getInternalCollections(schemaName)
    for (const { name, collection } of allCollections) {
      const merger = new ModelMerger(name, false, primitiveResolver)
      const iterator = await connector.sample(collection, this.samplingStrategy)
      while(await iterator.hasNext()) {
        const item = await iterator.next()
        merger.analyze(item)
      }
      await iterator.close()
      const mergeResult = merger.getType()
      types.push(mergeResult.type, ...mergeResult.embedded)
    }
    return types
  }
}

/**
 * Infers structure of a datatype from a set of data samples. 
 */
export class ModelMerger {
  private fields: { [fieldName: string]: FieldInfo }
  private embeddedTypes: { [typeName: string]: ModelMerger }

  public name: string
  public isEmbedded: boolean
  public primitiveResolver: IDataTypeInferrer

  constructor(name: string, isEmbedded: boolean, primitiveResolver: IDataTypeInferrer) {
    this.name = name
    this.isEmbedded = isEmbedded
    this.fields = {}
    this.embeddedTypes = {}
    this.primitiveResolver = primitiveResolver
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
    let type = ModelSampler.ErrorType
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

    // TODO: Abstract away
    const isId = info.name === MongoIdName

    // https://www.prisma.io/docs/releases-and-maintenance/releases-and-beta-access/mongodb-preview-b6o5/#directives
    // TODO: we might want to include directives, as soon as we start changing names. 
    return {
      name: info.name,
      type: type,
      isId: isId,
      isList: isArray,
      isReadOnly: false,
      isRequired: isRequired,
      isUnique: false, // Never unique in Mongo. 
      relationName: info.isRelationCandidate && !isId ? ModelSampler.ErrorType : null,
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
      const typeInfo = this.primitiveResolver.inferType(value)

      // Recursive embedding case. 
      if(typeInfo.type === ObjectTypeIdentifier) {
        // Generate pretty embedded model name, which has no purpose outside of the schema. 
        this.embeddedTypes[name] = this.embeddedTypes[name] || new ModelMerger(this.name + capitalize(name), true, this.primitiveResolver)
        if(typeInfo.isArray) {
          // Embedded array. 
          for(const item of value) {
            this.embeddedTypes[name].analyze(item)
          }
        } else {
          this.embeddedTypes[name].analyze(value)
        }
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
          type: null,
          isRelationCandidate: false
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
      types: types,
      isRelationCandidate: field.isRelationCandidate || info.isRelationCandidate
    }
  }

  private merge<T>(target: Array<T>, value: T) {
    if(target.indexOf(value) < 0) {
      target.push(value)
    }
    return target
  }
}