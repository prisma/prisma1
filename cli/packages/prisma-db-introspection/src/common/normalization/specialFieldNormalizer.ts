import { RelationalParser, ISDL, IGQLField, IGQLType } from 'prisma-datamodel'
import { INormalizer } from './normalizer'

export class SpecialFieldNormalizer implements INormalizer {
  protected baseModel: ISDL

  constructor(baseModel: ISDL) {
    this.baseModel = baseModel
  }

  public normalizeType(type: IGQLType, ref: IGQLType) { 
    for(const reservedField of [RelationalParser.createdAtFieldName, RelationalParser.updatedAtFieldName]) {
      const field = type.fields.find(x => x.name === reservedField)
      const refField = ref.fields.find(x => x.name === reservedField)

      // Remove reserved field if not found in ref type.
      if(field !== undefined && refField === undefined) {
        type.fields = type.fields.filter(x => x !== field)
      }
    }
  }

  public normalize(model: ISDL) {
    for(const type of model.types) {
      // We should move all tooling for finding types or fields into some common class.
      const ref = this.baseModel.types.find(x => x.name === type.name || x.databaseName === type.name)
      if(ref !== undefined) {
        this.normalizeType(type, ref)
      }
    }
  }
}