import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType, IGQLType } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'
import { ModelSampler } from './modelSampler'
import { RelationResolver } from './relationResolver'
import { Data } from './data'
import { IDocumentConnector, ICollectionDescription, SamplingStrategy, IDataIterator } from './documentConnector'

const randomSamplingLimit = 50
const relationThreshold = 0.3

// Maybe we need a new type here in the future. 
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

    const types = await sampler.sample(this, schemaName)
    await resolver.resolve(types, this, schemaName)

    return {
      types
    }
  }
}
