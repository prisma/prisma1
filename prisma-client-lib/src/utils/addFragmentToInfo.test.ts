import { test } from 'ava'
import { buildSchema, GraphQLResolveInfo } from 'graphql'
import { buildInfoFromFragment } from '../info'
import { addFragmentToInfo } from './addFragmentToInfo'
import { assertFields } from '../info.test'
import { omitDeep } from './removeKey'
import { printDocumentFromInfo } from '.'

test('addFragmentToInfo: add field by simple query', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
    extraField: String
  }
  `)
  const info = buildInfoFromFragment('book', schema, 'query', `{ title }`)
  const patchedInfo = addFragmentToInfo(info, '{extraField}')
  const selections = patchedInfo.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['title', 'extraField'])
})

test('addFragmentToInfo: add field to array payload', t => {
  const schema = buildSchema(`
  type Query {
    books: [Book!]!
  }

  type Book {
    title: String
    extraField: String
  }
  `)
  const info = buildInfoFromFragment('books', schema, 'query', `{ title }`)
  const patchedInfo = addFragmentToInfo(info, '{ extraField }')

  t.snapshot(printDocumentFromInfo(patchedInfo))
  t.snapshot(getRelevantPartsFromInfo(info))
  t.snapshot(getRelevantPartsFromInfo(patchedInfo))
})

test('addFragmentToInfo: add field by fragment', t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
    extraField: String
  }
  `)
  const info = buildInfoFromFragment('book', schema, 'query', `{ title }`)
  const patchedInfo = addFragmentToInfo(
    info,
    'fragment F on Book { extraField }',
  )
  const selections = patchedInfo.fieldNodes[0].selectionSet!.selections

  assertFields(t, selections, ['title', 'extraField'])
})

test("addFragmentToInfo: dont add field by fragment when type doesn't match", t => {
  const schema = buildSchema(`
  type Query {
    book: Book
  }

  type Book {
    title: String
    extraField: String
  }
  `)
  const info = buildInfoFromFragment('book', schema, 'query', `{ title }`)
  t.throws(() =>
    addFragmentToInfo(info, 'fragment F on UnknownType { extraField }'),
  )
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
