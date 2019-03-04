import {
  ModelObjectTypeGenerator,
  RelatedGeneratorArgs,
  RelatedModelInputObjectTypeGenerator,
  TypeFromModelGenerator,
} from '../../../generator'
import {
  IGQLType,
  IGQLField,
  camelCase,
  plural,
  capitalize,
} from 'prisma-datamodel'
import {
  GraphQLObjectType,
  GraphQLInputFieldConfigMap,
  GraphQLFieldConfig,
  GraphQLList,
  GraphQLNonNull,
  GraphQLInputObjectType,
  GraphQLString,
} from 'graphql/type'

export abstract class ModelCreateOneOrManyWithoutRelatedInputGenerator extends RelatedModelInputObjectTypeGenerator {
  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {
    return (
      !this.hasFieldsExcept(
        this.getWriteableFields(model.fields),
        (args.relatedField.relatedField as IGQLField).name,
      ) && !this.hasUniqueField(model.fields)
    )
  }

  protected abstract maybeWrapList(
    input: GraphQLInputObjectType,
  ):
    | GraphQLList<GraphQLNonNull<GraphQLInputObjectType>>
    | GraphQLInputObjectType

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (
      this.hasFieldsExcept(
        this.getWriteableFields(model.fields),
        (args.relatedField.relatedField as IGQLField).name,
      )
    ) {
      fields.create = {
        type: this.maybeWrapList(
          this.generators.modelCreateWithoutRelatedInput.generate(model, args),
        ),
      }
    }

    if (this.hasUniqueField(model.fields)) {
      fields.connect = {
        type: this.maybeWrapList(
          this.generators.modelWhereUniqueInput.generate(model, args),
        ),
      }
    }

    return fields
  }
}

// tslint:disable-next-line:max-classes-per-file
export default class ModelCreateManyWithoutRelatedInputGenerator extends ModelCreateOneOrManyWithoutRelatedInputGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}CreateManyWithout${capitalize(field.name)}Input`
  }
  protected maybeWrapList(
    input: GraphQLInputObjectType,
  ):
    | GraphQLList<GraphQLNonNull<GraphQLInputObjectType>>
    | GraphQLInputObjectType {
    return this.generators.scalarTypeGenerator.wrapList(input)
  }
}
