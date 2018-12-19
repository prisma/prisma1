import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, RelatedModelInputObjectTypeGenerator } from '../../../generator'
import { IGQLType, IGQLField, capitalize, plural } from 'prisma-datamodel'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"

export default class ModelUpdateWithWhereUniqueWithoutRelatedInput extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpdateWithWhereUniqueWithout${capitalize(field.name)}Input`
  }

  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {  
    return this.generators.modelUpdateWithoutRelatedDataInput.wouldBeEmpty(model, args) ||
           this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    fields.where = { type: new GraphQLNonNull(this.generators.modelWhereUniqueInput.generate(model, {})) }
    fields.data = { type: new GraphQLNonNull(this.generators.modelUpdateWithoutRelatedDataInput.generate(model, args)) }

    return fields
  }
}