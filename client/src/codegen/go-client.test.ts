import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { GoGenerator } from './go-client'
import { test } from 'ava'

const typeDefs = fs.readFileSync(
  path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'),
  'utf-8',
)
test('go generator', t => {
  const schema = buildSchema(typeDefs)
  const generator = new GoGenerator({
    schema,
    internalTypes: [],
  })
  const result = generator.render({
    endpoint: 'http://localhost:4466/test/test',
  })
  t.snapshot(result)
})
