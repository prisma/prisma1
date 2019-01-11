
import { IGQLType, IGQLField, GQLScalarField, IDirectiveInfo } from '../../src/datamodel/model'
import Renderer from '../../src/datamodel/renderer'
import Parser from '../../src/datamodel/parser'
import { DatabaseType } from '../../src/databaseType';

const renderer = Renderer.create(DatabaseType.postgres)
const parser = Parser.create(DatabaseType.postgres)

const simpleModel = 
`type User {
  age: int
  isAdmin: Boolean @default(value: false)
  name: String
  nationality: String @default(value: "DE")
  roles: [Int!]!
}`

const modelWithDirectives =
`type Test @dummyDirective(isDummy: true) @embedded {
  test: String @relation(link: INLINE)
  test2: Int @defaultValue(value: 10) @relation(name: "TestRelation")
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

  test('Render directives correctly', () => {
    const scalarField = new GQLScalarField('test', 'String')
    scalarField.directives = [{
      name: "relation",
      arguments: {
        link: "INLINE"
      }
    }]

    const arrayField = new GQLScalarField('test2', 'Int')
    arrayField.directives = [{
      name: "relation",
      arguments: {
        name: "\"TestRelation\""
      }
    }, {
      name: "defaultValue",
      arguments: {
        value: "10"
      }
    }]

    const typeDirectives: IDirectiveInfo[] = [{
      name: "dummyDirective",
      arguments: {
        isDummy: "true"
      }
    }]

    const type: IGQLType = {
      name: "Test", 
      isEmbedded: true,
      directives: typeDirectives,
      isEnum: false,
      fields: [
        scalarField, arrayField
      ]
    }



    const res = renderer.render({
      types: [type]
    })

    expect(res).toEqual(modelWithDirectives)
  })

  test('Render a single type consistently with the parser', () => {
    const parsed = parser.parseFromSchemaString(simpleModel);
    const rendered = renderer.render(parsed)

    expect(rendered).toEqual(simpleModel)
  })
})