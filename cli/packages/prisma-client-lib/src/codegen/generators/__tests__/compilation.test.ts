import * as path from 'path'
import * as fs from 'fs'
import { testTSCompilation, testFlowCompilation } from '../../utils/compile'
import test from 'ava'
import { fixturesPath } from './fixtures'

const datamodel = fs.readFileSync(
  path.join(fixturesPath, 'datamodel.prisma'),
  'utf-8',
)

test('flow compilation', async t => {
  t.is(await testFlowCompilation(datamodel), 0)
})

test('typescript compilation', async t => {
  t.is(await testTSCompilation(datamodel), 0)
})
