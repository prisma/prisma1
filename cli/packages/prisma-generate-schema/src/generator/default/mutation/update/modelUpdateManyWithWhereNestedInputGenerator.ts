import {
  ModelObjectTypeGenerator,
  RelatedGeneratorArgs,
  IGenerators,
  RelatedModelInputObjectTypeGenerator,
} from '../../../generator'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import {
  GraphQLObjectType,
  GraphQLInputFieldConfigMap,
  GraphQLFieldConfig,
  GraphQLList,
  GraphQLNonNull,
  GraphQLInputObjectType,
  GraphQLString,
} from 'graphql/type'

export default class ModelUpdateManyWithWhereNestedInputGenerator extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}UpdateManyWithWhereNestedInput`
  }

  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {
    return this.generators.modelUpdateManyDataInput.wouldBeEmpty(model, args)
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    fields.where = {
      type: new GraphQLNonNull(
        this.generators.modelScalarWhereInput.generate(model, {}),
      ),
    }
    fields.data = {
      type: new GraphQLNonNull(
        this.generators.modelUpdateManyDataInput.generate(model, {}),
      ),
    }

    return fields
  }
}
