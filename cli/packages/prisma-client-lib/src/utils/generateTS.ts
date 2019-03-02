import * as path from 'path'
import * as fs from 'fs'
import { TypescriptGenerator } from '../codegen/generators/typescript-client'
import { buildSchema } from 'graphql'
import { codeComment } from './codeComment'

export class TestTypescriptGenerator extends TypescriptGenerator {
  renderImports() {
    return `\
${codeComment}

import { DocumentNode, GraphQLSchema  } from 'graphql'
import { makePrismaClientClass } from '../../../makePrismaClientClass'
import { BaseClientOptions, Model } from '../../../types'
import { typeDefs } from './prisma-schema'`
  }
}

export async function generateTypescript(schemaString: string) {
  const schema = buildSchema(schemaString)
  const generator = new TestTypescriptGenerator({ schema, internalTypes: [] })
  const output = path.join(
    __dirname,
    '..',
    'artifacts',
    'generated',
    'prisma-client',
  )

  const code = generator.render()
  await fs.writeFileSync(path.join(output, 'index.ts'), code)

  const typeDefs = generator.renderTypedefs()
  await fs.writeFileSync(path.join(output, 'prisma-schema.ts'), typeDefs)
}
