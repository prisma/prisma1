import {
  Command,
  Flags,
  flags,
  ProjectInfo,
  Output,
  Project,
} from 'graphcool-cli-engine'
import chalk from 'chalk'

export default class Reset extends Command {
  static topic = 'reset'
  static description = 'Reset the stage data'
  static group = 'general'
  static flags: Flags = {
    force: flags.boolean({
      char: 'f',
      description: 'Force reset data without confirmation',
    }),
  }
  async run() {
    const { force } = this.flags

    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage

    const clusterName = this.definition.definition!.cluster
    const cluster = this.env.clusterByName(clusterName!, true)
    this.env.setActiveCluster(cluster!)

    if (!force) {
      await this.askForConfirmation(serviceName, stage)
    }

    await this.reset(serviceName, stage)
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

  private async askForConfirmation(serviceName: string, stage: string) {
    const confirmationQuestion = {
      name: 'confirmation',
      type: 'input',
      message: `Are you sure that you want to reset the data of ${chalk.bold(
        serviceName,
      )} in stage ${chalk.bold(stage)}? y/N`,
      default: 'n',
    }
    const { confirmation }: { confirmation: string } = await this.out.prompt(
      confirmationQuestion,
    )
    if (confirmation.toLowerCase().startsWith('n')) {
      this.out.exit(0)
    }
  }
}
