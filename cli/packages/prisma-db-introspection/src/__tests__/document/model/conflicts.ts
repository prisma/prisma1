import { SdlExpect, TypeIdentifiers } from 'prisma-datamodel'
import { ModelMerger, ModelSampler } from '../../../databases/document/modelSampler'
import { MockDocumentDataSource } from '../../../test-helpers/mockDataSource';

/**
 * Checks if model sampling and inferring module resolves conflicts in field definitions correctly.
 */
describe('Document model inferring, conflict resolution', () => {
  it('Should merge conflicting models additively.', () => {
    const user1 = {
      lastName: 'Test-1'
    }

    const user2 = {
      firstName: 'Test-2'
    }

    const merger = new ModelMerger('User', false, new MockDocumentDataSource({}))

    merger.analyze(user1)
    merger.analyze(user2)

    const { type } = merger.getType()

    expect(type.fields).toHaveLength(2)

    SdlExpect.field(type, 'lastName', false, false, TypeIdentifiers.string)
    SdlExpect.field(type, 'firstName', false, false, TypeIdentifiers.string)
  })

  it('Should merge conflicting models additively and recursively.', () => {
    const user1 = {
      lastName: 'Test-1',
      shippingAddress: {
        country: 'Germany'
      }
    }

    const user2 = {
      lastName: 'Test-3',
      firstName: 'Test-2',
      shippingAddress: {
        country: 'Germany',
        street: 'Teststreet'
      }
    }

    const user3 = {
      firstName: 'Test-2',
      shippingAddress: {
        street: 'Teststreet',
        houseNumber: 4
      }
    }


    const merger = new ModelMerger('User', false, new MockDocumentDataSource({}))

    merger.analyze(user1)
    merger.analyze(user2)
    merger.analyze(user3)

    const { type, embedded } = merger.getType()

    const embeddedType = SdlExpect.type(embedded, 'UserShippingAddress', false, true)

    expect(type.fields).toHaveLength(3)

    SdlExpect.field(embeddedType, 'country', false, false, TypeIdentifiers.string)
    SdlExpect.field(embeddedType, 'street', false, false, TypeIdentifiers.string)
    SdlExpect.field(embeddedType, 'houseNumber', false, false, TypeIdentifiers.integer)

    SdlExpect.field(type, 'lastName', false, false, TypeIdentifiers.string)
    SdlExpect.field(type, 'firstName', false, false, TypeIdentifiers.string)

    SdlExpect.field(type, 'shippingAddress', false, false, embeddedType)
  })


  it('Should bail on type conflict.', () => {
    const user1 = {
      lastName: 'Test-1',
      shippingAddress: {
        country: 'Germany'
      }
    }

    const user2 = {
      lastName: [false],
      firstName: 'Test-2',
      shippingAddress: {
        country: 'Germany',
        street: 8
      }
    }

    const user3 = {
      firstName: 'Test-2',
      shippingAddress: {
        street: 'Teststreet',
        houseNumber: 4
      }
    }

    const merger = new ModelMerger('User', false, new MockDocumentDataSource({}))

    merger.analyze(user1)
    merger.analyze(user2)
    merger.analyze(user3)

    const { type, embedded } = merger.getType()

    const embeddedType = SdlExpect.type(embedded, 'UserShippingAddress', false, true)

    expect(type.fields).toHaveLength(3)
    
    const conflictingEmbeddedField = SdlExpect.field(embeddedType, 'street', false, false, ModelSampler.ErrorType)
    SdlExpect.error(conflictingEmbeddedField)
    const conflictingField = SdlExpect.field(type, 'lastName', false, false, ModelSampler.ErrorType)
    SdlExpect.error(conflictingField)
  })
})