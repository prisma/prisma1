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

  static replaceEnv(str: string): string {
    const regex = /\${env:(.*?)}/
    const match = regex.exec(str)
    // tslint:disable-next-line:prefer-conditional-expression
    if (match) {
      return Generator.replaceEnv(
        `${str.slice(0, match.index)}$\{process.env['${match[1]}']}${str.slice(
          match[0].length + match.index,
        )}`,
      )
    } else {
      return `\`${str}\``
    }
  }
}
