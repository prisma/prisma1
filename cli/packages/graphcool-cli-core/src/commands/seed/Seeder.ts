import { Client, Config, Output } from 'graphcool-cli-engine'
import { GraphcoolDefinitionClass } from 'graphcool-yml'
import { Importer } from '../import/Importer'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'
import chalk from 'chalk'

export class Seeder {
  definition: GraphcoolDefinitionClass
  client: Client
  out: Output
  config: Config
  constructor(
    definition: GraphcoolDefinitionClass,
    client: Client,
    out: Output,
    config: Config,
  ) {
    this.definition = definition
    this.client = client
    this.out = out
    this.config = config
  }

  async seed(serviceName: string, stageName: string, reset: boolean = false) {
    const seed = this.definition.definition!.seed
    if (!seed) {
      throw new Error(
        `In order to seed, you need to provide a "seed" property in your graphcool.yml`,
      )
    }
    if (seed.import && seed.run) {
      throw new Error(
        `Please provider either seed.import or seed.run but not both at the same time`,
      )
    }

    if (seed.import) {
      const source = seed.import

      if (!source.endsWith('.zip')) {
        throw new Error(`Source must end with .zip`)
      }

      if (!fs.pathExistsSync(source)) {
        throw new Error(`Path ${source} does not exist`)
      }

      const token = this.definition.getToken(serviceName, stageName)

      if (reset) {
        await this.reset(serviceName, stageName)
      }

      await this.import(seed.import, serviceName, stageName, token)
    }

    if (seed.run) {
      await this.run(seed.run)
    }
  }

  async reset(serviceName, stageName) {
    const before = Date.now()
    this.out.action.start(
      `Resetting ${chalk.bold(`${serviceName}@${stageName}`)}`,
    )
    await this.client.reset(
      serviceName,
      stageName,
      this.definition.getToken(serviceName, stageName),
    )
    this.out.action.stop(chalk.cyan(`${Date.now() - before}ms`))
  }

  private run(cmd: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.out.log(chalk.dim(`$ ${cmd}`))
      const args = cmd.split(/\s/g)
      const child = childProcess.spawn(args[0], args.slice(1), {
        cwd: this.config.cwd,
      })
      child.stdout.on('data', data => {
        this.out.log(data)
      })
      child.stderr.on('data', data => {
        this.out.log(data)
      })
      child.on('error', err => {
        this.out.error(err)
      })
      child.on('close', code => {
        if (code !== 0) {
          reject(code)
        } else {
          resolve()
        }
      })
    })
  }

  private async import(
    source: string,
    serviceName: string,
    stage: string,
    token?: string,
  ) {
    await this.definition.load({})
    const typesString = this.definition.typesString!
    const importer = new Importer(
      source,
      typesString,
      this.client,
      this.out,
      this.config,
    )
    await importer.upload(serviceName, stage, token)
  }
}
