import { GQLScalarField, IGQLType, GQLOneRelationField } from '../src/datamodel/model'
import { toposort } from '../src/util/sort'

describe(`Toposort test`, () => {
  test('Should sort all types in topoligical order', () => {
    const embeddedEmbedded = {
      fields: [
        new GQLScalarField('id', 'int')
      ],
      name: 'EmbeddedEmbedded',
      isEmbedded: true,
      isEnum: false
    } as IGQLType

    const embedded = {
      fields: [
        new GQLOneRelationField('child', embeddedEmbedded)
      ],
      name: 'Embedded',
      isEmbedded: true,
      isEnum: false
    } as IGQLType

    const parent = {
      fields: [
        new GQLOneRelationField('child', embedded)
      ],
      name: 'Parent',
      isEmbedded: false,
      isEnum: false
    } as IGQLType

    const unrelated = {
      fields: [
        new GQLScalarField('id', 'int')
      ],
      name: 'Unrelated',
      isEmbedded: false,
      isEnum: false
    } as IGQLType

    const types = [embedded, embeddedEmbedded, unrelated, parent]

    const sorted = toposort(types)

    expect(sorted[0]).toBe(unrelated)
    expect(sorted[1]).toBe(parent)
    expect(sorted[2]).toBe(embedded)
    expect(sorted[3]).toBe(embeddedEmbedded)
  })
})