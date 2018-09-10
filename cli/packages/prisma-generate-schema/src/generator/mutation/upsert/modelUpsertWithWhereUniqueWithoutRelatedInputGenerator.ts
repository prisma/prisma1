import { GraphQLInputFieldConfigMap, GraphQLNonNull } from 'graphql/type'
import { IGQLField, IGQLType } from '../../../datamodel/model'
import { capitalize } from '../../../util/util'
import { RelatedGeneratorArgs } from '../../generator'
import ModelUpsertWithoutRelatedInputGenerator from './modelUpsertWithoutRelatedInputGenerator'

export default class ModelUpsertWithWhereUniqueWithoutRelatedInputGenerator extends ModelUpsertWithoutRelatedInputGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpsertWithWhereUniqueWithout${capitalize(
      field.name,
    )}Input`
  }

  protected generateWhereUnique(
    model: IGQLType,
    args: RelatedGeneratorArgs,
    fields: GraphQLInputFieldConfigMap,
  ) {
    fields.where = {
      type: new GraphQLNonNull(
        this.generators.modelWhereUniqueInput.generate(model, {}),
      ),
    }
  }
}
