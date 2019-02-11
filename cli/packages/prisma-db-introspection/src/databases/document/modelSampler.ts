import { IGQLField, IGQLType, IComment, TypeIdentifier, TypeIdentifiers, capitalize } from 'prisma-datamodel'
import { Data } from './data'
import { isArray, isRegExp } from 'util'
import { ObjectID } from 'bson' // TODO - remove dependency to mongo's BSON and subclass to abstract primitive type inferrer.
import {
  SamplingStrategy,
  IDocumentConnector,
  IDataTypeInferrer,
  InternalType,
  ObjectTypeIdentifier,
  UnsupportedTypeErrorKey,
  UnsupportedArrayTypeErrorKey,
  TypeInfo,
} from './documentConnector'

const MongoIdName = '_id'

interface Embedding {
  type: ModelMerger
  isArray: boolean
}

/**
 * Aggregates info about a field.
 */
interface FieldInfo {
  name: string
  types: InternalType[]
  isArray: boolean[]
  isRelationCandidate: boolean
  invalidTypes: string[]
}

/**
 * Samples all collections in a database and infers all primitive fields and embedded types.
 * Does not infer relations. Fields which might have a relation get their `relationName` attribute
 * set to `ModelSampler.ErrorType` for later resolving.
 */
export class ModelSampler<InternalCollectionType> implements ModelSampler<InternalCollectionType> {
  private samplingStrategy: SamplingStrategy

  public static ErrorType = '<Unknown>'

  /**
   * @param samplingStrategy The sampling strategy to use.
   */
  constructor(samplingStrategy: SamplingStrategy = SamplingStrategy.One) {
    this.samplingStrategy = samplingStrategy
  }

  /**
   * Samples all Collections in the given schema.
   * @param connector The connector, delivering the data.
   * @param schemaName The name of the schema to resolve.
   * @param primitiveResolver The resolver used for resolving primitive types.
   */
  public async sample(
    connector: IDocumentConnector<InternalCollectionType>,
    schemaName: string,
    primitiveResolver: IDataTypeInferrer,
  ) {
    let types: IGQLType[] = []

    const allCollections = await connector.getInternalCollections(schemaName)
    for (const { name, collection } of allCollections) {
      // For each collection, create a merging context.
      const merger = new ModelMerger(name, false, primitiveResolver)

      // Iterate over all samples.
      const iterator = await connector.sample(collection, this.samplingStrategy)
      while (await iterator.hasNext()) {
        // Merge each sample into our model.
        const item = await iterator.next()
        merger.analyze(item)
      }
      await iterator.close()
      // Construct the actual type.
      const mergeResult = merger.getType()
      types.push(mergeResult.type, ...mergeResult.embedded)
    }
    return types
  }
}

/**
 * Infers structure of a datatype from a set of data samples.
 *
 * Follows a streaming pattern to reduce the memory footprint
 */
export class ModelMerger {
  private fields: { [fieldName: string]: FieldInfo }
  private embeddedTypes: { [typeName: string]: ModelMerger }

  public name: string
  public isEmbedded: boolean
  public primitiveResolver: IDataTypeInferrer

  /**
   * @param name Name of the type
   * @param isEmbedded Indicates if the type is an embedded type
   * @param primitiveResolver Resolver or primitive types.
   */
  constructor(name: string, isEmbedded: boolean, primitiveResolver: IDataTypeInferrer) {
    this.name = name
    this.isEmbedded = isEmbedded
    this.fields = {}
    this.embeddedTypes = {}
    this.primitiveResolver = primitiveResolver
  }

  /**
   * Analyzes this data sample.
   */
  public analyze(data: Data) {
    for (const fieldName of Object.keys(data)) {
      this.analyzeField(fieldName, data[fieldName])
    }
  }

  /**
   * Gets the top level type only.
   */
  public getTopLevelType(): IGQLType {
    const fields = Object.keys(this.fields).map(key => this.toIGQLField(this.fields[key]))

    return {
      fields: fields,
      isEmbedded: this.isEmbedded,
      isLinkTable: false, // No link table in mongo
      name: this.name,
      isEnum: false, // No enum in mongo
      comments: [],
      directives: [],
      databaseName: null,
      indices: [],
    }
  }

  /**
   * Gets the type with embedded types attached recursivley.
   */
  public getType(): { type: IGQLType; embedded: IGQLType[] } {
    const type = this.getTopLevelType()
    const allEmbedded: IGQLType[] = []

    // Recurse over all embedded types.
    for (const embeddedFieldName of Object.keys(this.embeddedTypes)) {
      const embedded = this.embeddedTypes[embeddedFieldName].getType()
      allEmbedded.push(embedded.type, ...embedded.embedded)

      for (const field of type.fields) {
        if (field.name === embeddedFieldName) {
          field.type = embedded.type
        }
      }
    }

    return { type: type, embedded: allEmbedded }
  }

  /**
   * Takes a list of type candidates and merges them into one single type.
   *
   * TODO: If we get more types to summarize, we should have all summarization code (e.g. for arrays)
   * in one place.
   */

  private summarizeTypeList(typeCandidates: InternalType[]) {
    // Our float/int decision is based soley on the data itself.
    // If we have at least one float, integer is always a misguess.
    if (typeCandidates.indexOf(TypeIdentifiers.float) >= 0) {
      typeCandidates = typeCandidates.filter(x => x !== TypeIdentifiers.integer)
    }

    return typeCandidates
  }

