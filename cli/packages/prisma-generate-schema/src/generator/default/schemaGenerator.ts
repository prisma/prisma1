import { SchemaGeneratorBase } from '../generator'
import { IGQLType } from 'prisma-datamodel'
import { GraphQLSchema } from 'graphql/type'

export default class SchemaGenerator extends SchemaGeneratorBase {
  public getTypeName(input: IGQLType[], args: {}) {
    return '__Schema'
  }
  public generate(input: IGQLType[], args?: {}) {
    return new GraphQLSchema({
      query: this.generators.query.generate(input, {}),
      mutation: this.generators.mutation.generate(input, {}),
      subscription: this.generators.subscription.generate(input, {}),
    })
  }
}
