import { IGQLField, IGQLType } from '../../../datamodel/model'
import { GraphQLInputFieldConfigMap } from "graphql/type"
import ModelWhereInputGenerator from './modelWhereInputGenerator'
import { TypeFromModelGenerator } from '../../generator';

export default class ModelScalarWhereInputGenerator extends ModelWhereInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}ScalarWhereInput`
  }

  public wouldBeEmpty(input: IGQLType, args: {}) {
    return !this.hasScalarFields(input.fields)
  }

  public generateRelationFilterFields(field: IGQLField): GraphQLInputFieldConfigMap | null {
    return null;
  }
}