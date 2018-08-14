import { Command, flags, Flags } from 'prisma-cli-engine'
// import { sync } from 'cross-spawn'
import { spawnSync } from 'npm-run'
import { prettyTime, concatName } from '../../util'
import { fetchAndPrintSchema } from '../deploy/printSchema'
import * as fs from 'fs-extra'
import * as path from 'path'
import { getTmpDir } from './getTmpDir'

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
      this.env.setActiveCluster(cluster!)

      await this.client.initClusterClient(
        cluster!,
        serviceName,
        this.definition.stage,
        this.definition.getWorkspace(),
      )
      const schemaString = await fetchAndPrintSchema(
        this.client,
        serviceName,
        stageName!,
        token,
      )
      this.out.action.stop(prettyTime(Date.now() - before))
      for (const { generator, output } of this.definition.definition
        .generate!) {
        const resolvedOutput = fs.pathExistsSync(output)
          ? path.resolve(output)
          : path.join(this.config.cwd, output)

        fs.mkdirpSync(path.dirname(resolvedOutput))

        if (generator === 'schema') {
          await this.generateSchema(resolvedOutput, schemaString)
        }

        if (generator === 'typescript') {
          await this.generateTypescript(resolvedOutput, schemaString)
        }
      }
    }
  }

  async generateSchema(output: string, schemaString: string) {
    fs.writeFileSync(output, schemaString)
  }

  async generateTypescript(output: string, schemaString: string) {
    this.out.log(`Saving Prisma ORM (TypeScript) at ${output}`)
    const tmpDir = getTmpDir()
    const tmpSchemaPath = path.join(tmpDir, 'schema.graphql')
    fs.writeFileSync(tmpSchemaPath, schemaString)
    const args = ['-i', tmpSchemaPath, '--language', 'typescript', '-b', output]
    // const binPath = path.join(
    //   __dirname,
    //   '../../../../../node_modules/prisma-binding/dist/bin.js',
    // )
    // const child = sync(binPath, args)
    // const binPath = path.join(
    //   __dirname,
    //   '../../../../../node_modules/prisma-binding/dist/bin.js',
    // )
    const child = spawnSync(this.getPrismaBindingBinPath(), args)

    if (child.error) {
      this.out.error(child.error)
    }
  }

  getPrismaBindingBinPath() {
    let dots = ''
    let count = 0
    while (count < 6) {
      const modulePath = path.join(
        __dirname,
        dots + 'node_modules/prisma-binding/dist/bin.js',
      )
      if (fs.pathExistsSync(modulePath)) {
        return modulePath
      }
      count++
      dots = `../${dots}`
    }
    return 'prisma-binding'
  }
}
