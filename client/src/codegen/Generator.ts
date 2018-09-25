import { GraphQLSchema } from 'graphql'
import flatten from './utils/flatten'
import { interleave } from './utils/interleave'
import { Interpolation } from './types'

export class Generator {
  schema: GraphQLSchema

  constructor({ schema }: { schema: GraphQLSchema }) {
    this.schema = schema
  }
  compile(
    strings: TemplateStringsArray,
    ...interpolations: Interpolation<Generator>[]
  ) {
    return flatten<Generator>(interleave(strings, interpolations), this).join(
      '',
    )
  }
}
