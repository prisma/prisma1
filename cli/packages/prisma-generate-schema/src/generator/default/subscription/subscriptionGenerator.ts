import { GraphQLFieldConfigMap, GraphQLObjectType } from 'graphql/type'
import { IGQLType, camelCase } from 'prisma-datamodel'
import { FieldConfigUtils, RootGenerator } from '../../generator'

export default class SubscriptionGenerator extends RootGenerator {
  public getTypeName(input: IGQLType[], args: {}) {
    return 'Subscription'
  }

  protected shouldGenerateSubscription(type: IGQLType) {
    return !type.isEnum && !type.isRelationTable
  }

  protected generateInternal(input: IGQLType[], args: {}) {
    const fieldMaps = input
      .filter(type => this.shouldGenerateSubscription(type))
      .map(type => this.generateSubscriptionField(type))

    return new GraphQLObjectType({
      name: this.getTypeName(input, args),
      fields: FieldConfigUtils.merge(...fieldMaps),
    })
  }

  private generateSubscriptionField(model: IGQLType) {
    const fields = {} as GraphQLFieldConfigMap<any, any>

    fields[`${camelCase(model.name)}`] = {
      type: this.generators.modelSubscriptionPayload.generate(model, {}),
      args: {
        where: {
          type: this.generators.modelSubscriptionWhereInput.generate(model, {}),
        },
      },
    }

    return fields
  }
}
