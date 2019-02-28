import { GraphQLInputFieldConfigMap, GraphQLNonNull } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import { ModelInputObjectTypeGenerator } from '../../../generator'

export default class ModelUpsertNestedInputGenerator extends ModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpsertNestedInput`
  }

  protected generateWhereUnique(
    model: IGQLType,
    args: {},
    fields: GraphQLInputFieldConfigMap,
  ) {
    // Do nothing - work is done in subclass
  }

  protected generateFields(model: IGQLType, args: {}) {
    const fields = {} as GraphQLInputFieldConfigMap

    this.generateWhereUnique(model, args, fields)

    fields.update = {
      type: new GraphQLNonNull(
        this.generators.modelUpdateDataInput.generate(model, args),
      ),
    }
    fields.create = {
      type: new GraphQLNonNull(
        this.generators.modelCreateInput.generate(model, args),
      ),
    }

    return fields
  }
}
