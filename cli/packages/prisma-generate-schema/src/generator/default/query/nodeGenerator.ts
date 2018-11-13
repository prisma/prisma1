import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, AuxillaryObjectTypeGenerator, AuxillaryInterfaceGenerator } from '../../generator'
import { GraphQLInterfaceType, GraphQLID, GraphQLNonNull } from "graphql/type"


export default class NodeGenerator extends AuxillaryInterfaceGenerator {
  public getTypeName(input: null, args: {}) {
    return 'Node'
  }
  protected generateInternal(input: null, args: {}) {
    return new GraphQLInterfaceType({
      name: this.getTypeName(input, args),
      fields: {
        id: { type: new GraphQLNonNull(GraphQLID) }
      }
    })
  }

}