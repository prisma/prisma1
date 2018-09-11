import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import { camelCase, plural, capitalize } from '../../../util/util';


export abstract class ModelCreateOneOrManyWithoutRelatedInputGenerator extends RelatedModelInputObjectTypeGenerator {

  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {
    return !TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields, (args.relatedField.relatedField as IGQLField).name) &&
      !TypeFromModelGenerator.hasUniqueField(model.fields)
  }

  protected abstract maybeWrapList(input: GraphQLInputObjectType): GraphQLInputObjectType

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields, (args.relatedField.relatedField as IGQLField).name)) {
      fields.create = { type: this.maybeWrapList(this.generators.modelCreateWithoutRelatedInput.generate(model, args)) }
    }

    if (TypeFromModelGenerator.hasUniqueField(model.fields)) {
      fields.connect = { type: this.maybeWrapList(this.generators.modelWhereUniqueInput.generate(model, args)) }
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
  protected maybeWrapList(input: GraphQLInputObjectType) {
    return this.generators.scalarTypeGenerator.wrapList(input)
  }
}