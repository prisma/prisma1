import { TestContext, test } from 'ava'
import {
  buildSchema,
  SelectionNode,
  FieldNode,
  GraphQLResolveInfo,
} from 'graphql'
import {
  buildInfoForAllScalars,
  buildInfoFromFragment,
  makeSubInfo,
} from './info'
import { omitDeep } from './utils/removeKey'
import { printDocumentFromInfo } from './utils'

test('buildInfoForAllScalars: 1 field', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
  }
  `)
  const info = buildInfoForAllScalars('book', schema, 'query')
  const selections = info.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['title'])
})

test('buildInfoForAllScalars: 2 fields', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
    number: Float
  }
  `)
  const info = buildInfoForAllScalars('book', schema, 'query')
  const selections = info.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['title', 'number'])
  t.is(info.fieldName, 'book')
})

test('buildInfoForAllScalars: excludes object type fields', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
    number: Float
    otherBook: Book
  }
  `)
  const info = buildInfoForAllScalars('book', schema, 'query')
  const selections = info.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['title', 'number'])
  t.is(info.fieldName, 'book')
})

test('buildInfoForAllScalars: enums', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    color: Color
  }

  enum Color { Red, Blue }
  `)
  const info = buildInfoForAllScalars('book', schema, 'query')
  const selections = info.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['color'])
})

test('buildInfoForAllScalars: minimal static root field', t => {
  const schema = buildSchema(`
  type Query {
    count: Int
  }
  `)
  const info = buildInfoForAllScalars('count', schema, 'query')
  t.is(info.fieldNodes.length, 1)
})

test('buildInfoForAllScalars: mutation', t => {
  const schema = buildSchema(`
  type Query {
    book: Int # use name root field name but different type
  }

  type Mutation {
    book: Book
  }

  type Book {
    title: String
  }
  `)
  const info = buildInfoForAllScalars('book', schema, 'mutation')
  const selections = info.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['title'])
})

test('buildInfoForAllScalars: throws error when field not found', t => {
  const schema = buildSchema(`
  type Query {
    count: Int
  }
  `)
  t.throws(() => buildInfoForAllScalars('other', schema, 'query'))
})

test('buildInfoFromFragment: 1 field', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
  }
  `)
  const info = buildInfoFromFragment('book', schema, 'query', `{ title }`)
  const selections = info.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['title'])
})

test('buildInfoFromFragment: nested', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
    otherBook: Book
  }
  `)
  const fragment = `{ title otherBook { otherBook { title } } }`
  const info = buildInfoFromFragment('book', schema, 'query', fragment)
  const selections = info.fieldNodes[0].selectionSet!.selections as any

  t.is(selections[0].name.value, 'title')
  t.is(selections[1].name.value, 'otherBook')
  t.is(selections[1].selectionSet.selections[0].name.value, 'otherBook')
  t.is(
    selections[1].selectionSet.selections[0].selectionSet.selections[0].name
      .value,
    'title',
  )
})

test('buildInfoFromFragment: invalid selection', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
  }
  `)
  t.throws(() => buildInfoFromFragment('book', schema, 'query', `{ xxx }`))
})

test('makeSubInfo: works when path has been selected', t => {
  const schema = buildSchema(`
    type Query {
      book: Book
    }

    type Book {
      title: String
      extraField: String
      page: Page
    }

    type Page {
      content: String
      wordCount: Int
    }
  `)
  const info = buildInfoFromFragment(
    'book',
    schema,
    'query',
    `{ title page { content wordCount } }`,
  )

  const subInfo = makeSubInfo(info, 'page')!

  t.snapshot(printDocumentFromInfo(subInfo))
  t.snapshot(getRelevantPartsFromInfo(subInfo))
})

test('makeSubInfo: works when path has been selected and adds fragment', t => {
  const schema = buildSchema(`
    type Query {
      book: Book
    }

    type Book {
      title: String
      extraField: String
      page: Page
    }

    type Page {
      content: String
      wordCount: Int
    }
  `)
  const info = buildInfoFromFragment(
    'book',
    schema,
    'query',
    `{ title page { content } }`,
  )

  const subInfo = makeSubInfo(
    info,
    'page',
    'fragment Frag on Page { wordCount }',
  )!

  t.snapshot(printDocumentFromInfo(subInfo))
})

test('makeSubInfo: works with inline fragment', t => {
  const schema = buildSchema(`
    type Query {
      book: Book
    }

    type Book {
      title: String
      extraField: String
      page: Page
    }

    type Page {
      content: String
      wordCount: Int
    }
  `)
  const info = buildInfoFromFragment(
    'book',
    schema,
    'query',
    `{ title ... on Book { page { content } } }`,
  )

  const subInfo = makeSubInfo(info, 'page')!

  t.snapshot(printDocumentFromInfo(subInfo))
  t.snapshot(getRelevantPartsFromInfo(subInfo))
})

function getRelevantPartsFromInfo(info: GraphQLResolveInfo) {
  const {
    fragments,
    fieldName,
    returnType,
    parentType,
    path,
    rootValue,
    operation,
    variableValues,
    fieldNodes,
  } = info

  return {
    fragments,
    fieldName,
    returnType: returnType.toString(),
    parentType: parentType.toString(),
    path,
    rootValue,
    operation,
    variableValues,
    selectionSet: omitDeep(fieldNodes[0].selectionSet, 'loc'),
  }
}

test('makeSubInfo: returns null when path has not been selected', t => {
  const schema = buildSchema(`
    type Query {
      book: Book
    }

    type Book {
      title: String
      extraField: String
      page: Page
    }

    type Page {
      content: String
      wordCount: Int
    }
  `)
  const info = buildInfoFromFragment('book', schema, 'query', `{ title }`)

  const subInfo = makeSubInfo(info, 'page')!

  t.is(subInfo, null)
})

export function assertFields(
  t: TestContext,
  selections: ReadonlyArray<SelectionNode>,
  names: string[],
) {
  const fields = names.map<FieldNode>(value => ({
    kind: 'Field',
    name: { kind: 'Name', value },
  }))

  for (const field of fields) {
    t.true(
      selections.some(
        s => s.kind === 'Field' && s.name.value === field.name.value,
      ),
    )
  }

  t.is(selections.length, names.length)
}
