import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { TypescriptGenerator } from './typescript-client'
import { test } from 'ava'

const typeDefs = fs.readFileSync(
  path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'),
  'utf-8',
)
test.only('typescript generator', t => {
  const schema = buildSchema(typeDefs)
  const generator = new TypescriptGenerator({
    schema,
    internalTypes: [],
  })
  const result = generator.render()
  t.snapshot(result)
})
