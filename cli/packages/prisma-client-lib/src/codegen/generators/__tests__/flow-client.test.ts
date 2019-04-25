import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { FlowGenerator } from '../../generators/flow-client'
import { test } from 'ava'
import { fixturesPath } from './fixtures'
import { parseInternalTypes } from 'prisma-generate-schema'
import { DatabaseType } from 'prisma-datamodel'

const typeDefs = fs.readFileSync(
  path.join(fixturesPath, 'schema.graphql'),
  'utf-8',
)
test('flow generator', t => {
  try {
    const schema = buildSchema(typeDefs)
    const generator = new FlowGenerator({
      schema,
      internalTypes: parseInternalTypes(typeDefs, DatabaseType.mysql).types,
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
      internalTypes: parseInternalTypes(typeDefs, DatabaseType.mysql).types,
    })
    const result = generator.renderTypedefs()
    t.snapshot(result)
  } catch (e) {
    console.log(e.codeFrame)
  }
})
