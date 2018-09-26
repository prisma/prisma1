import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import { plural, capitalize } from '../../../util/util';


export default class ModelUpdateOneWithoutRelatedInputTypeGenerator extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = (args.relatedField.relatedField as IGQLField)
    return `${input.name}UpdateOneWithout${capitalize(field.name)}Input`
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields, (args.relatedField.relatedField as IGQLField).name)) {
      fields.create = { type: this.generators.modelCreateWithoutRelatedInput.generate(model, args) }
      fields.update = { type: this.generators.modelUpdateWithoutRelatedDataInput.generate(model, args) }
      fields.upsert = { type: this.generators.modelUpsertWithoutRelatedInput.generate(model, args) }
    }

    fields.delete = { type: this.generators.scalarTypeGenerator.generate('Boolean', {}) }

    if (!args.relatedField.isRequired || args.relatedField.isList) {
      fields.disconnect = { type: this.generators.scalarTypeGenerator.generate('Boolean', {}) }
    }

    if (!this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
      fields.connect = { type: this.generators.modelWhereUniqueInput.generate(model, args) }
    }
    return fields
  }
}