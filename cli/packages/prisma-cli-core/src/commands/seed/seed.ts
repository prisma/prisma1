import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import { Importer } from '../import/Importer'
import { Seeder } from './Seeder'
import { prettyTime } from '../../utils/util'
import chalk from 'chalk'
import * as path from 'path'

export default class Seed extends Command {
  static topic = 'seed'
  static description = 'Seed a service with data specified in the prisma.yml'
  static flags: Flags = {
    reset: flags.boolean({
      char: 'r',
      description: 'Reset the service before seeding',
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
  }
  async run() {
    const { reset } = this.flags
    const envFile = this.flags['env-file']

    if (envFile && !fs.pathExistsSync(path.join(this.config.cwd, envFile))) {
      await this.out.error(`--env-file path '${envFile}' does not exist`)
    }

    await this.definition.load(this.flags, envFile)
    const serviceName = this.definition.service!

    const cluster = this.definition.getCluster()
    this.env.setActiveCluster(cluster!)

    await this.client.initClusterClient(
      cluster!,
      serviceName,
      this.definition.stage,
      this.definition.getWorkspace(),
    )

    const seed = this.definition.definition!.seed
    if (!seed) {
      throw new Error(
        `In order to seed, you need to provide a "seed" property in your prisma.yml`,
      )
    }

    const seeder = new Seeder(
      this.definition,
      this.client,
      this.out,
      this.config,
    )

    const seedSource =
      this.definition.definition!.seed!.import ||
      this.definition.definition!.seed!.run

    if (!seedSource) {
      // Await on error to wait for it to set the exit code to 1
      await this.out.error(
        'Invalid seed property in `prisma.yml`. Please use `import` or `run` under the `seed` property. Follow the docs for more info: http://bit.ly/prisma-seed-optional',
      )
    }

    this.out.action.start(`Seeding based on ${chalk.bold(seedSource!)}`)
    const before = Date.now()
    await seeder.seed(
      serviceName,
      this.definition.stage!,
      reset,
      this.definition.getWorkspace()!,
    )
    this.out.action.stop(prettyTime(Date.now() - before))
  }
}
