import {
  IGQLType,
  IGQLField,
  GQLScalarField,
  IDirectiveInfo,
  GQLOneRelationField,
} from '../../src/datamodel/model'
import Renderer from '../../src/datamodel/renderer'
import Parser from '../../src/datamodel/parser'
import { DatabaseType } from '../../src/databaseType'
import { dedent } from '../../src/util/util'

describe(`Renderer datamodel v1.1 test`, () => {
  test('Render postgres special fields according to datamodel v1.1', () => {
    const renderer = Renderer.create(DatabaseType.postgres, true)

    const modelWithDirectives = dedent(`
      type Test {
        hasBeenCreatedAt: DateTime @createdAt
        hasBeenUpdatedAt: DateTime @updatedAt
        primaryId: Int @id
        stringList: [String] @scalarList(strategy: RELATION)
      }`)

    const field1 = new GQLScalarField('hasBeenCreatedAt', 'DateTime')
    field1.isCreatedAt = true
    const field2 = new GQLScalarField('hasBeenUpdatedAt', 'DateTime')
    field2.isUpdatedAt = true
    const field3 = new GQLScalarField('primaryId', 'Int')
    field3.isId = true
    const field4 = new GQLScalarField('stringList', 'String')
    field4.isList = true

    const type: IGQLType = {
      name: 'Test',
      isEmbedded: false,
      isLinkTable: false,
      isEnum: false,
      fields: [field1, field2, field3, field4],
      comments: [],
      directives: [],
      databaseName: null,
      indices: [],
    }

    const res = renderer.render(
      {
        types: [type],
      },
      true,
    )

    expect(res).toEqual(modelWithDirectives)
  })
})
