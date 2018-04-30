import { readDefinition } from '../yaml'
import { PrismaDefinitionClass } from '../PrismaDefinition'
import chalk from 'chalk'
import * as fs from 'fs'
import { migrateToEndpoint } from './yamlComment'

export class DefinitionMigrator {
  prismaDefinition: PrismaDefinitionClass
  constructor(prismaDefinition: PrismaDefinitionClass) {
    this.prismaDefinition = prismaDefinition
  }

  async migrate(definitionPath: string): Promise<boolean> {
    return this.migrateEndpoint(definitionPath)
  }

  private async migrateEndpoint(definitionPath: string): Promise<boolean> {
    const { definition, rawJson } = await readDefinition(
      definitionPath,
      {},
      this.prismaDefinition.out,
      this.prismaDefinition.envVars,
    )
    if (definition.endpoint) {
      // we're fine
      return false
    }

    // check if service, stage and cluster exist and if they're not interpolated. Hence, the equality check
    const endpoint = this.prismaDefinition.getEndpoint()
    if (
      definition.service &&
      definition.stage &&
      definition.cluster &&
      endpoint
    ) {
      const log = this.prismaDefinition.out
        ? this.prismaDefinition.out.log.bind(this.prismaDefinition.out)
        : console.log.bind(console)
      const splittedCluster = definition.cluster.split('/')
      if (
        rawJson.service !== definition.service ||
        rawJson.stage !== definition.stage ||
        rawJson.cluster !== definition.cluster ||
        splittedCluster.length === 1
      ) {
        log(`${chalk.yellow(
          'warning',
        )} prisma.yml: "cluster", "service" and "stage" are deprecated and will be replaced with "endpoint".
To get the endpoint, run ${chalk.cyan(
          'prisma info',
        )}. Read more here: https://bit.ly/migrate-prisma-yml\n`)
        return false
      }

      const definitionString = fs.readFileSync(definitionPath, 'utf-8')
      const backupPath = definitionPath + '.backup'
      if (!fs.existsSync(backupPath)) {
        fs.copyFileSync(definitionPath, backupPath)
      }
      const newDefinitionString = migrateToEndpoint(definitionString, endpoint)
      fs.writeFileSync(definitionPath, newDefinitionString)
      log(`${chalk.yellow(
        'warning',
      )} prisma.yml: "cluster", "service", "stage" is being replaced by "endpoint".
We did this migration for you in your prisma.yml so you don't have to take care of it.
The old prisma.yml has been copied to prisma.yml.backup.

Read more about the migration here: https://bit.ly/migrate-prisma-yml\n`)
      return true
    }

    return false
  }
}
