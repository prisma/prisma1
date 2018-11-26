import DocumentParser from '../../src/datamodel/parser/documentParser'
import { IGQLType } from '../../src/datamodel/model'
import { SdlExpect } from '../../src/test-helpers' 

describe(`Document parser specific tests`, () => {
  test('Mark an ID field correctly.', () => {
    const model = `
      type User {
        id: ID! @id
        anotherInt: Int! @default(value: 10)
      }
    `

    const { types } = new DocumentParser().parseFromSchemaString(model)

    const userType = SdlExpect.type(types, 'User')

    SdlExpect.field(userType, 'id', true, false, 'ID', true, true, null)
  })


  test('Mark an read only fields correctly.', () => {
    const model = `
      type User {
        id: ID! @id
        anotherInt: Int! @default(value: 10)
        wasCreatedAt: Date! @createdAt
        wasUpdatedAt: Date! @updatedAt
      }
    `

    const { types } = new DocumentParser().parseFromSchemaString(model)

    const userType = SdlExpect.type(types, 'User')

    SdlExpect.field(userType, 'id', true, false, 'ID', true, true, null)
    SdlExpect.field(userType, 'wasCreatedAt', true, false, 'Date', false, true, null)
    SdlExpect.field(userType, 'wasUpdatedAt', true, false, 'Date', false, true, null)
  })

  test('Mark an embedded types correctly.', () => {
    const model = `
      type User @embedded {
        email: ID! @id
      }
    `

    const { types } = new DocumentParser().parseFromSchemaString(model)

    const userType = SdlExpect.type(types, 'User', false, true)
  })
})