  /**
   * Merges all collected info into a field, including type info, relation info
   * and array properties.
   *
   * Creates error comments on inconsistency.
   */
  private toIGQLField(info: FieldInfo): IGQLField {
    let type = ModelSampler.ErrorType
    let isArray = false
    let isRequired = false
    const comments: IComment[] = []

    // Check for inconsistent array usage
    if (info.isArray.length > 1) {
      comments.push({
        isError: true,
        text: 'Datatype inconsistency: Sometimes is array, and sometimes not.',
      })
    } else if (info.isArray.length === 1) {
      isArray = info.isArray[0]
    }

    // Check for inconsistent data type
    let typeCandidates = this.summarizeTypeList([...info.types])
    let invalidTypes = info.invalidTypes

    if (typeCandidates.length > 1) {
      comments.push({
        isError: true,
        text: 'Datatype inconsistency. Conflicting types found: ' + typeCandidates.join(', '),
      })
    }

    // Check for missing data type
    // If we have a type error, let the type be ModelSampler.ErrorType,
    // so we have a constant to check for.
    if (typeCandidates.length === 1) {
      type = typeCandidates[0]
    } else if (invalidTypes.length === 0) {
      // No type info at all.
      comments.push({
        isError: true,
        text: 'No type information found for field.',
      })
    } else if (invalidTypes.length === 1) {
      // No conflict, but an invalid type
      comments.push({
        isError: true,
        text: 'Field type not supported: ' + invalidTypes[0],
      })
    } else {
      comments.push({
        isError: true,
        text: 'Field type not found due to conflict. Candidates: ' + [...typeCandidates].join(', '),
      })
    }

    // TODO: Abstract away
    const isId = info.name === MongoIdName

    // TODO: This can be changed to allow other _id types.
    if (isId && type !== TypeIdentifiers.id) {
      comments.push({
        isError: false,
        text: `Type ${type} is currently not supported for id fields.`,
      })
    }

    // https://www.prisma.io/docs/releases-and-maintenance/releases-and-beta-access/mongodb-preview-b6o5/#directives
    // TODO: we might want to include directives, as soon as we start changing field names. Otherwise, we can put that into a different module.

    return {
      name: info.name,
      type: type,
      isId: isId,
      idStrategy: null,
      associatedSequence: null,
      isList: isArray,
      isReadOnly: false,
      // ID fields are always required
      isRequired: isRequired || isId,
      // Never unique in Mongo.
      isUnique: false,
      // Reserved relation name for potential relations.
      relationName: info.isRelationCandidate && !isId ? ModelSampler.ErrorType : null,
      relatedField: null,
      defaultValue: null,
      comments: comments,
      databaseName: null,
      directives: [],
      isCreatedAt: false,
      isUpdatedAt: false,
    }
  }

  /**
   * Initialization helper for empty field info
   * structures
   * @param name
   */
  private initField(name: string) {
    this.fields[name] = this.fields[name] || {
      invalidTypes: [],
      isArray: [],
      name: name,
      types: [],
    }
  }

  /**
   * Analyzes a field with respect to it's value.
   */
  private analyzeField(name: string, value: any) {
    try {
      // Attempt field analysis
      const typeInfo = this.primitiveResolver.inferType(value)

      // Recursive embedding case.
      if (typeInfo.type === ObjectTypeIdentifier) {
        // Generate basic embedded model name, which has no purpose outside of the schema.
        this.embeddedTypes[name] =
          this.embeddedTypes[name] || new ModelMerger(this.name + capitalize(name), true, this.primitiveResolver)
        if (typeInfo.isArray) {
          // Embedded array.
          for (const item of value) {
            this.embeddedTypes[name].analyze(item)
          }
        } else {
          this.embeddedTypes[name].analyze(value)
        }
      }

      this.initField(name)
      this.fields[name] = this.mergeField(this.fields[name], typeInfo)
    } catch (err) {
      // On error, register an invalid type.
      if (err.name === UnsupportedTypeErrorKey) {
        this.initField(name)
        this.fields[name].invalidTypes = this.merge(this.fields[name].invalidTypes, err.invalidType)
      } else if (err.name == UnsupportedArrayTypeErrorKey) {
        this.initField(name)
        this.fields[name] = this.mergeField(this.fields[name], {
          isArray: true,
          type: null,
          isRelationCandidate: false,
        })
        this.fields[name].invalidTypes = this.merge(this.fields[name].invalidTypes, err.invalidType)
      } else {
        throw err
      }
    }
  }

  /**
   * Merges two field infos.
   */
  private mergeField(field: FieldInfo, info: TypeInfo): FieldInfo {
    let types = field.types

    if (info.type !== null) {
      types = this.merge(field.types, info.type)
    }

    return {
      invalidTypes: field.invalidTypes,
      isArray: this.merge(field.isArray, info.isArray),
      name: field.name,
      types: types,
      isRelationCandidate: field.isRelationCandidate || info.isRelationCandidate,
    }
  }

  private merge<T>(target: Array<T>, value: T) {
    if (target.indexOf(value) < 0) {
      target.push(value)
    }
    return target
  }
}
