import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType, IGQLType, TypeIdentifier } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'
import { Data } from './data'

export enum SamplingStrategy {
  One = 'One', 
  All = 'All',
  Random = 'Random'
}

export interface IDataIterator {
  hasNext(): Promise<boolean>
  next(): Promise<Data>
  close(): Promise<void>
}

export interface ICollectionDescription<Type> {
  name: string, 
  collection: Type
}

export interface IDataExists<InternalCollectionType> {
  exists(collection: InternalCollectionType, id: any): Promise<boolean>
}

export const ObjectTypeIdentifier = 'EmbeddedObject'
export type InternalType = TypeIdentifier | 'EmbeddedObject'

export interface TypeInfo {
  type: InternalType | null,
  isArray: boolean,
  isRelationCandidate: boolean
}

export const UnsupportedTypeErrorKey = 'UnsupportedType'
export const UnsupportedArrayTypeErrorKey = 'UnsupportedArrayType'

export class UnsupportedTypeError extends Error {
  public invalidType: string

  constructor(public message: string, invalidType: string) {
    super(message);
    this.name = UnsupportedTypeErrorKey
    this.invalidType = invalidType
  }
}

export class UnsupportedArrayTypeError extends Error {
  public invalidType: string

  constructor(public message: string, invalidType: string) {
    super(message);
    this.name = UnsupportedArrayTypeErrorKey
    this.invalidType = invalidType
  }
}

export interface IDataTypeInferrer {
  inferType(value: any): TypeInfo
}

export interface IDocumentConnector<InternalCollectionType> extends IConnector, IDataExists<InternalCollectionType>, IDataTypeInferrer {
  getDatabaseType(): DatabaseType
  listSchemas(): Promise<string[]>
  getInternalCollections(schema: string): Promise<ICollectionDescription<InternalCollectionType>[]>
  getInternalCollection(schema: string, collection: string): Promise<InternalCollectionType>
  introspect(schema: string): Promise<DocumentIntrospectionResult>
  sample(collection: InternalCollectionType, samplingStrategy: SamplingStrategy): Promise<IDataIterator>
  listModels(schemaName: string, modelSamplingStrategy: SamplingStrategy, relationSamplingStrategy: SamplingStrategy): Promise<ISDL>
}
