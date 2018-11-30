import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType, IGQLType, TypeIdentifiers } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'
import { ModelSampler } from './modelSampler'
import { RelationResolver } from './relationResolver'
import { Data } from './data'
import { IDocumentConnector, ICollectionDescription, SamplingStrategy, IDataIterator, InternalType, UnsupportedArrayTypeError, TypeInfo, ObjectTypeIdentifier, UnsupportedTypeError } from './documentConnector'

const randomSamplingLimit = 50
const relationThreshold = 0.3

export abstract class DocumentConnector<InternalCollectionType> implements IDocumentConnector<InternalCollectionType> {
  abstract getDatabaseType(): DatabaseType
  abstract listSchemas(): Promise<string[]>
  public abstract getInternalCollections(schema: string): Promise<ICollectionDescription<InternalCollectionType>[]>
  public abstract getInternalCollection(schema: string, collection: string): Promise<InternalCollectionType>
  protected abstract sampleOne(collection: InternalCollectionType): Promise<IDataIterator>
  protected abstract sampleMany(collection: InternalCollectionType, limit: number): Promise<IDataIterator>
  protected abstract sampleAll(collection: InternalCollectionType): Promise<IDataIterator>
  public abstract exists(collection: InternalCollectionType, id: any): Promise<boolean>

  public async introspect(schema: string): Promise<DocumentIntrospectionResult> {
    return new DocumentIntrospectionResult(await this.listModels(schema), this.getDatabaseType())
  }

  public sample(collection: InternalCollectionType, samplingStrategy: SamplingStrategy): Promise<IDataIterator> {
    switch(samplingStrategy) {
      case SamplingStrategy.One: return this.sampleOne(collection)
      case SamplingStrategy.All: return this.sampleAll(collection)
      case SamplingStrategy.Random: return this.sampleMany(collection, randomSamplingLimit)
    }
    throw new Error('Invalid sampling type specified: ' + samplingStrategy)
  }

  public async listModels(schemaName: string, modelSamplingStrategy: SamplingStrategy = SamplingStrategy.One, relationSamplingStrategy: SamplingStrategy = SamplingStrategy.Random): Promise<ISDL> {
    const sampler = new ModelSampler<InternalCollectionType>(modelSamplingStrategy)
    const resolver = new RelationResolver<InternalCollectionType>(relationSamplingStrategy, relationThreshold)

    const types = await sampler.sample(this, schemaName, this)
    await resolver.resolve(types, this, schemaName)

    return {
      types
    }
  }

  private inferArrayType(array: any[]) {
    var type: TypeInfo | null = null
    
    for(const item of array) {
      const itemType = this.inferType(item)

      if(itemType.isArray) {
        throw new UnsupportedArrayTypeError('Received a nested array while analyzing data. This is not supported yet.', 'ArrayArray')
      }

      if(type === null) {
        type = itemType
      } else if(type.type === TypeIdentifiers.integer && itemType.type === TypeIdentifiers.float ||
                type.type === TypeIdentifiers.float && itemType.type === TypeIdentifiers.integer) {
        // Special case: We treat int as a case of float, if needed.
        type.type = TypeIdentifiers.float
      } else if(type.type !== itemType.type) {
        throw new UnsupportedArrayTypeError('Mixed arrays are not supported.', `Array of ${type} and ${itemType.type}`)
      }
    }

    return type
  }

  private isRelationCandidate(value: any): boolean {
    return typeof(value) === 'string'
  }

  public inferType(value: any): TypeInfo  {
    // Maybe an array, which would otherwise be identified as object.
    if (Array.isArray(value)) {
      // We resolve this case recursively.
      const type = this.inferArrayType(value) 
      if(type !== null) {
        return { type: type.type, isArray: true, isRelationCandidate: type.isRelationCandidate }
      } else {
        return { type: null, isArray: true, isRelationCandidate: false }
      }
    }

    const isRelationCandidate = this.isRelationCandidate(value)

    // Base types
    switch(typeof(value)) {
      case "number": return { type: value % 1 === 0 ? TypeIdentifiers.integer : TypeIdentifiers.float, isArray: false, isRelationCandidate }
      case "boolean": return { type: TypeIdentifiers.boolean, isArray: false, isRelationCandidate }
      case "string": return { type: TypeIdentifiers.string, isArray: false, isRelationCandidate }
      case "object": return { type: ObjectTypeIdentifier, isArray: false, isRelationCandidate }
      default: break
    }

    throw new UnsupportedTypeError('Received an unsupported type:', typeof value)
  }
}
