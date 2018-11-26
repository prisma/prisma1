import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { TypescriptDefinitionsGenerator } from './typescript-definitions'
import { test } from 'ava'

const typeDefs = fs.readFileSync(
  path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'),
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
