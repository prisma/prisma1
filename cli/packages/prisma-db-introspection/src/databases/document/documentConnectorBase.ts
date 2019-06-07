import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType, IGQLType, TypeIdentifiers } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'
import { ModelSampler } from './modelSampler'
import { RelationResolver } from './relationResolver'
import { Data } from './data'
import {
  IDocumentConnector,
  ICollectionDescription,
  SamplingStrategy,
  IDataIterator,
  InternalType,
  UnsupportedArrayTypeError,
  TypeInfo,
  ObjectTypeIdentifier,
  UnsupportedTypeError,
} from './documentConnector'

import * as debug from 'debug'

let log = debug('DocumentConnector')

/**
 * Sets how many items are queried when the `Random` sampling strategy is used.
 */
const randomSamplingLimit = 50

/**
 * Sets the minimum hit/miss ratio needed to identify a pair of field, model as relation.
 */
const relationThreshold = 0.3

/**
 * Base implementation of a DocumentConnector, holding all logic which should be equal for all
 * Document databases. For a documentation of abstract members, please see IDocumentConnector.
 */
export abstract class DocumentConnector<InternalCollectionType>
  implements IDocumentConnector<InternalCollectionType> {
  abstract getDatabaseType(): DatabaseType
  abstract listSchemas(): Promise<string[]>
  public abstract getInternalCollections(
    schema: string,
  ): Promise<ICollectionDescription<InternalCollectionType>[]>
  public abstract getInternalCollection(
    schema: string,
    collection: string,
  ): Promise<InternalCollectionType>
  public abstract exists(
    collection: InternalCollectionType,
    id: any,
  ): Promise<boolean>
  /**
   * Samples a single document from the given collection.
   */
  protected abstract sampleOne(
    collection: InternalCollectionType,
  ): Promise<IDataIterator>
  /**
   * Samples a number of documents from the given collection. The documents are randomly picked and
   * can be duplicates.
   */
  protected abstract sampleMany(
    collection: InternalCollectionType,
    limit: number,
  ): Promise<IDataIterator>
  /**
   * Samples all documents from a collection.
   */
  protected abstract sampleAll(
    collection: InternalCollectionType,
  ): Promise<IDataIterator>

  /**
   * Calls `listModels` and wraps the result.
   */
  public async introspect(
    schema: string,
  ): Promise<DocumentIntrospectionResult> {
    return new DocumentIntrospectionResult(
      await this.listModels(schema),
      this.getDatabaseType(),
    )
  }

  public sample(
    collection: InternalCollectionType,
    samplingStrategy: SamplingStrategy,
  ): Promise<IDataIterator> {
    switch (samplingStrategy) {
      case SamplingStrategy.One:
        return this.sampleOne(collection)
      case SamplingStrategy.All:
        return this.sampleAll(collection)
      case SamplingStrategy.Random:
        return this.sampleMany(collection, randomSamplingLimit)
    }
    throw new Error('Invalid sampling type specified: ' + samplingStrategy)
  }

  public async listModels(
    schemaName: string,
    modelSamplingStrategy: SamplingStrategy = SamplingStrategy.Random,
    relationSamplingStrategy: SamplingStrategy = SamplingStrategy.Random,
  ): Promise<ISDL> {
    // First, we sample our collections to create a flat type schema.
    // Then, we attempt to find relations using sampling and a ratio test.

    log('Listing models.')

    const sampler = new ModelSampler<InternalCollectionType>(
      modelSamplingStrategy,
    )
    const resolver = new RelationResolver<InternalCollectionType>(
      relationSamplingStrategy,
      relationThreshold,
    )

    const types = await sampler.sample(this, schemaName, this)
    await resolver.resolve(types, this, schemaName)

    return {
      types,
    }
  }

  /**
   * Infers the primitive type for an array. Uses inferType internally.
   */
  private inferArrayType(array: any[]) {
    var type: TypeInfo | null = null

    for (const item of array) {
      const itemType = this.inferType(item)

      if (itemType.isArray) {
        throw new UnsupportedArrayTypeError(
          'Received a nested array while analyzing data. This is not supported yet.',
          'ArrayArray',
        )
      }

      if (type === null) {
        type = itemType
      } else if (
        (type.type === TypeIdentifiers.integer &&
          itemType.type === TypeIdentifiers.float) ||
        (type.type === TypeIdentifiers.float &&
          itemType.type === TypeIdentifiers.integer)
      ) {
        // Special case: We treat int as a case of float, if needed.
        type.type = TypeIdentifiers.float
      } else if (type.type !== itemType.type) {
        throw new UnsupportedArrayTypeError(
          'Mixed arrays are not supported.',
          `[${type.type} | ${itemType.type}]`,
        )
      }
    }

    return type
  }

  /**
   * Infers the type of a primitive value.
   *
   * This method should be overridden accordingly for each Connector implementation.
   */
  public inferType(value: any): TypeInfo {
    // Maybe an array, which would otherwise be identified as object.
    if (Array.isArray(value)) {
      // We resolve this case recursively.
      const type = this.inferArrayType(value)
      if (type !== null) {
        return {
          type: type.type,
          isArray: true,
          isRelationCandidate: type.isRelationCandidate,
        }
      } else {
        return { type: null, isArray: true, isRelationCandidate: false }
      }
    }

    // Base types
    switch (typeof value) {
      case 'number':
        return {
          type:
            value % 1 === 0 ? TypeIdentifiers.integer : TypeIdentifiers.float,
          isArray: false,
          isRelationCandidate: false,
        }
      case 'boolean':
        return {
          type: TypeIdentifiers.boolean,
          isArray: false,
          isRelationCandidate: false,
        }
      // Base case: String types might identify relations.
      case 'string':
        return {
          type: TypeIdentifiers.string,
          isArray: false,
          isRelationCandidate: true,
        }
      case 'object':
        if (value instanceof Date) {
          return {
            type: TypeIdentifiers.dateTime,
            isArray: false,
            isRelationCandidate: false,
          }
        } else if (value === null) {
          throw new UnsupportedTypeError(
            'Received an unsupported type: ',
            'null',
          )
        } else {
          return {
            type: ObjectTypeIdentifier,
            isArray: false,
            isRelationCandidate: false,
          }
        }
      default:
        break
    }

    throw new UnsupportedTypeError(
      'Received an unsupported type:',
      typeof value,
    )
  }
}
