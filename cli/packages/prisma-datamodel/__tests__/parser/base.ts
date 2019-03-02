import RelationalParser from '../../src/datamodel/parser/relationalParser'
import DocumentParser from '../../src/datamodel/parser/documentParser'
import { IGQLType } from '../../src/datamodel/model'
import { SdlExpect } from '../../src/test-helpers'

const parsersToTest = [
  { name: 'relational', instance: new RelationalParser() },
  { name: 'document', instance: new DocumentParser() },
]

for (const parser of parsersToTest) {
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

      const { types } = parser.instance.parseFromSchemaString(model)

      const userType = SdlExpect.type(types, 'User')

      SdlExpect.field(userType, 'requiredInt', true, false, 'Int')
      SdlExpect.field(userType, 'stringList', false, true, 'String')
      SdlExpect.field(userType, 'optionalDateTime', false, false, 'DateTime')
      SdlExpect.field(
        userType,
        'anotherInt',
        true,
        false,
        'Int',
        false,
        false,
        '10',
      )
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

      const { types } = parser.instance.parseFromSchemaString(model)

      const userType = SdlExpect.type(types, 'User')
      const userRoleEnum = SdlExpect.type(types, 'UserRole', true)

      SdlExpect.field(userType, 'enumField', true, false, userRoleEnum)

      SdlExpect.field(userRoleEnum, 'user', false, false, 'String')
      SdlExpect.field(userRoleEnum, 'admin', false, false, 'String')
      SdlExpect.field(userRoleEnum, 'mod', false, false, 'String')
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

      const { types } = parser.instance.parseFromSchemaString(model)

      const A = SdlExpect.type(types, 'A')
      const B = SdlExpect.type(types, 'B')
      const C = SdlExpect.type(types, 'C')

      const Ab = SdlExpect.field(A, 'b', false, false, B)
      const Ac = SdlExpect.field(A, 'c', false, false, C)

      const Ba = SdlExpect.field(B, 'a', false, false, A)
      const Bc = SdlExpect.field(B, 'c', false, false, C)

      const Ca = SdlExpect.field(C, 'a', false, false, A)

      expect(Ab.relatedField).toEqual(Ba)
      expect(Ac.relatedField).toEqual(Ca)

      expect(Ba.relatedField).toEqual(Ab)
      expect(Bc.relatedField).toEqual(null)

      expect(Ca.relatedField).toEqual(Ac)
    })
  })
}
