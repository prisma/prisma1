import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { GoGenerator } from './GoGenerator'
import { test } from 'ava'

const typeDefs = fs.readFileSync(
  path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'),
  'utf-8',
)
test('go generator', t => {
  const schema = buildSchema(typeDefs)
  const generator = new GoGenerator({
    schema,
  })
  const result = generator.render()
  t.snapshot(result)
})
