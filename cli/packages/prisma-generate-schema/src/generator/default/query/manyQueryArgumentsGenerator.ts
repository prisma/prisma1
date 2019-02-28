import { ArgumentsGenerator, RelatedGeneratorArgs } from '../../generator'
import { IGQLField, IGQLType } from 'prisma-datamodel'
import {
  GraphQLFieldConfigArgumentMap,
  GraphQLInt,
  GraphQLString,
} from 'graphql/type'

export default class ManyQueryArgumentsGenerator extends ArgumentsGenerator {
  public generate(model: IGQLType, args: {}) {
    return {
      where: { type: this.generators.modelWhereInput.generate(model, {}) },
      orderBy: { type: this.generators.modelOrderByInput.generate(model, {}) },
      skip: { type: GraphQLInt },
      after: { type: GraphQLString },
      before: { type: GraphQLString },
      first: { type: GraphQLInt },
      last: { type: GraphQLInt },
    } as GraphQLFieldConfigArgumentMap
  }
}
