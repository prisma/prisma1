import { IGQLType, IGQLField, ISDL } from 'prisma-datamodel'

import ModelNameNormalizer from './modelNameNormalizer'
//+ 30 mins

export default class ModelNameAndDirectiveNormalizer extends ModelNameNormalizer {
  private baseModel: ISDL
  
  constructor(baseModel: ISDL) {
    super()
    this.baseModel = baseModel
  }

  // https://github.com/prisma/prisma/issues/3725
  public normalize(model: ISDL) {
    super.normalize(model)
  }

  protected normalizeType(type: IGQLType) {
    super.normalizeType(type)
  }

  protected normalizeField(field: IGQLField) {
    super.normalizeField(field)
  }
  // TODO: Only invoke renaming for new fields.   
}