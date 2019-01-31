import RelationalParser from '../../src/datamodel/parser/relationalParser'
import DocumentParser from '../../src/datamodel/parser/documentParser'
import { IGQLType } from '../../src/datamodel/model'
import { SdlExpect } from '../../src/test-helpers' 
import { TypeIdentifiers } from '../../src/datamodel/scalar';

const parsersToTest = [{ name: 'relational', instance: new RelationalParser()}, { name: 'document', instance: new DocumentParser()}]

for(const parser of parsersToTest) {
  describe(`${parser.name} parser directive tests`, () => {
    test('Parse a type with build-in directives correctly.', () => {
      const model = `
        type User @db(name: "user") {
          id: Int! @id
          createdAt: DateTime! @createdAt
          updatedAt: DateTime! @updatedAt
          mappedField: String! @db(name: "dbField") @relation(name: "typeRelation")
        }
      `

      const { types } = parser.instance.parseFromSchemaString(model)

      const userType = SdlExpect.type(types, 'User')
      expect(userType.databaseName).toBe('user')

      const idField = SdlExpect.field(userType, 'id', true, false, 'Int', true, true)
      expect(idField.isId).toBe(true)
      const createdAtField = SdlExpect.field(userType, 'createdAt', true, false, 'DateTime', false, true)
      expect(createdAtField.isCreatedAt).toBe(true)
      const updatedAtField = SdlExpect.field(userType, 'updatedAt', true, false, 'DateTime', false, true)
      expect(updatedAtField.isUpdatedAt).toBe(true)
      const mappedField = SdlExpect.field(userType, 'mappedField', true, false, 'String', false, false)
      expect(mappedField.databaseName).toBe('dbField')
      expect(mappedField.relationName).toBe('typeRelation')
    })


    test('Parse a type with multiple index directives correctly.', () => {
      const model = `
      type User @db(name: "user") 
          @indexes(value: [{ name: "NameIndex", fields: ["firstName", "lastName"], unique: false },
          { name: "PrimaryIndex", fields: ["id"] }]) {
        id: Int! @id
        createdAt: DateTime! @createdAt
        updatedAt: DateTime! @updatedAt
        firstName: String!
        lastName: String!
      }`

      const { types } = parser.instance.parseFromSchemaString(model)

      const userType = SdlExpect.type(types, 'User')
      const idField = SdlExpect.field(userType, 'id', true, false, 'Int', true, true)
      const firstNameField = SdlExpect.field(userType, 'firstName', true, false, TypeIdentifiers.string)
      const lastNameField = SdlExpect.field(userType, 'lastName', true, false, TypeIdentifiers.string)
 
      SdlExpect.index(userType, 'NameIndex', [firstNameField, lastNameField], false)
      // True is the default value
      SdlExpect.index(userType, 'PrimaryIndex', [idField], true)
    })

    test('Parse a type with unknown directives correctly.', () => {
      const model = `
        type DirectiveUser @typeDirective(name: "database") {
          id: Int! @id
          createdAt: DateTime! @funnyDirective @createdAt
          updatedAt: DateTime!
          mappedField: String! @unfunnyDirective(funny: "false")
        }
      `

      const { types } = parser.instance.parseFromSchemaString(model)

      const userType = SdlExpect.type(types, 'DirectiveUser')
      SdlExpect.directive(userType, { name: 'typeDirective', arguments: { name: 'database' } })

      const createdAtField = SdlExpect.field(userType, 'createdAt', true, false, 'DateTime', false, true)
      SdlExpect.directive(createdAtField, { name: 'funnyDirective', arguments: {} })

      const mappedField = SdlExpect.field(userType, 'mappedField', true, false, 'String')
      SdlExpect.directive(mappedField, { name: 'unfunnyDirective', arguments: { funny: 'false' } })
    })
  })
}