import { Command, flags, Flags } from 'prisma-cli-engine'
import { prettyTime } from '../../utils/util'
import chalk from 'chalk'
import * as fs from 'fs-extra'
import * as path from 'path'
import { buildSchema } from 'graphql'
import {
  TypescriptGenerator,
  TypescriptDefinitionsGenerator,
  JavascriptGenerator,
  GoGenerator,
  FlowGenerator,
} from 'prisma-client-lib'
import { spawnSync } from 'npm-run'
import { spawnSync as nativeSpawnSync } from 'child_process'
import generateCRUDSchemaString, { parseInternalTypes } from 'prisma-generate-schema'
import { DatabaseType, IGQLType } from 'prisma-datamodel'
import { fetchAndPrintSchema } from '../deploy/printSchema'

export default class GenereateCommand extends Command {
  static topic = 'generate'
  static description = 'Generate a schema or Prisma Bindings'
  static flags: Flags = {
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
    ['endpoint']: flags.boolean({
      description: 'Use a specific endpoint for schema generation or pick endpoint from prisma.yml',
      required: false,
    }),
  }
  async run() {
    const envFile = this.flags['env-file']
    await this.definition.load(this.flags, envFile)

    if (
      this.definition.definition &&
      this.definition.definition!.generate &&
      Array.isArray(this.definition.definition!.generate) &&
      this.definition.definition!.generate!.length > 0
    ) {
      const before = Date.now()

      let schemaString
      if (this.flags.endpoint) {
        this.out.action.start(`Downloading schema`)
        const serviceName = this.definition.service!
        const stageName = this.definition.stage!
        const token = this.definition.getToken(serviceName, stageName)
        const cluster = await this.definition.getCluster()
        const workspace = this.definition.getWorkspace()
        this.env.setActiveCluster(cluster!)
        await this.client.initClusterClient(cluster!, serviceName, stageName, workspace)
        schemaString = await fetchAndPrintSchema(this.client, serviceName, stageName!, token, workspace!)
      } else {
        this.out.action.start(`Generating schema`)
        if (!this.definition.definition!.datamodel) {
          await this.out.error(`The property ${chalk.bold('datamodel')} is missing in your prisma.yml`)
        }
        const databaseType =
          this.definition.definition!.databaseType! === 'document' ? DatabaseType.mongo : DatabaseType.postgres
        schemaString = generateCRUDSchemaString(this.definition.typesString!, databaseType)
      }

      if (!schemaString) {
        await this.out.error(chalk.red(`Failed to download/generate the schema`))
      }

      this.out.action.stop(prettyTime(Date.now() - before))

      for (const { generator, output } of this.definition.definition.generate!) {
        const resolvedOutput = output.startsWith('/') ? output : path.join(this.config.definitionDir, output)

        fs.mkdirpSync(resolvedOutput)

        if (generator === 'graphql-schema') {
          await this.generateSchema(resolvedOutput, schemaString)
        }

        const isMongo = this.definition.definition && this.definition.definition.databaseType === 'document'

        const internalTypes = parseInternalTypes(
          this.definition.typesString!,
          isMongo ? DatabaseType.mongo : DatabaseType.postgres,
        ).types

        if (generator === 'typescript-client') {
          await this.generateTypescript(resolvedOutput, schemaString, internalTypes)
        }

        if (generator === 'javascript-client') {
          await this.generateJavascript(resolvedOutput, schemaString, internalTypes)
        }

        if (generator === 'go-client') {
          await this.generateGo(resolvedOutput, schemaString, internalTypes)
        }

        if (generator === 'flow-client') {
          await this.generateFlow(resolvedOutput, schemaString, internalTypes)
        }

        const generators = ['graphql-schema', 'typescript-client', 'javascript-client', 'go-client', 'flow-client']
        if (!generators.includes(generator)) {
          this.out.error(
            `Please choose one of the supported generators. Possible generators: ${generators
              .map(g => `${g}`)
              .join(`, `)}`,
          )
        }
      }
    }
  }

  async generateSchema(output: string, schemaString: string) {
    fs.writeFileSync(path.join(output, 'prisma.graphql'), schemaString)

    this.out.log(`Saving Prisma GraphQL schema (SDL) at ${output}`)
  }

  async generateTypescript(output: string, schemaString: string, internalTypes: IGQLType[]) {
    const schema = buildSchema(schemaString)

    const generator = new TypescriptGenerator({ schema, internalTypes })
    const endpoint = TypescriptGenerator.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret
      ? TypescriptGenerator.replaceEnv(this.definition.rawJson!.secret)
      : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const code = generator.render(options)
    fs.writeFileSync(path.join(output, 'index.ts'), code)

    const typeDefs = generator.renderTypedefs()
    fs.writeFileSync(path.join(output, 'prisma-schema.ts'), typeDefs)

    this.out.log(`Saving Prisma Client (TypeScript) at ${output}`)
  }

  async generateJavascript(output: string, schemaString: string, internalTypes: IGQLType[]) {
    const schema = buildSchema(schemaString)

    const generator = new JavascriptGenerator({ schema, internalTypes })
    const generatorTS = new TypescriptDefinitionsGenerator({
      schema,
      internalTypes,
    })
    const endpoint = JavascriptGenerator.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret
      ? JavascriptGenerator.replaceEnv(this.definition.rawJson!.secret)
      : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const javascript = generator.renderJavascript(options)
    fs.writeFileSync(path.join(output, 'index.js'), javascript)

    const typescript = generatorTS.render(options)
    fs.writeFileSync(path.join(output, 'index.d.ts'), typescript)

    const typeDefs = generatorTS.renderTypedefs().replace('export const typeDefs = ', '')
    fs.writeFileSync(
      path.join(output, 'prisma-schema.js'),
      `module.exports = {
        typeDefs: ${typeDefs}
      }
    `,
    )

    this.out.log(`Saving Prisma Client (JavaScript) at ${output}`)
  }

  async generateGo(output: string, schemaString: string, internalTypes: IGQLType[]) {
    const schema = buildSchema(schemaString)

    const generator = new GoGenerator({ schema, internalTypes })

    const endpoint = GoGenerator.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret ? GoGenerator.replaceEnv(this.definition.rawJson!.secret) : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const goCode = generator.render(options)
    fs.writeFileSync(path.join(output, 'prisma.go'), goCode)

    this.out.log(`Saving Prisma Client (Go) at ${output}`)
    // Run "go fmt" on the file if user has it installed.
    const isPackaged = fs.existsSync('/snapshot')
    const spawnPath = isPackaged ? nativeSpawnSync : spawnSync
    spawnPath('go', ['fmt', path.join(output, 'prisma.go')])
  }

  async generateFlow(output: string, schemaString: string, internalTypes: IGQLType[]) {
    const schema = buildSchema(schemaString)

    const generator = new FlowGenerator({ schema, internalTypes })

    const endpoint = FlowGenerator.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret ? FlowGenerator.replaceEnv(this.definition.rawJson!.secret) : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const flowCode = generator.render(options)
    fs.writeFileSync(path.join(output, 'index.js'), flowCode)

    const typeDefs = generator.renderTypedefs()
    fs.writeFileSync(path.join(output, 'prisma-schema.js'), typeDefs)

    this.out.log(`Saving Prisma Client (Flow) at ${output}`)
  }
}
