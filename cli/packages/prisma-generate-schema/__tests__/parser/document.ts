import DocumentParser from '../../src/datamodel/documentParser'
import { IGQLType } from '../../src/datamodel/model'
import { expectField, expectType } from './helpers' 

describe(`Document parser specific tests`, () => {
  test('Mark an ID field correctly.', () => {
    const model = `
      type User {
        email: String! @id
        anotherInt: Int! @default(value: 10)
      }
    `

    const types = new DocumentParser().parseFromSchemaString(model)

    const userType = expectType(types, 'User')

    expectField(userType, 'email', true, false, 'String', true, true, null)
  })


  test('Mark an read only fields correctly.', () => {
    const model = `
      type User {
        email: String! @id
        anotherInt: Int! @default(value: 10)
        wasCreatedAt: Date! @createdAt
        wasUpdatedAt: Date! @updatedAt
      }
    `

    const types = new DocumentParser().parseFromSchemaString(model)

    const userType = expectType(types, 'User')

    expectField(userType, 'email', true, false, 'String', true, true, null)
    expectField(userType, 'wasCreatedAt', true, false, 'String', false, true, null)
    expectField(userType, 'wasUpdatedAt', true, false, 'String', false, true, null)
  })

  test('Mark an embedded types correctly.', () => {
    const model = `
      @embedded
      type User {
        email: String! @id
      }
    `

    const types = new DocumentParser().parseFromSchemaString(model)

    const userType = expectType(types, 'User', true)
  })
})