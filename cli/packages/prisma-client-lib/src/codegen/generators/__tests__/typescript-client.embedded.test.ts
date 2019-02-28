import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { TypescriptGenerator } from '../typescript-client'
import { test } from 'ava'
import { parseInternalTypes } from 'prisma-generate-schema'
import { DatabaseType } from 'prisma-datamodel'
import { fixturesPath } from './fixtures'

const typeDefs = fs.readFileSync(
  path.join(fixturesPath, 'embedded.graphql'),
  'utf-8',
)

const datamodel = `
type User {
  id: ID! @unique
  name: String!
  address: Address
}

type Address @embedded {
  location: String
}
`

test.skip('typescript generator - embedded', t => {
  const schema = buildSchema(typeDefs)
  const generator = new TypescriptGenerator({
    schema,
    internalTypes: parseInternalTypes(datamodel, DatabaseType.postgres).types,
  })
  const result = generator.render()
  t.snapshot(result)
})
