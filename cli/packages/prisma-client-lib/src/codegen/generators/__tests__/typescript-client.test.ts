import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { TypescriptGenerator } from '../typescript-client'
import { test } from 'ava'
import { fixturesPath } from './fixtures'
import { parseInternalTypes } from 'prisma-generate-schema'
import { DatabaseType } from 'prisma-datamodel'

const typeDefs = fs.readFileSync(
  path.join(fixturesPath, 'schema.graphql'),
  'utf-8',
)
// These are the only two tests that test the fix for https://github.com/prisma/prisma/issues/3372
// as we need to provide internal types manually in the tests because of them being different from the datamodel.
test('typescript generator', t => {
  const schema = buildSchema(typeDefs)
  const generator = new TypescriptGenerator({
    schema,
    internalTypes: parseInternalTypes(typeDefs, DatabaseType.mysql).types,
  })
  const result = generator.render()
  t.snapshot(result)
})
test('typescript generator definitions', t => {
  const schema = buildSchema(typeDefs)
  const generator = new TypescriptGenerator({
    schema,
    internalTypes: parseInternalTypes(typeDefs, DatabaseType.mysql).types,
  })
  const result = generator.renderTypedefs()
  t.snapshot(result)
})
