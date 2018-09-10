import { GraphQLEnumType } from 'graphql/type'
import { AuxillaryObjectTypeGenerator } from '../generator'

export default class MutationTypeGenerator extends AuxillaryObjectTypeGenerator {
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
