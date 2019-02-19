import * as util from 'util'
import { parse } from 'graphql'
import { printSchema, buildSchema } from 'graphql/utilities'
import { AstTools, DatabaseType, DefaultParser } from 'prisma-datamodel'
import Generator from '../../src/generator'
import * as fs from 'fs'
import * as path from 'path'

export default function blackBoxTest(
  name: string,
  databaseType: DatabaseType,
  filePrefix: string,
) {
  const generator = Generator.create(databaseType)

  const modelPath = path.join(
    __dirname,
    `cases/${name}/model_${filePrefix}.graphql`,
  )
  const prismaPath = path.join(__dirname, `cases/${name}/${filePrefix}.graphql`)

  expect(fs.existsSync(modelPath))
  expect(fs.existsSync(prismaPath))

  const model = fs.readFileSync(modelPath, { encoding: 'UTF-8' })
  const prisma = fs.readFileSync(prismaPath, { encoding: 'UTF-8' })

  const { types } = DefaultParser.create(databaseType).parseFromSchemaString(model)
  const ourSchema = generator.schema.generate(types, {})

  const ourPrintedSchema = printSchema(ourSchema)

  // Write a copy of the generated schema to the FS, for debugging
  fs.writeFileSync(path.join(__dirname, `cases/${name}/generated_${databaseType}.graphql`), ourPrintedSchema, {
    encoding: 'UTF-8',
  })

  // Check if our schema equals the prisma schema.
  const prismaSchema = buildSchema(prisma)
  AstTools.assertTypesEqual(prismaSchema, ourSchema, `${name}/${databaseType}`)

  // Check if we can parse the schema we build (e.g. it's syntactically valid).
  parse(ourPrintedSchema)
}

const testNames = fs.readdirSync(path.join(__dirname, 'cases'))

for (const testName of testNames) {
  test(`Generates schema for ${testName}/relational correctly`, () => {
    blackBoxTest(testName, DatabaseType.postgres, 'relational')
  })
  test(`Generates schema for ${testName}/document correctly`, () => {
    blackBoxTest(testName, DatabaseType.mongo, 'document')
  })
}
