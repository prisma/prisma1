import { SdlExpect, TypeIdentifiers } from 'prisma-datamodel'
import { ModelMerger, ModelSampler } from '../../../databases/document/modelSampler'
import { MockDocumentDataSource } from '../../../test-helpers/mockDataSource';

/**
 * Checks if model sampling and inferring marks potential relation field correctly.
 */
describe('Document model inferring mark relation fields', () => {
  it('Should mark potential relation fields correctly.', () => {
    const user = {
      _id: 'id',
      fk1: 'Hello',
      fk2: '000000000000000000000000',
      field: 3
    }

    const merger = new ModelMerger('User', false, new MockDocumentDataSource({}))

    merger.analyze(user)

    const { type } = merger.getType()

    expect(type.fields).toHaveLength(4)

    expect(SdlExpect.field(type, '_id', true, false, TypeIdentifiers.string, true).relationName).toBe(null)
    expect(SdlExpect.field(type, 'fk1', false, false, TypeIdentifiers.string).relationName).toBe(ModelSampler.ErrorType)
    expect(SdlExpect.field(type, 'fk2', false, false, TypeIdentifiers.string).relationName).toBe(ModelSampler.ErrorType)
    expect(SdlExpect.field(type, 'field', false, false, TypeIdentifiers.integer).relationName).toBe(null)
  })
})