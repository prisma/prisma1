import { Command, flags, Flags } from 'prisma-cli-engine'
import { prettyTime, concatName } from '../../util'
import { fetchAndPrintSchema } from '../deploy/printSchema'
import * as fs from 'fs-extra'
import * as path from 'path'
import { buildSchema } from 'graphql'
import { TypescriptGenerator, TypescriptDefinitionGenerator, GoGenerator, FlowGenerator } from 'prisma-lib'
import { spawnSync } from 'npm-run'

export default class GenereateCommand extends Command {
  static topic = 'generate'
  static description = 'Generate a schema or Prisma Bindings'
  static flags: Flags = {
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
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
      this.out.action.start(`Downloading schema`)
      const serviceName = this.definition.service!
      const stageName = this.definition.stage!
      const token = this.definition.getToken(serviceName, stageName)
      const cluster = this.definition.getCluster()
      const workspace = this.definition.getWorkspace()
      this.env.setActiveCluster(cluster!)

      await this.client.initClusterClient(
        cluster!,
        serviceName,
        stageName,
        workspace,
      )
      const schemaString = await fetchAndPrintSchema(
        this.client,
        serviceName,
        stageName!,
        token,
        workspace!,
      )
      this.out.action.stop(prettyTime(Date.now() - before))
      for (const { generator, output } of this.definition.definition
        .generate!) {
        const resolvedOutput = output.startsWith('/')
          ? output
          : path.join(this.config.definitionDir, output)

        fs.mkdirpSync(path.dirname(resolvedOutput))

        if (generator === 'schema') {
          await this.generateSchema(resolvedOutput, schemaString)
        }

        if (generator === 'typescript') {
          await this.generateTypescript(resolvedOutput, schemaString)
        }

        if (generator === 'javascript') {
          await this.generateJavascript(resolvedOutput, schemaString)
        }

        if (generator === 'go') {
          await this.generateGo(resolvedOutput, schemaString)
        }

        if (generator === 'flow') {
          await this.generateFlow(resolvedOutput, schemaString)
        }

        const generators = ["schema", "typescript", "javascript", "go", "flow"]
        if (!generators.includes(generator)) {
          this.out.error(`Please choose one of the supported generators. Possible generators: ${generators.map(g => `${g}`).join(`, `)}`)
        }
      }
    }
  }

  async generateSchema(output: string, schemaString: string) {
    fs.writeFileSync(output, schemaString)
  }

  async generateTypescript(output: string, schemaString: string) {
    const schema = buildSchema(schemaString)

    const generator = new TypescriptGenerator({ schema })
    const endpoint = this.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret
      ? this.replaceEnv(this.definition.rawJson!.secret)
      : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const code = generator.render(options)
    fs.writeFileSync(output, code)
    this.out.log(`Saving Prisma Client (TypeScript) at ${output}`)
  }

  async generateJavascript(output: string, schemaString: string) {
    const schema = buildSchema(schemaString)

    const generator = new TypescriptDefinitionGenerator({ schema })
    const typingsPath = output.replace(/.js$/, '.d.ts')
    this.out.log(`Saving Prisma ORM (Javascript) at ${output} and ${typingsPath}`)
    const endpoint = this.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret
      ? this.replaceEnv(this.definition.rawJson!.secret)
      : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const javascript = generator.renderJavascript(options)
    fs.writeFileSync(output, javascript)

    const typings = generator.renderDefinition(options)
    fs.writeFileSync(typingsPath, typings)
    this.out.log(`Saving Prisma Client (JavaScript) at ${output}`)
  }

  async generateGo(output: string, schemaString: string) {
    const schema = buildSchema(schemaString)

    const generator = new GoGenerator({ schema })

    const endpoint = this.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret
      ? this.replaceEnv(this.definition.rawJson!.secret)
      : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const goCode = generator.render(options)
    fs.writeFileSync(output, goCode)

    this.out.log(`Saving Prisma Client (Go) at ${output}`)
    // Run "go fmt" on the file if user has it installed.
    spawnSync("go", ["fmt", output])
  }

  async generateFlow(output: string, schemaString: string) {
    const schema = buildSchema(schemaString)

    const generator = new FlowGenerator({ schema })

    const endpoint = this.replaceEnv(this.definition.rawJson!.endpoint)
    const secret = this.definition.rawJson.secret
      ? this.replaceEnv(this.definition.rawJson!.secret)
      : null
    const options: any = { endpoint }
    if (secret) {
      options.secret = secret
    }

    const flowCode = generator.render(options)
    fs.writeFileSync(output, flowCode)
    this.out.log(`Saving Prisma Client (Flow) at ${output}`)
  }

  replaceEnv(str) {
    const regex = /\${env:(.*?)}/
    const match = regex.exec(str)
    // tslint:disable-next-line:prefer-conditional-expression
    if (match) {
      return `process.env['${match[1]}']`
    } else {
      return `'${str}'`
    }
  }
}
