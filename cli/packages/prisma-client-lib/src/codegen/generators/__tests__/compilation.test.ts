import * as path from 'path'
import * as fs from 'fs'
import { testTSCompilation, testFlowCompilation } from '../../utils/compile'
import test from 'ava'
import { fixturesPath } from './fixtures'

const typeDefs = fs.readFileSync(
  path.join(fixturesPath, 'schema.graphql'),
  'utf-8',
)

test('flow compilation', async t => {
  t.is(await testFlowCompilation(typeDefs), 0)
})

test('typescript compilation', async t => {
  t.is(await testTSCompilation(typeDefs), 0)
})
