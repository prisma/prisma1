import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, ModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../../generator'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"


export default class ModelUpdateManyInputTypeGenerator extends ModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateManyInput`
  }

  public wouldBeEmpty(model: IGQLType, args: {}) {

    const writeableFields = this.getWriteableFields(model.fields)

    return this.generators.modelCreateInput.wouldBeEmpty(model, args) &&
      (this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args) || writeableFields.length === 0) &&
      this.generators.modelScalarWhereInput.wouldBeEmpty(model, args) &&
      this.generators.modelUpdateManyWithWhereNestedInput.wouldBeEmpty(model, args)
  }

  protected generateFields(model: IGQLType, args: {}) {
    const fields = {} as GraphQLInputFieldConfigMap

    const writeableFields = this.getWriteableFields(model.fields)

    if (!this.generators.modelCreateInput.wouldBeEmpty(model, args)) {
      fields.create = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelCreateInput.generate(model, args)) }
    }

    if (writeableFields.length > 0 && !this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
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
    }
    if (!this.generators.modelUpdateManyWithWhereNestedInput.wouldBeEmpty(model, args)) {
      fields.updateMany = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelUpdateManyWithWhereNestedInput.generate(model, args)) }
    }

    return fields
  }
}