import { SdlExpect, TypeIdentifiers } from 'prisma-datamodel'
import { ModelMerger } from '../../databases/document/modelMerger'

describe('Document model inferring, conflict resolution', () => {
  it('Should merge conflicting models additively.', () => {
    const user1 = {
      lastName: 'Test-1'
    }

    const user2 = {
      firstName: 'Test-2'
    }

    const merger = new ModelMerger('User')

    merger.analyze(user1)
    merger.analyze(user2)

    const { type } = merger.getType()

//    expect(type.fields).toHaveLength(3)

//    SdlExpect.field(type, '_id', false, false, TypeIdentifiers.string, true)
//    SdlExpect.field(type, 'lastName', false, false, TypeIdentifiers.string)
//    SdlExpect.field(type, 'firstName', false, false, TypeIdentifiers.string)
  })
})