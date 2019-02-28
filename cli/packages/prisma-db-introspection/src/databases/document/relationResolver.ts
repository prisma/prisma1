import {
  SamplingStrategy,
  ICollectionDescription,
  IDocumentConnector,
  IDataExists,
} from './documentConnector'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import { Data } from './data'
import { ModelSampler } from './modelSampler'
import { IDirectiveInfo } from 'prisma-datamodel'

export interface IRelationResolver<InternalCollectionType> {
  resolve(
    types: IGQLType[],
    resolver: IDataExists<InternalCollectionType>,
    schemaName: string,
  ): Promise<void>
}

interface IRelationScore {
  hits: number
  misses: number
}

/**
 * Resolves relations on a given model by querying the database.
 */
export class RelationResolver<InternalCollectionType>
  implements IRelationResolver<InternalCollectionType> {
  private samplingStrategy: SamplingStrategy
  private ratioThreshold: number

  /**
   * @param samplingStrategy Sampling strategy for picking samples.
   * @param threshold Ratio of hits/(misses + hits) we need to accept.
   */
  constructor(
    samplingStrategy: SamplingStrategy = SamplingStrategy.Random,
    threshold: number = 0.5,
  ) {
    this.samplingStrategy = samplingStrategy
    this.ratioThreshold = threshold
  }

  /**
   * Resolves relations in the given type list.
   * @param types Types to resolve relations for. Can be a subset of all types in the database,
   * we still check all collections. Types are edited in-place.
   * @param connector Database connector for sampling
   * @param schemaName The schema to work on.
   */
  public async resolve(
    types: IGQLType[],
    connector: IDocumentConnector<InternalCollectionType>,
    schemaName: string,
  ) {
    const allCollections = await connector.getInternalCollections(schemaName)
    for (const type of types) {
      if (!type.isEmbedded) {
        const [collection] = allCollections.filter(x => x.name === type.name)

        if (collection === undefined) {
          throw new Error(
            `Missmatch between collections and given types: Collection ${
              type.name
            } does not exist.`,
          )
        }

        const iterator = await connector.sample(
          collection.collection,
          this.samplingStrategy,
        )

        // Create resolver context
        const context = new RelationResolveContext<InternalCollectionType>(
          type,
          allCollections,
          connector,
        )
        // Iterate over samples
        while (await iterator.hasNext()) {
          const data = await iterator.next()
          await context.attemptResolve(data)
        }
        await iterator.close()

        // Collect results
        context.connectRelationsIfResolved(types, this.ratioThreshold)
      }
    }
  }
}

/**
 * Resolver context for resolving relations.
 * Follows a streaming pattern to keep the memory footprint low.
 */
class RelationResolveContext<Type> {
  private type: IGQLType
  // First dimension: Field name, second: Remote Collection
  private fieldScores: { [key: string]: { [key: string]: IRelationScore } }
  private embeddedTypes: { [key: string]: RelationResolveContext<Type> }
  private collections: ICollectionDescription<Type>[]
  private connector: IDataExists<Type>

  /**
   * @param type The type which is being resolved.
   * @param collections A list of all existing collections.
   * @param connector A connector, capable of checking wether a certain item exists.
   */
  constructor(
    type: IGQLType,
    collections: ICollectionDescription<Type>[],
    connector: IDataExists<Type>,
  ) {
    this.type = type
    this.collections = collections
    this.connector = connector
    this.fieldScores = {}
    this.embeddedTypes = {}

    // Build up hit/miss table for all combinations of field/collection
    for (const field of type.fields) {
      // We do not resolve error'd fields
      if (!this.hasError(field)) {
        if (typeof field.type === 'string') {
          // Primitive
          if (field.relationName === ModelSampler.ErrorType) {
            for (const collection of this.collections) {
              if (!this.fieldScores[field.name]) {
                this.fieldScores[field.name] = {}
              }
              this.fieldScores[field.name][collection.name] = {
                hits: 0,
                misses: 0,
              }
            }
          }
        } else {
          // Embedded document, handled recursively
          this.embeddedTypes[field.name] = new RelationResolveContext<Type>(
            field.type,
            this.collections,
            this.connector,
          )
        }
      }
    }
  }

