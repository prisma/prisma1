import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'

export enum SamplingStrategy {
  One = 'One', 
  All = 'All',
  Random = 'Random'
}

const randomSamplingLimit = 50

// Maybe we need a new type here in the future. 
export abstract class DocumentConnector<InternalCollectionType> implements IConnector {

  protected samplingStrategy 

  constructor(samplingStrategy: SamplingStrategy) {
    this.samplingStrategy = samplingStrategy
  }

  abstract getDatabaseType(): DatabaseType
  abstract listSchemas(): Promise<string[]>
  protected abstract getInternalCollections(schema: string): Promise<InternalCollectionType[]>
  abstract introspect(schema: string): Promise<DocumentIntrospectionResult> 
  protected abstract sampleOne(collection: InternalCollectionType): AsyncIterableIterator<Data> 
  protected abstract sampleMany(collection: InternalCollectionType, limit: number): AsyncIterableIterator<Data> 
  protected abstract sampleAll(collection: InternalCollectionType): AsyncIterableIterator<Data>
  protected abstract exists(collection: InternalCollectionType, id: any): Promise<boolean>

  public sample(collection: InternalCollectionType) {
    switch(this.samplingStrategy) {
      case SamplingStrategy.One: return this.sampleOne(collection)
      case SamplingStrategy.All: return this.sampleAll(collection)
      case SamplingStrategy.Random: return this.sampleMany(collection, randomSamplingLimit)
    }
  }

  public async listModels(schemaName: string): Promise<ISDL> {
    let models = []

    for(const collection of await this.getInternalCollections(schemaName)) {
      for await (const model of this.sample(collection)) {

      }
    }
  }
}
