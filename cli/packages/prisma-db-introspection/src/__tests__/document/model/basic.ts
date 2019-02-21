import { SdlExpect, TypeIdentifiers } from 'prisma-datamodel'
import {
  ModelMerger,
  ModelSampler,
} from '../../../databases/document/modelSampler'
import { MockDocumentDataSource } from '../../../test-helpers/mockDataSource'

/**
 * Checks if model sampling and inferring module resolves fields an embedded types correctly.
 */
describe('Basic document model inferring', () => {
  it('Should create the sdl description for a simple model correctly.', () => {
    const document = {
      _id: 0,
      street: 'Test-Street',
      houseNumber: 3,
      rating: 5.7,
    }

    const merger = new ModelMerger(
      'document',
      false,
      new MockDocumentDataSource({}),
    )

    merger.analyze(document)

    const { type, embedded } = merger.getType()

    expect(embedded).toHaveLength(0)
    expect(type.name).toBe('document')
    expect(type.isEmbedded).toBe(false)
    expect(type.isEnum).toBe(false)

    expect(type.directives).toHaveLength(0)
    expect(type.comments).toHaveLength(0)

    expect(type.fields).toHaveLength(4)

    SdlExpect.field(type, '_id', true, false, TypeIdentifiers.integer, true)
    SdlExpect.field(type, 'street', false, false, TypeIdentifiers.string)
    SdlExpect.field(type, 'houseNumber', false, false, TypeIdentifiers.integer)
    SdlExpect.field(type, 'rating', false, false, TypeIdentifiers.float)
  })

  it('Should create types for arrays correctly', () => {
    const arrays = {
      _id: 'this-is-so-unique',
      floatArray: [0, 1.2, 5],
      stringArray: ['hello'],
      mixedArray: [0, 'hello', false],
      nestedArray: [[0, 1], [2, 3]],
    }

    const merger = new ModelMerger(
      'arrays',
      false,
      new MockDocumentDataSource({}),
    )

    merger.analyze(arrays)

    const { type } = merger.getType()

    expect(type.name).toBe('arrays')

    SdlExpect.field(type, '_id', true, false, TypeIdentifiers.string, true)
    SdlExpect.field(type, 'stringArray', false, true, TypeIdentifiers.string)
    SdlExpect.field(type, 'floatArray', false, true, TypeIdentifiers.float)
    const mixedField = SdlExpect.field(
      type,
      'mixedArray',
      false,
      true,
      ModelSampler.ErrorType,
    )
    SdlExpect.error(mixedField)
    const nestedField = SdlExpect.field(
      type,
      'nestedArray',
      false,
      true,
      ModelSampler.ErrorType,
    )
    SdlExpect.error(nestedField)
  })

  it('Should create embedded types correctly', () => {
    const customer = {
      _id: 1,
      customer: 'Hugo',
      shippingAddress: {
        street: 'Test-Street',
        number: 3,
      },
    }

    const merger = new ModelMerger(
      'Customer',
      false,
      new MockDocumentDataSource({}),
    )

    merger.analyze(customer)

    const { type, embedded } = merger.getType()

    expect(embedded).toHaveLength(1)

    const embeddedType = SdlExpect.type(
      embedded,
      'CustomerShippingAddress',
      false,
      true,
    )

    expect(type.name).toBe('Customer')
    expect(type.isEmbedded).toBe(false)
    expect(type.fields).toHaveLength(3)

    SdlExpect.field(type, '_id', true, false, TypeIdentifiers.integer, true)
    SdlExpect.field(type, 'customer', false, false, TypeIdentifiers.string)
    SdlExpect.field(type, 'shippingAddress', false, false, embeddedType)

    expect(embeddedType.fields).toHaveLength(2)

    SdlExpect.field(
      embeddedType,
      'street',
      false,
      false,
      TypeIdentifiers.string,
    )
    SdlExpect.field(
      embeddedType,
      'number',
      false,
      false,
      TypeIdentifiers.integer,
    )
  })

  it('Should create embedded array types correctly', () => {
    const customer = {
      _id: 1,
      customer: 'Hugo',
      orders: [
        {
          item: 'Laptop',
          count: 3,
        },
        {
          item: 'Wok',
          count: 1,
        },
      ],
    }

    const merger = new ModelMerger(
      'Customer',
      false,
      new MockDocumentDataSource({}),
    )

    merger.analyze(customer)

    const { type, embedded } = merger.getType()

    expect(embedded).toHaveLength(1)

    const embeddedType = SdlExpect.type(embedded, 'CustomerOrders', false, true)

    expect(type.name).toBe('Customer')
    expect(type.isEmbedded).toBe(false)
    expect(type.fields).toHaveLength(3)

    SdlExpect.field(type, '_id', true, false, TypeIdentifiers.integer, true)
    SdlExpect.field(type, 'customer', false, false, TypeIdentifiers.string)
    SdlExpect.field(type, 'orders', false, true, embeddedType)

    expect(embeddedType.fields).toHaveLength(2)

    SdlExpect.field(embeddedType, 'item', false, false, TypeIdentifiers.string)
    SdlExpect.field(
      embeddedType,
      'count',
      false,
      false,
      TypeIdentifiers.integer,
    )
  })
})
