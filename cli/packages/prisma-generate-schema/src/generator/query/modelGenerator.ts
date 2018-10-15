import { ModelObjectTypeGenerator } from '../generator'
import { IGQLType, IGQLField } from '../../datamodel/model'

export default class ModelGenerator extends ModelObjectTypeGenerator {

  public getTypeName(input: IGQLType, args: {}) {
    return input.name
  }

  protected generateRelationField(model: IGQLType, a: {}, field: IGQLField) {
    const argumentsList = field.isList ?
      this.generators.manyQueryArguments.generate(field.type as IGQLType, { relatedField: field, relatedType: model, relationName: null }) :
      this.generators.oneQueryArguments.generate(field.type as IGQLType, { relatedField: field, relatedType: model, relationName: null })
    return {
      type: this.generators.scalarTypeGenerator.wraphWithModifiers(field, this.generators.model.generate(field.type as IGQLType, {})),
      args: argumentsList
    }
  }
}