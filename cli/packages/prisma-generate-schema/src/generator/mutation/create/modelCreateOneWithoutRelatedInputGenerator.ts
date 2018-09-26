import { GraphQLInputObjectType } from 'graphql/type'
import { IGQLField, IGQLType } from '../../../datamodel/model'
import { capitalize } from '../../../util/util'
import { RelatedGeneratorArgs } from '../../generator'
import { ModelCreateOneOrManyWithoutRelatedInputGenerator } from './modelCreateManyWithoutRelatedInputGenerator'

export default class ModelCreateOneWithoutRelatedInputGenerator extends ModelCreateOneOrManyWithoutRelatedInputGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}CreateOneWithout${capitalize(field.name)}Input`
  }
  protected maybeWrapList(input: GraphQLInputObjectType) {
    return input
  }
}
