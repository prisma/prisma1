import { GraphQLInputFieldConfigMap, GraphQLNonNull } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import ModelUpsertNestedInputGenerator from './modelUpsertNestedInputGenerator'

export default class ModelUpsertWithWhereUniqueNestedInputGenerator extends ModelUpsertNestedInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpsertWithWhereUniqueNestedInput`
  }

  protected generateWhereUnique(
    model: IGQLType,
    args: {},
    fields: GraphQLInputFieldConfigMap,
  ) {
    fields.where = {
      type: new GraphQLNonNull(
        this.generators.modelWhereUniqueInput.generate(model, {}),
      ),
    }
  }
}
