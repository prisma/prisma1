import { GraphQLResolveInfo, GraphQLSchema } from 'graphql'
import { Transform } from 'graphql-tools'

export type Operation = 'query' | 'mutation' | 'subscription'
// needed to exclude 'subscription' in delegate api
export type QueryOrMutation = 'query' | 'mutation'

export interface FragmentReplacement {
  field: string
  fragment: string
}

export interface QueryMap {
  [rootField: string]: (
    args?: { [key: string]: any },
    context?: { [key: string]: any },
    info?: GraphQLResolveInfo | string,
  ) => Promise<any>
}

export interface SubscriptionMap {
  [rootField: string]: (
    args?: any,
    context?: { [key: string]: any },
    info?: GraphQLResolveInfo | string,
  ) => AsyncIterator<any> | Promise<AsyncIterator<any>>
}

export interface BindingOptions {
  fragmentReplacements?: FragmentReplacement[]
  schema: GraphQLSchema
  before?: () => void
}

export interface BindingWithoutSchemaOptions {
  fragmentReplacements?: FragmentReplacement[]
  before?: () => void
}

// args: {
//   [key: string]: any
// },
// info?: GraphQLResolveInfo | string,
// context?: {
//   [key: string]: any
// },

export interface Args {
  [key: string]: any
}

export interface Context {
  [key: string]: any
}

export interface Options {
  transforms?: Transform[]
  context?: Context
}

export interface Filter {
  [key: string]: any
}

export interface Exists {
  [rootField: string]: (filter: Filter) => Promise<boolean>
}

export interface BasePrismaOptions {
  fragmentReplacements?: FragmentReplacement[]
  endpoint?: string
  secret?: string
  debug?: boolean
}

export interface PrismaOptions extends BasePrismaOptions {
  typeDefs: string
}
