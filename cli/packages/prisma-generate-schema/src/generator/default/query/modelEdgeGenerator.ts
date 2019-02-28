import { GraphQLNonNull, GraphQLObjectType, GraphQLString } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import { ModelObjectTypeGenerator } from '../../generator'

export default class ModelEdgeGenerator extends ModelObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}Edge`
  }

  protected generateInternal(input: IGQLType, args: {}) {
    return new GraphQLObjectType({
      name: this.getTypeName(input, args),
      fields: {
        node: {
          type: new GraphQLNonNull(this.generators.model.generate(input, {})),
        },
        cursor: { type: new GraphQLNonNull(GraphQLString) },
      },
    })
  }
}
