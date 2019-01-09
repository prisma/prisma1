import { GraphQLSchema } from 'graphql'
import flatten from './utils/flatten'
import { interleave } from './utils/interleave'
import { Interpolation } from './types'
import { IGQLType } from 'prisma-datamodel'

export interface GeneratorInput {
  schema: GraphQLSchema
  internalTypes: IGQLType[]
}

export class Generator {
  schema: GraphQLSchema
  internalTypes: IGQLType[] = []

  constructor({ schema, internalTypes }: GeneratorInput) {
    this.internalTypes = internalTypes
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
