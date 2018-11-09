import { GraphQLInputFieldConfigMap, GraphQLNonNull } from 'graphql/type'
import { IGQLField, IGQLType } from '../../../../datamodel/model'
import { capitalize } from '../../../../util/util'
import {
  RelatedGeneratorArgs,
  RelatedModelInputObjectTypeGenerator,
} from '../../../generator'

export default class ModelUpsertWithoutRelatedInputGenerator extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpsertWithout${capitalize(field.name)}Input`
  }

  protected generateWhereUnique(
    model: IGQLType,
    args: RelatedGeneratorArgs,
    fields: GraphQLInputFieldConfigMap,
  ) {
    // Do nothing - work is done in subclass
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    this.generateWhereUnique(model, args, fields)

    fields.update = {
      type: new GraphQLNonNull(
        this.generators.modelUpdateWithoutRelatedDataInput.generate(
          model,
          args,
        ),
      ),
    }
    fields.create = {
      type: new GraphQLNonNull(
        this.generators.modelCreateWithoutRelatedInput.generate(model, args),
      ),
    }

    return fields
  }
}
