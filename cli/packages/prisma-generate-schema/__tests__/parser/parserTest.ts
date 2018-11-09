import DatamodelParser from '../../src/datamodel/parser'
import { IGQLType } from '../../src/datamodel/model'

/**
 * Assertion helper for fields.
 */
function expectField(
  candidate: IGQLType,
  name: string,
  required: boolean,
  list: boolean,
  type: string | IGQLType,
  defaultValue: any = null,
) {
  const [fieldObj] = candidate.fields.filter(f => f.name === name)

  expect(fieldObj).toBeDefined()

  expect(fieldObj.isRequired).toEqual(required)
  expect(fieldObj.isList).toEqual(list)
  expect(fieldObj.type).toEqual(type)
  expect(fieldObj.defaultValue).toEqual(defaultValue)

  return fieldObj
}

/**
 * Assertion helper for types
 */
function expectType(types: IGQLType[], name: string, isEnum: boolean = false) {
  const [type] = types.filter(t => t.name === name)

  expect(type).toBeDefined()
  expect(type.isEnum).toEqual(isEnum)

  return type
}

test('Parse a type with scalars correctly.', () => {
  const model = `
    type User {
      requiredInt: Int!
      stringList: [String!]!
      optionalDateTime: DateTime
      anotherInt: Int! @default(value: 10)
    }
  `

  const types = DatamodelParser.parseFromSchemaString(model)

  const userType = expectType(types, 'User')

  expectField(userType, 'requiredInt', true, false, 'Int')
  expectField(userType, 'stringList', false, true, 'String')
  expectField(userType, 'optionalDateTime', false, false, 'DateTime')
  expectField(userType, 'anotherInt', true, false, 'Int', '10')
})

test('Parse a type with @embedded directive correctly.', () => {
  const model = `
    type User @embedded {
      requiredInt: Int!
      stringList: [String!]!
      optionalDateTime: DateTime
      anotherInt: Int! @default(value: 10)
    }
  `

  const types = DatamodelParser.parseFromSchemaString(model)

  const userType = expectType(types, 'User')

  expectField(userType, 'requiredInt', true, false, 'Int')
  expectField(userType, 'stringList', false, true, 'String')
  expectField(userType, 'optionalDateTime', false, false, 'DateTime')
  expectField(userType, 'anotherInt', true, false, 'Int', '10')
  expect(userType.isEmbedded).toBe(true)
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

  const types = DatamodelParser.parseFromSchemaString(model)

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

  const types = DatamodelParser.parseFromSchemaString(model)

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
