import DatamodelParser from '../../src/datamodel/parser'
import * as util from 'util'
import { parse } from 'graphql'
import { printSchema, buildSchema } from 'graphql/utilities'
import RelationalGenerator from '../../src/generator/default'
import DocumentGenerator from '../../src/generator/document'
import AstTools from '../../src/util/astTools'
import * as fs from 'fs'
import * as path from 'path'

export default function blackBoxTest(name: string, databaseType: string) {
  const generator = databaseType === 'relational' ?
     new RelationalGenerator() :
     new DocumentGenerator()

  const modelPath = path.join(__dirname, `cases/${name}/model.graphql`)
  const prismaPath = path.join(__dirname, `cases/${name}/${databaseType}.graphql`)

  expect(fs.existsSync(modelPath))
  expect(fs.existsSync(prismaPath))

  const model = fs.readFileSync(modelPath, { encoding: 'UTF-8' })
  const prisma = fs.readFileSync(prismaPath, { encoding: 'UTF-8' })

  const types = DatamodelParser.parseFromSchemaString(model)
  const ourSchema = generator.schema.generate(types, {})

  const ourPrintedSchema = printSchema(ourSchema)

  // Write a copy of the generated schema to the FS, for debugging
  fs.writeFileSync(
    path.join(__dirname, `cases/${name}/generated_${databaseType}.graphql`),
    ourPrintedSchema,
    { encoding: 'UTF-8' },
  )

  // Check if our schema equals the prisma schema.
  const prismaSchema = buildSchema(prisma)
  AstTools.assertTypesEqual(prismaSchema, ourSchema, `${name}/${databaseType}`)

  // Check if we can parse the schema we build (e.g. it's syntactically valid).
  parse(ourPrintedSchema)
}

const testNames = fs.readdirSync(path.join(__dirname, 'cases'))

for (const testName of testNames) {
  test(`Generates schema for ${testName}/relational correctly`, () => {
    blackBoxTest(testName, 'relational')
  })
  test(`Generates schema for ${testName}/document correctly`, () => {
    blackBoxTest(testName, 'document')
  })
}
