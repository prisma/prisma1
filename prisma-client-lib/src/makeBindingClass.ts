import { GraphQLSchema } from 'graphql'
import { Binding as BaseBinding } from './Binding'
import { BindingWithoutSchemaOptions } from './types'

export function makeBindingClass<T>({ schema }: { schema: GraphQLSchema }): T {
  return class Binding extends BaseBinding {
    constructor(options?: BindingWithoutSchemaOptions) {
      super({ schema, ...options })
    }
  } as any
}
