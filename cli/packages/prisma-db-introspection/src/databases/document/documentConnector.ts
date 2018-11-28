import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType, IGQLType } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'
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
  close(): Promise<void>
}

export interface ICollectionDescription<Type> {
  name: string, 
  collection: Type
}

export interface IDocumentConnector<InternalCollectionType> extends IConnector {
  getDatabaseType(): DatabaseType
  listSchemas(): Promise<string[]>
  getInternalCollections(schema: string): Promise<ICollectionDescription<InternalCollectionType>[]>
  getInternalCollection(schema: string, collection: string): Promise<InternalCollectionType>
  exists(collection: InternalCollectionType, id: any): Promise<boolean>
  introspect(schema: string): Promise<DocumentIntrospectionResult>
  sample(collection: InternalCollectionType, samplingStrategy: SamplingStrategy): Promise<DataIterator>
  listModels(schemaName: string, modelSamplingStrategy: SamplingStrategy, relationSamplingStrategy: SamplingStrategy): Promise<ISDL>
}
