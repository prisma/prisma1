import { GraphQLList, GraphQLNonNull, GraphQLObjectType } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import { ModelObjectTypeGenerator } from '../../generator'

export default class ModelConnectionGenerator extends ModelObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}Connection`
  }

  protected generateInternal(input: IGQLType, args: {}) {
    return new GraphQLObjectType({
      name: this.getTypeName(input, args),
      fields: {
        pageInfo: {
          type: new GraphQLNonNull(this.generators.pageInfo.generate(null, {})),
        },
        edges: {
          type: new GraphQLNonNull(
            new GraphQLList(this.generators.modelEdge.generate(input, {})),
          ),
        },
        aggregate: {
          type: new GraphQLNonNull(
            this.generators.aggregateModel.generate(input, {}),
          ),
        },
      },
    })
  }
}
