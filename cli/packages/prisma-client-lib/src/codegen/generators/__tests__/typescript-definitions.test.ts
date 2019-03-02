import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { TypescriptDefinitionsGenerator } from '../../generators/typescript-definitions'
import { test } from 'ava'
import { fixturesPath } from './fixtures'

const typeDefs = fs.readFileSync(
  path.join(fixturesPath, 'schema.graphql'),
  'utf-8',
)
test('typescript definitions generator', t => {
  const schema = buildSchema(typeDefs)
  const generator = new TypescriptDefinitionsGenerator({
    schema,
    internalTypes: [],
  })
  const result = generator.render()
  t.snapshot(result)
})
