import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType, IGQLType, TypeIdentifier } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'
import { Data } from './data'

/**
 * Enum specifying the sampling strategy.
 */
export enum SamplingStrategy {
  One = 'One', 
  All = 'All',
  Random = 'Random'
}

/**
 * An interface specifying an async
 * iterator over a Collection. 
 * This is needed as for await is
 * only supported in later node versions. 
 */
export interface IDataIterator {
  hasNext(): Promise<boolean>
  next(): Promise<Data>
  close(): Promise<void>
}

/**
 * Describes a collection of abritary type.
 */
export interface ICollectionDescription<Type> {
  name: string, 
  collection: Type
}

/**
 * Specifies a simple `exists` method for any data source.
 */
export interface IDataExists<InternalCollectionType> {
  /**
   * Returns true if the item with the given id exists on
   * the given collection, false otherwise. 
   */
  exists(collection: InternalCollectionType, id: any): Promise<boolean>
}

/**
 * Reserved internal type for embedded objects, 
 * used as a placeholder until SDL is constructed. 
 */
export const ObjectTypeIdentifier = 'EmbeddedObject'
export type InternalType = TypeIdentifier | 'EmbeddedObject'

/**
 * Internal type information data structure. 
 */
export interface TypeInfo {
  /**
   * The type, or null if the type is unknown. 
   */
  type: InternalType | null,
  /**
   * Is an array.
   */
  isArray: boolean,
  /**
   * Might be a relation.
   */
  isRelationCandidate: boolean
}

export const UnsupportedTypeErrorKey = 'UnsupportedType'
export const UnsupportedArrayTypeErrorKey = 'UnsupportedArrayType'

/**
 * Error which should be thrown by the type inferrer
 * when it encounters an unsupported type, for example 
 * a binary blob. 
 */
export class UnsupportedTypeError extends Error {
  public invalidType: string

  constructor(public message: string, invalidType: string) {
    super(message);
    this.name = UnsupportedTypeErrorKey
    this.invalidType = invalidType
  }
}

/**
 * Error which should be thrown by the type inferrer 
 * when it encounters an unsupported array type, for example
 * a nested array.
 */
export class UnsupportedArrayTypeError extends Error {
  public invalidType: string

  constructor(public message: string, invalidType: string) {
    super(message);
    this.name = UnsupportedArrayTypeErrorKey
    this.invalidType = invalidType
  }
}

/**
 * Specifies a `inferType` method to infer primitive types from data.
 */
export interface IDataTypeInferrer {
  /**
   * Infers a primitive type from a given value. 
   * Also, infers if the type is array or not. 
   * 
   * If a complex type (e.g. an Object) is encountered, 
   * the value of `ObjectTypeIdentifier` is returned. 
   * 
   * If an invalid type is encountered, a `UnsupportedArrayTypeError` or
   * `UnsupportedTypeError` is thrown. 
   */
  inferType(value: any): TypeInfo
}

/**
 * Interface for DocumentDatabse connectors, which implement all methods necessary for a 
 * complete schema resolving.
 * 
 * Please also see the abstract implementation of this interface, `DocumentConnector`.
 */
export interface IDocumentConnector<InternalCollectionType> extends IConnector, IDataExists<InternalCollectionType>, IDataTypeInferrer {
  /**
   * Returns a list of all Collections available in this schema.
   */
  getInternalCollections(schema: string): Promise<ICollectionDescription<InternalCollectionType>[]>
  /**
   * Returns a specific Collection, identifyed by it's name.
   */
  getInternalCollection(schema: string, collection: string): Promise<InternalCollectionType>
  /**
   * Introspects this schema and returns a introspection result.
   */
  introspect(schema: string): Promise<DocumentIntrospectionResult>
  /**
   * Samples a number of items from the given collection, using the given sampling strategy.
   */
  sample(collection: InternalCollectionType, samplingStrategy: SamplingStrategy): Promise<IDataIterator>
  /**
   * Lists all models found in the database, provides a more fine-grained result for the used
   * strategies, but is incompatible with the `IConnector` interface. 
   */
  listModels(schemaName: string, modelSamplingStrategy: SamplingStrategy, relationSamplingStrategy: SamplingStrategy): Promise<ISDL>
}
