import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, RelatedModelInputObjectTypeGenerator } from '../../../generator'
import { IGQLType, IGQLField } from '../../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import { capitalize, plural } from '../../../../util/util';


export default class ModelUpdateWithWhereUniqueWithoutRelatedInput extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpdateWithWhereUniqueWithout${capitalize(field.name)}Input`
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    fields.where = { type: new GraphQLNonNull(this.generators.modelWhereUniqueInput.generate(model, {})) }
    fields.data = { type: new GraphQLNonNull(this.generators.modelUpdateWithoutRelatedDataInput.generate(model, args)) }

    return fields
  }
}