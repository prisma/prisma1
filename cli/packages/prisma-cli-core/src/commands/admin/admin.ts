import { Command, flags, Flags } from 'prisma-cli-engine'
import chalk from 'chalk'
import * as opn from 'opn'
import { satisfiesVersion } from '../../utils/satisfiesVersion'

export default class Admin extends Command {
  static topic = 'admin'
  static description = 'Open service endpoints in Prisma Admin'

  static flags: Flags = {
    'env-file': flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
  }

  async run() {
    const envFile = this.flags['env-file']
    await this.definition.load(this.flags, envFile)

    const serviceName = this.definition.service!
    const stage = this.definition.stage!

    const token = this.definition.getToken(serviceName, stage)
    const cluster = await this.definition.getCluster(false)
    const clusterVersion = await cluster!.getVersion()

    if (satisfiesVersion(clusterVersion!, '1.29.0')) {
      let adminUrl = this.definition.endpoint + '/_admin'
      if (token && token.length > 0) {
        adminUrl += `?token=${token}`
      }
      this.out.log(`Opening Prisma Admin ${adminUrl} in the browser`)
      opn(adminUrl).catch(() => {})
    } else {
      this.out.log(`Your Prisma server at ${chalk.bold(
        `${this.definition.endpoint}`,
      )} doesn't support Prisma Admin yet. Prisma Admin is supported from Prisma ${chalk.green(
        `1.29`,
      )} and higher. Your Prisma server currently uses Prisma ${chalk.red(
        `${clusterVersion}`,
      )}.\n\n
Please upgrade your Prisma server to use Prisma Admin.`)
      this.out.exit(1)
    }
  }

  normalizeVersion(version: string) {
    version = version.replace(/-beta.*/, '').replace('-alpha', '')
    const regex = /(\d+\.\d+)/
    const match = regex.exec(version)
    if (match) {
      return match[1] + '.0'
    }
    return version
  }
}
