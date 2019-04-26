import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { TypescriptGenerator } from '../typescript-client'
import { test } from 'ava'
import { fixturesPath } from './fixtures'
import generateCRUDSchemaString, {
  parseInternalTypes,
} from 'prisma-generate-schema'
import { DatabaseType } from 'prisma-datamodel'

const datamodel = fs.readFileSync(
  path.join(fixturesPath, 'datamodel.prisma'),
  'utf-8',
)

test('typescript generator', t => {
  const schema = buildSchema(generateCRUDSchemaString(datamodel, DatabaseType.postgres))
  const generator = new TypescriptGenerator({
    schema,
    internalTypes: parseInternalTypes(datamodel, DatabaseType.postgres).types,
  })
  const result = generator.render()
  t.snapshot(result)
})
test('typescript generator definitions', t => {
  const schema = buildSchema(generateCRUDSchemaString(datamodel, DatabaseType.postgres))
  const generator = new TypescriptGenerator({
    schema,
    internalTypes: parseInternalTypes(datamodel, DatabaseType.postgres).types,
  })
  const result = generator.renderTypedefs()
  t.snapshot(result)
})
