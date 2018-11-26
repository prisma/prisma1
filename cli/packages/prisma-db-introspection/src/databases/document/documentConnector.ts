import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType, IGQLType } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'
import { ModelMerger } from './modelMerger'
import { Data } from './data'

export enum SamplingStrategy {
  One = 'One', 
  All = 'All',
  Random = 'Random'
}

const randomSamplingLimit = 50

export interface DataIterator {
  hasNext(): Promise<boolean>
  next(): Promise<Data>
}

// Maybe we need a new type here in the future. 
export abstract class DocumentConnector<InternalCollectionType> implements IConnector {

  protected samplingStrategy 

  constructor(samplingStrategy: SamplingStrategy) {
    this.samplingStrategy = samplingStrategy
  }

  abstract getDatabaseType(): DatabaseType
  abstract listSchemas(): Promise<string[]>
  protected abstract getInternalCollections(schema: string): Promise<{ name: string, collection: InternalCollectionType }[]>
  abstract introspect(schema: string): Promise<DocumentIntrospectionResult> 
  protected abstract sampleOne(collection: InternalCollectionType): Promise<DataIterator>
  protected abstract sampleMany(collection: InternalCollectionType, limit: number): Promise<DataIterator>
  protected abstract sampleAll(collection: InternalCollectionType): Promise<DataIterator>
  protected abstract exists(collection: InternalCollectionType, id: any): Promise<boolean>

  public sample(collection: InternalCollectionType): Promise<DataIterator> {
    switch(this.samplingStrategy) {
      case SamplingStrategy.One: return this.sampleOne(collection)
      case SamplingStrategy.All: return this.sampleAll(collection)
      case SamplingStrategy.Random: return this.sampleMany(collection, randomSamplingLimit)
    }
    throw new Error('Invalid sampling type specified: ' + this.samplingStrategy)
  }

  public async listModels(schemaName: string): Promise<ISDL> {
    let types: IGQLType[] = []

    const collections = await this.getInternalCollections(schemaName)
    for (const { name, collection } of collections) {
      const merger = new ModelMerger(name, false)
      const iterator = await this.sample(collection)
      while(await iterator.hasNext()) {
        const item = await iterator.next()
        merger.analyze(item)
      }
      types.push(merger.getType())
    }
    return {
      types
    }
  }
}
