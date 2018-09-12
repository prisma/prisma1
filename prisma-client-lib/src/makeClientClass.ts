import { GraphQLSchema } from 'graphql'
import { Client as BaseClient } from './Client'
import { ClientWithoutSchemaOptions } from './types'

export function makeClientClass<T>({ schema }: { schema: GraphQLSchema }): T {
  return class Client extends BaseClient {
    constructor(options?: ClientWithoutSchemaOptions) {
      super({ schema, ...options })
    }
  } as any
}
