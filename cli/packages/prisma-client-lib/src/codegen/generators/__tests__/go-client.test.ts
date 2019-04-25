import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { GoGenerator } from '../go-client'
import { test } from 'ava'
import { fixturesPath } from './fixtures'
import { parseInternalTypes } from 'prisma-generate-schema'
import { DatabaseType } from 'prisma-datamodel'

const typeDefs = fs.readFileSync(
  path.join(fixturesPath, 'schema.graphql'),
  'utf-8',
)
test('go generator', t => {
  const schema = buildSchema(typeDefs)
  const generator = new GoGenerator({
    schema,
    internalTypes: parseInternalTypes(typeDefs, DatabaseType.mysql).types,
  })
  const result = generator.render({
    endpoint: 'http://localhost:4466/test/test',
  })
  t.snapshot(result)
})
