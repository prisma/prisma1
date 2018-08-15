import { $$asyncIterator } from 'iterall'
import { buildInfo } from './info'
import {
  GraphQLResolveInfo,
  graphql,
  GraphQLSchema,
  buildSchema,
  GraphQLUnionType,
  GraphQLInterfaceType,
} from 'graphql'
import { delegateToSchema, ReplaceFieldWithFragment } from 'graphql-tools'
import { IResolvers } from 'graphql-tools/dist/Interfaces'
import {
  BindingOptions,
  Options,
  QueryOrMutation,
  Operation,
  FragmentReplacement,
} from './types'
import { importSchema } from 'graphql-import'
import * as fs from 'fs'
import * as path from 'path'

export class Delegate {
  schema: GraphQLSchema
  before: () => void

  private fragmentReplacements: FragmentReplacement[]

  constructor({ schema, fragmentReplacements, before }: BindingOptions) {
    this.fragmentReplacements = fragmentReplacements || []
    this.schema = schema

    this.before = before || (() => undefined)
  }

  public async request<T = any>(
    query: string,
    variables?: { [key: string]: any },
  ): Promise<T> {
    this.before()
    return graphql(this.schema, query, null, null, variables).then(
      r => r as any,
    )
  }

  public async delegate(
    operation: QueryOrMutation,
    fieldName: string,
    args: {
      [key: string]: any
    },
    infoOrQuery?: GraphQLResolveInfo | string,
    options?: Options,
  ) {
    this.before()

    return this.delegateToSchema(
      operation,
      fieldName,
      args,
      infoOrQuery,
      options,
    ).result
  }

  public async delegateSubscription(
    fieldName: string,
    args?: { [key: string]: any },
    infoOrQuery?: GraphQLResolveInfo | string,
    options?: Options,
  ): Promise<AsyncIterator<any>> {
    this.before()

    const { result, info } = this.delegateToSchema(
      'subscription',
      fieldName,
      args,
      infoOrQuery,
      options,
    )

    const iterator = await result

    return {
      async next() {
        const { value } = await iterator.next()
        const data = { [info.fieldName]: value.data[fieldName] }
        return { value: data, done: false }
      },
      return() {
        return Promise.resolve({ value: undefined, done: true })
      },
      throw(error) {
        return Promise.reject(error)
      },
      [$$asyncIterator]() {
        return this
      },
    }
  }

  public getAbstractResolvers(
    filterSchema?: GraphQLSchema | string,
  ): IResolvers {
    const typeMap = this.schema.getTypeMap()

    if (filterSchema && typeof filterSchema === 'string') {
      if (filterSchema.endsWith('graphql')) {
        const schemaPath = path.resolve(filterSchema)

        if (!fs.existsSync(schemaPath)) {
          throw new Error(`No schema found for path: ${schemaPath}`)
        }

        filterSchema = importSchema(schemaPath)
      }
      filterSchema = buildSchema(filterSchema)
    }
    const filterTypeMap =
      filterSchema instanceof GraphQLSchema
        ? filterSchema.getTypeMap()
        : typeMap
    const filterType = typeName => typeName in filterTypeMap

    const resolvers = {}
    Object.keys(typeMap)
      .filter(filterType)
      .forEach(typeName => {
        const type = typeMap[typeName]
        if (
          type instanceof GraphQLUnionType ||
          type instanceof GraphQLInterfaceType
        ) {
          resolvers[typeName] = {
            __resolveType: type.resolveType,
          }
        }
      })

    return resolvers
  }

  private delegateToSchema(
    operation: Operation,
    fieldName: string,
    args?: { [key: string]: any },
    infoOrQuery?: GraphQLResolveInfo | string,
    options?: Options,
  ): { info: any; result: Promise<any> } {
    const info = buildInfo(fieldName, operation, this.schema, infoOrQuery)

    const transforms = options && options.transforms ? options.transforms : []
    const result = delegateToSchema({
      schema: this.schema,
      operation,
      fieldName,
      args: args || {},
      context: options && options.context ? options.context : {},
      info,
      transforms: [
        new ReplaceFieldWithFragment(this.schema, this.fragmentReplacements),
        ...transforms,
      ],
    })

    return { info, result }
  }
}
