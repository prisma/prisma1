import {
  GraphQLBoolean,
  GraphQLNonNull,
  GraphQLObjectType,
  GraphQLString,
} from 'graphql/type'
import { AuxillaryObjectTypeGenerator } from '../../generator'

export default class PageInfoGenerator extends AuxillaryObjectTypeGenerator {
  public getTypeName(input: null, args: {}) {
    return 'PageInfo'
  }

  protected generateInternal(input: null, args: {}) {
    return new GraphQLObjectType({
      name: this.getTypeName(input, args),
      fields: {
        hasNextPage: { type: new GraphQLNonNull(GraphQLBoolean) },
        hasPreviousPage: { type: new GraphQLNonNull(GraphQLBoolean) },
        startCursor: { type: GraphQLString },
        endCursor: { type: GraphQLString },
      },
    })
  }
}
