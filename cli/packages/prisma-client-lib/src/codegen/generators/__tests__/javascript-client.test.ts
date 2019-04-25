import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { JavascriptGenerator } from '../../generators/javascript-client'
import { test } from 'ava'
import { fixturesPath } from './fixtures'
import { parseInternalTypes } from 'prisma-generate-schema'
import { DatabaseType } from 'prisma-datamodel'

const typeDefs = fs.readFileSync(
  path.join(fixturesPath, 'schema.graphql'),
  'utf-8',
)
test('typescript definition generator', t => {
  const schema = buildSchema(typeDefs)
  const generator = new JavascriptGenerator({
    schema,
    internalTypes: parseInternalTypes(typeDefs, DatabaseType.mysql).types,
  })
  const javascript = generator.renderJavascript()
  t.snapshot(javascript)
})
