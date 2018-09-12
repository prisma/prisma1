import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, RelatedModelInputObjectTypeGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLFieldConfigMap, GraphQLFieldConfig, GraphQLList, GrqphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import ModelUpdateInputGenerator from './modelUpdateInputGenerator';
import { capitalize, plural } from '../../../util/util';


export default class ModelUpdateWithoutRelatedInputGenerator extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpdateWithout${capitalize(field.name)}DataInput`
  }

  protected generateScalarFieldType(model: IGQLType, args: {}, field: IGQLField) {
    return ModelUpdateInputGenerator.generateScalarFieldTypeForInputType(model, field, this.generators)
  }

  protected generateRelationFieldType(model: IGQLType, args: RelatedGeneratorArgs, field: IGQLField) {
    if (field.relatedField === args.relatedField) {
      return null
    }
    return ModelUpdateInputGenerator.generateRelationFieldForInputType(model, field, this.generators)
  }
}