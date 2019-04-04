import {
  ModelObjectTypeGenerator,
  RelatedGeneratorArgs,
  RelatedModelInputObjectTypeGenerator,
  TypeFromModelGenerator,
} from '../../../generator'
import { IGQLType, IGQLField, plural, capitalize } from 'prisma-datamodel'
import {
  GraphQLObjectType,
  GraphQLInputFieldConfigMap,
  GraphQLFieldConfig,
  GraphQLList,
  GraphQLNonNull,
  GraphQLInputObjectType,
  GraphQLString,
} from 'graphql/type'

export default class ModelUpdateOneWithoutRelatedInputTypeGenerator extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpdateOneRequiredWithout${capitalize(field.name)}Input`
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (
      this.hasFieldsExcept(
        this.getCreateInputFields(model.fields),
        (args.relatedField.relatedField as IGQLField).name,
      )
    ) {
      fields.create = {
        type: this.generators.modelCreateWithoutRelatedInput.generate(
          model,
          args,
        ),
      }
    }

    if (
      this.hasFieldsExcept(
        this.getWriteableFields(model.fields),
        (args.relatedField.relatedField as IGQLField).name,
      )
    ) {
      fields.update = {
        type: this.generators.modelUpdateWithoutRelatedDataInput.generate(
          model,
          args,
        ),
      }
      fields.upsert = {
        type: this.generators.modelUpsertWithoutRelatedInput.generate(
          model,
          args,
        ),
      }
    }

    if (!this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
      fields.connect = {
        type: this.generators.modelWhereUniqueInput.generate(model, args),
      }
    }

    return fields
  }
}
