import { IGQLType, IGQLField, GQLScalarField, IDirectiveInfo } from '../../src/datamodel/model'
import Renderer from '../../src/datamodel/renderer'
import Parser from '../../src/datamodel/parser'
import { DatabaseType } from '../../src/databaseType'

const renderer = Renderer.create(DatabaseType.postgres)
const parser = Parser.create(DatabaseType.postgres)

const simpleModel = `type User {
  age: int
  isAdmin: Boolean @default(value: false)
  name: String
  nationality: String @default(value: "DE")
  roles: [Int]
}`

const complicatedModel = `type User @indexes(value: [
  {name: "NameIndex", fields: ["firstName", "lastName"]},
  {name: "PrimaryIndex", fields: ["id"], unique: true}
]) {
  id: Int! @id(strategy: SEQUENCE) @sequence(name: "test_seq", initialValue: 8, allocationSize: 100)
  age: int
  isAdmin: Boolean @default(value: false)
  nationality: String @default(value: "DE")
  roles: [Int]
  firstName: String!
  lastName: String!
}`

const modelWithDirectives = `type Test @dummyDirective(isDummy: true) {
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

    const model: IGQLType = {
      fields: [
        new GQLScalarField('name', 'String'),
        new GQLScalarField('age', 'int'),
        fieldWithDefaultValue,
        fieldWithStringDefaultValue,
        listField,
      ],
      name: 'User',
      isEmbedded: false,
      isEnum: false,
      indices: [],
      directives: [],
      comments: [],
      databaseName: null,
    }

    const res = renderer.render(
      {
        types: [model],
      },
      true,
    )

    expect(res).toEqual(simpleModel)
  })

  test('Render directives correctly', () => {
    const scalarField = new GQLScalarField('test', 'String')
    scalarField.directives = [
      {
        name: 'relation',
        arguments: {
          link: 'INLINE',
        },
      },
    ]

    const arrayField = new GQLScalarField('test2', 'Int')
    arrayField.directives = [
      {
        name: 'relation',
        arguments: {
          name: '"TestRelation"',
        },
      },
      {
        name: 'defaultValue',
        arguments: {
          value: '10',
        },
      },
    ]

    const typeDirectives: IDirectiveInfo[] = [
      {
        name: 'dummyDirective',
        arguments: {
          isDummy: 'true',
        },
      },
    ]

    const type: IGQLType = {
      name: 'Test',
      isEmbedded: false,
      directives: typeDirectives,
      isEnum: false,
      fields: [scalarField, arrayField],
      comments: [],
      indices: [],
      databaseName: null,
    }

    const res = renderer.render(
      {
        types: [type],
      },
      true,
    )

    expect(res).toEqual(modelWithDirectives)
  })

  test('Render a single type consistently with the parser', () => {
    const parsed = parser.parseFromSchemaString(simpleModel)
    const rendered = renderer.render(parsed, true)

    expect(rendered).toEqual(simpleModel)
  })

  test('Render a more complicated schema consistently with the parser', () => {
    const parsed = parser.parseFromSchemaString(simpleModel)
    const rendered = renderer.render(parsed, true)

    expect(rendered).toEqual(simpleModel)
  })
})
