import DatamodelParser from '../../src/datamodel/parser'
import * as util from 'util'
import { parse } from 'graphql'
import { printSchema, buildSchema } from 'graphql/utilities'
import Generators from '../../src/generator/defaultGenerators'
import AstTools from '../../src/util/astTools'
import * as fs from 'fs'
import * as path from 'path'

export default function blackBoxTest(name: string) {
  const generators = new Generators()

  const modelPath = path.join(__dirname, `cases/${name}/model.graphql`)
  const prismaPath = path.join(__dirname, `cases/${name}/prisma.graphql`)

  expect(fs.existsSync(modelPath))
  expect(fs.existsSync(prismaPath))

  const model = fs.readFileSync(modelPath, { encoding: 'UTF-8' })
  const prisma = fs.readFileSync(prismaPath, { encoding: 'UTF-8' })

  const types = DatamodelParser.parseFromSchemaString(model)
  const ourSchema = generators.schema.generate(types, {})

  const ourPrintedSchema = printSchema(ourSchema)

  // Write a copy of the generated schema to the FS, for debugging
  fs.writeFileSync(
    path.join(__dirname, `cases/${name}/generated.graphql`),
    ourPrintedSchema,
    { encoding: 'UTF-8' },
  )

  // Check if our schema equals the prisma schema.
  const prismaSchema = buildSchema(prisma)
  AstTools.assertTypesEqual(prismaSchema, ourSchema)

  // Check if we can parse the schema we build (e.g. it's syntactically valid).
  parse(ourPrintedSchema)
}

// const testNames = fs.readdirSync(path.join(__dirname, 'cases'))
const testNames = ['embedded']

for (const testName of testNames) {
  test(`Generates input type for ${testName} correctly`, () => {
    blackBoxTest(testName)
  })
}
