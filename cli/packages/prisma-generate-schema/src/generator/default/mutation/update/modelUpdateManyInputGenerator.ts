import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, ModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../../generator'
import { IGQLType, IGQLField } from '../../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"


export default class ModelUpdateManyInputTypeGenerator extends ModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateManyInput`
  }

  public wouldBeEmpty(model: IGQLType, args: {}) {
    return this.generators.modelCreateInput.wouldBeEmpty(model, args) &&
      this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)
  }

  protected generateFields(model: IGQLType, args: {}) {
    const fields = {} as GraphQLInputFieldConfigMap

    // TODO: The nonIdFields way for filtering out "to-be" generated fields, can be encapsulated in the respective "wouldBeEmpty" or another helper function
    const nonIdFields = model.fields.filter(field => field.name !== 'id')

    if (!this.generators.modelCreateInput.wouldBeEmpty(model, args)) {
      fields.create = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelCreateInput.generate(model, args)) }
    }

    if (nonIdFields.length > 0 && !this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
      fields.update = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelUpdateWithWhereUniqueNestedInput.generate(model, args)) }
      fields.upsert = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelUpsertWithWhereUniqueNestedInput.generate(model, args)) }
    }
    if (!this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
      fields.delete = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
      if(!model.isEmbedded) {
        fields.connect = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
        fields.disconnect = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
      }
    }
    if (!this.generators.modelScalarWhereInput.wouldBeEmpty(model, args)) {
      fields.deleteMany = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelScalarWhereInput.generate(model, args)) }
      fields.updateMany = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelUpdateManyWithWhereNestedInput.generate(model, args)) }
    }

    return fields
  }
}