
import { IGQLType, IGQLField, GQLScalarField, IDirectiveInfo, GQLOneRelationField } from '../../src/datamodel/model'
import Renderer from '../../src/datamodel/renderer'
import Parser from '../../src/datamodel/parser'
import { DatabaseType } from '../../src/databaseType';

const renderer = Renderer.create(DatabaseType.postgres)
const parser = Parser.create(DatabaseType.postgres)

const modelWithDirectives = `type Test @embedded {
  hasBeenCreatedAt: DateTime @createdAt
  hasBeenUpdatedAt: DateTime @updatedAt
  mappedField: String @db(name: "dbField") @relation(name: "typeRelation")
  primaryId: Int @id
}`

describe(`Renderer directives test`, () => {
  test('Render built-in directives correctly', () => {
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
})