import RelationalParser from '../../src/datamodel/relationalParser'
import DocumentParser from '../../src/datamodel/documentParser'
import { IGQLType } from '../../src/datamodel/model'
import { expectField, expectType } from './helpers' 

const parsersToTest = [{ name: 'relational', instance: new RelationalParser()}, { name: 'document', instance: new DocumentParser()}]

for(const parser of parsersToTest) {
  describe(`${parser.name} parser basic tests`, () => {
    test('Parse a type with scalars correctly.', () => {
      const model = `
        type User {
          requiredInt: Int!
          stringList: [String!]!
          optionalDateTime: DateTime
          anotherInt: Int! @default(value: 10)
        }
      `

      const types = parser.instance.parseFromSchemaString(model)

      const userType = expectType(types, 'User')

      expectField(userType, 'requiredInt', true, false, 'Int')
      expectField(userType, 'stringList', false, true, 'String')
      expectField(userType, 'optionalDateTime', false, false, 'DateTime')
      expectField(userType, 'anotherInt', true, false, 'Int', false, false,  '10')
    })

    test('Parse a type with an enum correctly.', () => {
      const model = `
        type User {
          enumField: UserRole!
        }

        enum UserRole {
          user,
          admin,
          mod
        }
      `

      const types = parser.instance.parseFromSchemaString(model)

      const userType = expectType(types, 'User')
      const userRoleEnum = expectType(types, 'UserRole', true)

      expectField(userType, 'enumField', true, false, userRoleEnum)

      expectField(userRoleEnum, 'user', false, false, 'String')
      expectField(userRoleEnum, 'admin', false, false, 'String')
      expectField(userRoleEnum, 'mod', false, false, 'String')
    })

    test('Connect relations correctly.', () => {
      const model = `
        type A {
          b: B
          c: C @relation(nane: "relation")
        }

        type B {
          a: A
          c: C
        }

        type C {
          a: A @relation(nane: "relation")
        }
      `

      const types = parser.instance.parseFromSchemaString(model)

      const A = expectType(types, 'A')
      const B = expectType(types, 'B')
      const C = expectType(types, 'C')

      const Ab = expectField(A, 'b', false, false, B)
      const Ac = expectField(A, 'c', false, false, C)

      const Ba = expectField(B, 'a', false, false, A)
      const Bc = expectField(B, 'c', false, false, C)

      const Ca = expectField(C, 'a', false, false, A)

      expect(Ab.relatedField).toEqual(Ba)
      expect(Ac.relatedField).toEqual(Ca)

      expect(Ba.relatedField).toEqual(Ab)
      expect(Bc.relatedField).toEqual(null)

      expect(Ca.relatedField).toEqual(Ac)
    })
  })
}