  private hasError(obj: IGQLField | IGQLType) {
    return obj.comments.some(x => x.isError)
  }

  /**
   * Uses a given document, data, for analysis.
   *
   * For each primitive field of the current type with a potential relation,
   * checks if the corresponding value in data is the id of any collection.
   *
   * If so, increases the hit counter, otherwise, increases the miss counter.
   */
  public async attemptResolve(data: Data) {
    if (Array.isArray(data)) {
      // Array resolve
      for (const val of data) {
        await this.attemptResolve(val)
      }
    } else {
      // Flat resolve
      // TODO: Warn on model missmatch?
      for (const field of this.type.fields) {
        if (!this.hasError(field)) {
          if (typeof field.type === 'string') {
            // Primitive field
            if (field.relationName === ModelSampler.ErrorType) {
              if (data[field.name] !== undefined) {
                const value = data[field.name]
                if (Array.isArray(value) !== field.isList) {
                  throw new Error(
                    `Array declaration missmatch: ${this.type.name}.${
                      field.name
                    } `,
                  )
                }
                const values = Array.isArray(value) ? value : [value]
                // Handling of array relations.
                for (const value of values) {
                  for (const collection of this.collections) {
                    if (
                      await this.connector.exists(collection.collection, value)
                    ) {
                      this.fieldScores[field.name][collection.name].hits += 1
                    } else {
                      this.fieldScores[field.name][collection.name].misses += 1
                    }
                  }
                }
              }
            }
          } else {
            // Embedded document, recursive
            if (data[field.name] !== undefined) {
              await this.embeddedTypes[field.name].attemptResolve(
                data[field.name],
              )
            }
          }
        }
      }
    }
  }

  /**
   * Checks all aggregated hits and misses, and if the hit/miss ratio
   * is high enough, adds a relation.
   * @param availableTypes A list of all types in the database.
   * @param threshold The hit/miss ratio threshold.
   */
  public connectRelationsIfResolved(
    availableTypes: IGQLType[],
    threshold: number = 0.3,
  ) {
    // Recursive, as above, find maximal candidate, check if is over threshold, connect all.
    for (const field of this.type.fields) {
      if (!this.hasError(field)) {
        if (typeof field.type === 'string') {
          if (field.relationName === ModelSampler.ErrorType) {
            // Primitive field
            let bestRatio = 0
            let bestCandidate: ICollectionDescription<Type> | null = null
            for (const collection of this.collections) {
              const score = this.fieldScores[field.name][collection.name]
              const ratio =
                score.misses + score.hits === 0
                  ? 0
                  : score.hits / (score.misses + score.hits)

              if (ratio > bestRatio || bestCandidate === null) {
                bestRatio = ratio
                bestCandidate = collection
              }
            }

            if (bestRatio > threshold && bestCandidate !== null) {
              const candidateName = bestCandidate.name
              const [foreignType] = availableTypes.filter(
                x => x.name == candidateName,
              )

              if (foreignType === undefined) {
                throw new Error(
                  `Missmatch between collections and given types: Type ${candidateName} does not exist.`,
                )
              }

              field.type = foreignType

              // Add relation directive
              const relationDirective: IDirectiveInfo = {
                name: 'relation',
                arguments: {},
              }

              // Explicit assignment here is a workaround for a TS/Jest bug.
              // The object initialization above creates a field called 'arguments_1' for
              // whatever reason.
              relationDirective.arguments = {
                link: 'INLINE',
              }

              field.directives.push(relationDirective)
            }
            // We always remove the fields <unknown> relation tag.
            field.relationName = null
          }
        } else {
          // Embedded document, recursive
          this.embeddedTypes[field.name].connectRelationsIfResolved(
            availableTypes,
            threshold,
          )
        }
      }
    }

    return this.type
  }
}
