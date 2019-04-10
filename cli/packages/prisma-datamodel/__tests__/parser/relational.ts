import { IGQLType } from '../../src/datamodel/model'
import { SdlExpect } from '../../src/test-helpers'
import RelationalParser from '../../src/datamodel/parser/relationalParser'

describe(`Relational parser specific tests`, () => {
  test('Find an ID field correctly.', () => {
    const model = `
      type User {
        id: ID! @id
        anotherInt: Int! @default(value: 10)
      }
    `

    const { types } = new RelationalParser().parseFromSchemaString(model)

    const userType = SdlExpect.type(types, 'User')

    SdlExpect.field(userType, 'id', true, false, 'ID', true, true, null)
  })

  test('Mark an read only fields correctly.', () => {
    const model = `
      type User {
        id: ID! @id
        anotherInt: Int! @default(value: 10)
        signedUp: Date! @createdAt
        updatedAt: Date!
      }
    `

    const { types } = new RelationalParser().parseFromSchemaString(model)

    const userType = SdlExpect.type(types, 'User')
    SdlExpect.field(userType, 'id', true, false, 'ID', true, true, null)
    SdlExpect.field(
      userType,
      'signedUp',
      true,
      false,
      'Date',
      false,
      true,
      null,
    )
    SdlExpect.field(
      userType,
      'updatedAt',
      true,
      false,
      'Date',
      false,
      true,
      null,
    )
  })

  test('Respect pg-specific db directive.', () => {
    const model = `
      type User {
        id: ID! @id
        anotherInt: Int! @pgColumn(name: "databaseInt")
      }
    `

    const { types } = new RelationalParser().parseFromSchemaString(model)

    const userType = SdlExpect.type(types, 'User')

    SdlExpect.field(userType, 'id', true, false, 'ID', true, true, null)
    const intField = SdlExpect.field(userType, 'anotherInt', true, false, 'Int')

    expect(intField.databaseName).toBe('databaseInt')
  })
})
