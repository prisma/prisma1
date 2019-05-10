import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { JavascriptGenerator } from '../../generators/javascript-client'
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
test('typescript definition generator', t => {
  const schema = buildSchema(generateCRUDSchemaString(datamodel, DatabaseType.postgres))
  const generator = new JavascriptGenerator({
    schema,
    internalTypes: parseInternalTypes(datamodel, DatabaseType.postgres).types,
  })
  const javascript = generator.renderJavascript()
  t.snapshot(javascript)
})
