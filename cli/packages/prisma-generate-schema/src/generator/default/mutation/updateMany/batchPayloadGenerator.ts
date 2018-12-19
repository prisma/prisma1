import { GraphQLNonNull, GraphQLObjectType } from 'graphql/type'
import { AuxillaryObjectTypeGenerator } from '../../../generator'
import { TypeIdentifiers } from 'prisma-datamodel'

export default class BatchPayloadGenerator extends AuxillaryObjectTypeGenerator {
  public getTypeName(input: null, args: {}) {
    return 'BatchPayload'
  }

  protected generateInternal(input: null, args: {}) {
    return new GraphQLObjectType({
      name: this.getTypeName(input, args),
      fields: {
        // TODO: Replace with custom long type.
        count: {
          type: new GraphQLNonNull(
            this.generators.scalarTypeGenerator.generate(
              TypeIdentifiers.long,
              {},
            ),
          ),
        },
      },
    })
  }
}
