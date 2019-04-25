import * as fs from 'fs'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { FlowGenerator } from '../../generators/flow-client'
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
test('flow generator', t => {
  try {
    const schema = buildSchema(generateCRUDSchemaString(datamodel, DatabaseType.postgres))
    const generator = new FlowGenerator({
      schema,
      internalTypes: parseInternalTypes(datamodel, DatabaseType.postgres).types,
    })
    const result = generator.render()
    t.snapshot(result)
  } catch (e) {
    console.log(e.codeFrame)
  }
})
test('flow generator - print schema', t => {
  try {
    const schema = buildSchema(generateCRUDSchemaString(datamodel, DatabaseType.postgres))
    const generator = new FlowGenerator({
      schema,
      internalTypes: parseInternalTypes(datamodel, DatabaseType.postgres).types,
    })
    const result = generator.renderTypedefs()
    t.snapshot(result)
  } catch (e) {
    console.log(e.codeFrame)
  }
})
