import { GraphQLInt, GraphQLNonNull, GraphQLObjectType } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import { ModelObjectTypeGenerator } from '../../generator'

export default class AggregateModelGenerator extends ModelObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `Aggregate${input.name}`
  }

  protected generateInternal(input: IGQLType, args: {}) {
    return new GraphQLObjectType({
      name: this.getTypeName(input, args),
      fields: {
        count: { type: new GraphQLNonNull(GraphQLInt) },
      },
    })
  }
}
