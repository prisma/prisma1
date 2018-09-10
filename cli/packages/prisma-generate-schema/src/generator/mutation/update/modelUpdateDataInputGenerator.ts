import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLFieldConfigMap, GraphQLFieldConfig, GraphQLList, GrqphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import ModelUpdateInputGenerator from './modelUpdateInputGenerator';


export default class ModelUpdateDataInputGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateDataInput`
  }
}