
import { IGQLType, IGQLField, GQLScalarField, IDirectiveInfo, GQLOneRelationField } from '../../src/datamodel/model'
import Renderer from '../../src/datamodel/renderer'
import Parser from '../../src/datamodel/parser'
import { DatabaseType } from '../../src/databaseType'
import { dedent } from '../../src/util/util'

describe(`Renderer directives test`, () => {
  test('Render built-in directives correctly with mongo renderer', () => {
    const renderer = Renderer.create(DatabaseType.mongo)
    
    const modelWithDirectives = dedent(`
      type Test @embedded {
        hasBeenCreatedAt: DateTime @createdAt
        hasBeenUpdatedAt: DateTime @updatedAt
        mappedField: String @db(name: "dbField") @relation(name: "typeRelation")
        primaryId: Int @id
      }`)
      
    const field1 = new GQLScalarField('hasBeenCreatedAt', 'DateTime')
    field1.isCreatedAt = true
    const field2 = new GQLScalarField('hasBeenUpdatedAt', 'DateTime')
    field2.isUpdatedAt = true
    const field3 = new GQLScalarField('primaryId', 'Int')
    field3.isId = true
    const field4 = new GQLScalarField('mappedField', 'String')
    field4.databaseName = 'dbField'
    field4.relationName = 'typeRelation'

    const type: IGQLType = {
      name: "Test", 
      isEmbedded: true,
      // This will be ignored since we are dealing with an embedded type
      databaseName: 'testType',
      isEnum: false,
      fields: [
        field1, field2, field3, field4
      ]
    }

    const res = renderer.render({
      types: [type]
    })

    expect(res).toEqual(modelWithDirectives)
  })

  test('Render built-in directives correctly with postgres renderer', () => {
    const renderer = Renderer.create(DatabaseType.postgres)

    const modelWithDirectives = dedent(`
      type Test @db(name: "testType") {
        createdAt: DateTime
        id: Int
        mappedField: String @db(name: "dbField") @relation(name: "typeRelation")
        updatedAt: DateTime
      }`)
      
    const field1 = new GQLScalarField('createdAt', 'DateTime')
    field1.isCreatedAt = true
    const field2 = new GQLScalarField('updatedAt', 'DateTime')
    field2.isUpdatedAt = true
    const field3 = new GQLScalarField('id', 'Int')
    field3.isId = true
    const field4 = new GQLScalarField('mappedField', 'String')
    field4.databaseName = 'dbField'
    field4.relationName = 'typeRelation'

    const type: IGQLType = {
      name: "Test", 
      isEmbedded: false,
      databaseName: 'testType',
      isEnum: false,
      fields: [
        field1, field2, field3, field4
      ]
    }

    const res = renderer.render({
      types: [type]
    })

    expect(res).toEqual(modelWithDirectives)
  })

  test('Render built-in index directive correctly', () => {
    const renderer = Renderer.create(DatabaseType.mongo)
    
    const modelWithDirectives = dedent(`
      type User @index(name: "NameIndex", fields: ["firstName", "lastName"], unique: false) @index(name: "PrimaryIndex", fields: ["id"]) {
        createdAt: DateTime! @createdAt
        firstName: String!
        id: Int! @id
        lastName: String!
        updatedAt: DateTime! @updatedAt
      }`)
      
    const createdAtField = new GQLScalarField('createdAt', 'DateTime', true)
    createdAtField.isCreatedAt = true
    const updatedAtField = new GQLScalarField('updatedAt', 'DateTime', true)
    updatedAtField.isUpdatedAt = true
    const idField = new GQLScalarField('id', 'Int', true)
    idField.isId = true
    const firstNameField = new GQLScalarField('firstName', 'String', true)
    const lastNameField = new GQLScalarField('lastName', 'String', true)

    const type: IGQLType = {
      name: "User", 
      isEmbedded: false,
      isEnum: false,
      fields: [
        idField, createdAtField, updatedAtField, firstNameField, lastNameField
      ],
      indices: [
        { name: 'NameIndex', fields: [firstNameField, lastNameField], unique: false},
        { name: 'PrimaryIndex', fields: [idField], unique: true}
      ]
    }

    const res = renderer.render({
      types: [type]
    })

    expect(res).toEqual(modelWithDirectives)
  })

})