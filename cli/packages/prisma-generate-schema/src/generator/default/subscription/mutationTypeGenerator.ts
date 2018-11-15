import { GraphQLEnumType } from 'graphql/type'
import { AuxillaryEnumGenerator } from '../../generator'

export default class MutationTypeGenerator extends AuxillaryEnumGenerator {
  public getTypeName(input: null, args: {}) {
    return 'MutationType'
  }

  protected generateInternal(input: null, args: {}) {
    return new GraphQLEnumType({
      name: this.getTypeName(input, args),
      values: {
        CREATED: {},
        UPDATED: {},
        DELETED: {},
      },
    })
  }
}
