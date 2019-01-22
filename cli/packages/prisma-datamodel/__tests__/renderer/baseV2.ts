import { IGQLType, IGQLField, GQLScalarField, IDirectiveInfo, GQLOneRelationField } from '../../src/datamodel/model'
import Renderer from '../../src/datamodel/renderer'
import Parser from '../../src/datamodel/parser'
import { DatabaseType } from '../../src/databaseType'
import { dedent } from '../../src/util/util'

describe(`Renderer datamodel v2 test`, () => {
  test('Render postgres special fields according to datamodel v2', () => {
    const renderer = Renderer.create(DatabaseType.postgres, true)
    
    const modelWithDirectives = dedent(`
      type Test {
        hasBeenCreatedAt: DateTime @createdAt
        hasBeenUpdatedAt: DateTime @updatedAt
        primaryId: Int @id
      }`)
      
    const field1 = new GQLScalarField('hasBeenCreatedAt', 'DateTime')
    field1.isCreatedAt = true
    const field2 = new GQLScalarField('hasBeenUpdatedAt', 'DateTime')
    field2.isUpdatedAt = true
    const field3 = new GQLScalarField('primaryId', 'Int')
    field3.isId = true

    const type: IGQLType = {
      name: "Test", 
      isEmbedded: false,
      isEnum: false,
      fields: [
        field1, field2, field3
      ]
    }

    const res = renderer.render({
      types: [type]
    })

    expect(res).toEqual(modelWithDirectives)
  })
})