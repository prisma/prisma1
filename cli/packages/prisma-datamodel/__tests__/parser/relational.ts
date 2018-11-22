import { IGQLType } from '../../src/datamodel/model'
import { expectField, expectType } from './helpers' 
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

    const userType = expectType(types, 'User')

    expectField(userType, 'id', true, false, 'ID', true, true, null)
  })


  test('Mark an read only fields correctly.', () => {
    const model = `
      type User {
        id: ID! @id
        anotherInt: Int! @default(value: 10)
        createdAt: Date!
        updatedAt: Date!
      }
    `

    const { types } = new RelationalParser().parseFromSchemaString(model)

    const userType = expectType(types, 'User')
    expectField(userType, 'id', true, false, 'ID', true, true, null)
    expectField(userType, 'createdAt', true, false, 'Date', false, true, null)
    expectField(userType, 'updatedAt', true, false, 'Date', false, true, null)
  })
})