import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../../generator'
import { IGQLType, IGQLField, plural, capitalize } from 'prisma-datamodel'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"

export default class ModelUpdateManyWithoutRelatedInputTypeGenerator extends RelatedModelInputObjectTypeGenerator {

  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {
    return this.generators.modelCreateWithoutRelatedInput.wouldBeEmpty(model, args) &&
      this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)
  }


  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpdateManyWithout${capitalize(field.name)}Input`
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (!this.generators.modelCreateWithoutRelatedInput.wouldBeEmpty(model, args)) {
      fields.create = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelCreateWithoutRelatedInput.generate(model, args)) }
    }

    if (!this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
      fields.delete = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
      fields.connect = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
      fields.set = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
      fields.disconnect = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
    }
    if(!this.generators.modelUpdateWithWhereUniqueWithoutRelatedInput.wouldBeEmpty(model, args)) {
      fields.update = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelUpdateWithWhereUniqueWithoutRelatedInput.generate(model, args)) }
      fields.upsert = { type: this.generators.scalarTypeGenerator.wrapList(this.generators.modelUpsertWithWhereUniqueWithoutRelatedInput.generate(model, args)) }
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