import {
  GraphQLList,
  GraphQLNonNull,
  GraphQLString,
  GraphQLFieldConfigMap,
} from 'graphql/type'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import { ModelObjectTypeGenerator, RootGenerator } from '../../generator'

export default class ModelSubscriptionPayloadGenerator extends ModelObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}SubscriptionPayload`
  }

  protected generateFields(input: IGQLType, args: {}) {
    const payload = {
      mutation: {
        type: new GraphQLNonNull(
          this.generators.mutationType.generate(null, {}),
        ),
      },
      node: { type: this.generators.model.generate(input, {}) },
      updatedFields: {
        type: new GraphQLList(new GraphQLNonNull(GraphQLString)),
      },
    } as GraphQLFieldConfigMap<any, any>

    if (this.hasScalarFields(input.fields)) {
      payload.previousValues = {
        type: this.generators.modelPreviousValues.generate(input, {}),
      }
    }

    return payload
  }
}
