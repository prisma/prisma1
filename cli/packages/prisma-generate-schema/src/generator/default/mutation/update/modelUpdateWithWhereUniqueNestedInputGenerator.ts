import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, RelatedModelInputObjectTypeGenerator } from '../../../generator'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"


export default class ModelUpdateWithWhereUniqueNestedInput extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}UpdateWithWhereUniqueNestedInput`
  }

  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {  
    return this.generators.modelUpdateDataInput.wouldBeEmpty(model, args) ||
           this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    fields.where = { type: new GraphQLNonNull(this.generators.modelWhereUniqueInput.generate(model, {})) }
    fields.data = { type: new GraphQLNonNull(this.generators.modelUpdateDataInput.generate(model, {})) }

    return fields
  }
}