import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { FlowGenerator } from './flow-client'
import { test } from 'ava'

const typeDefs = fs.readFileSync(
  path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'),
  'utf-8',
)
test('flow generator', t => {
  try {
    const schema = buildSchema(typeDefs)
    const generator = new FlowGenerator({
      schema,
      internalTypes: [],
    })
    const result = generator.render()
    t.snapshot(result)
  } catch (e) {
    console.log(e.codeFrame)
  }
})
test('flow generator - print schema', t => {
  try {
    const schema = buildSchema(typeDefs)
    const generator = new FlowGenerator({
      schema,
      internalTypes: [],
    })
    const result = generator.renderTypedefs()
    t.snapshot(result)
  } catch (e) {
    console.log(e.codeFrame)
  }
})
