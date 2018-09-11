import { GraphQLList, GraphQLNonNull, GraphQLString, GraphQLFieldConfigMap } from 'graphql/type'
import { IGQLType, IGQLField } from '../../datamodel/model'
import { ModelObjectTypeGenerator, RootGenerator } from '../generator'

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
    } as GraphQLFieldConfigMap

    if (this.hasScalarField(input.fields)) {
      payload.previousValues = { type: this.generators.modelPreviousValues.generate(input, {}) }
    }

    return payload
  }

  private hasScalarField(fields: IGQLField[]) {
    return fields.filter(field => this.generators.scalarTypeGenerator.isScalarField(field)).length > 0
  }
}
