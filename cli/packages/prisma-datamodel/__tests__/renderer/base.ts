
import { IGQLType, IGQLField, GQLScalarField } from '../../src/datamodel/model'
import Renderer from '../../src/datamodel/renderer'
import Parser from '../../src/datamodel/parser'
import { DatabaseType } from '../../src/databaseType';

const renderer = Renderer.create(DatabaseType.postgres)
const parser = Parser.create(DatabaseType.postgres)

const simpleModel = 
`type User {
    name: String
    age: int
    isAdmin: Boolean @default(value: false)
    nationality: String @default(value: "DE")
    roles: [Int!]!
}`


describe(`Renderer test`, () => {
  test('Renderer a single type with scalars and default value correctly.', () => {

    const fieldWithDefaultValue = new GQLScalarField('isAdmin', 'Boolean')
    fieldWithDefaultValue.defaultValue = 'false'

    const fieldWithStringDefaultValue = new GQLScalarField('nationality', 'String')
    fieldWithStringDefaultValue.defaultValue = 'DE'

    const listField = new GQLScalarField('roles', 'Int')
    listField.isList = true

    const model = {
      fields: [
        new GQLScalarField('name', 'String'),
        new GQLScalarField('age', 'int'),
        fieldWithDefaultValue,
        fieldWithStringDefaultValue,
        listField
      ],
      name: 'User',
      isEmbedded: false,
      isEnum: false
    } as IGQLType

    const res = renderer.render({
      types: [model]
    })

    expect(res).toEqual(simpleModel)
  })

  test('Render a single type consistently with the parser', () => {
    const parsed = parser.parseFromSchemaString(simpleModel);
    const rendered = renderer.render(parsed)

    expect(rendered).toEqual(simpleModel)
  })
})