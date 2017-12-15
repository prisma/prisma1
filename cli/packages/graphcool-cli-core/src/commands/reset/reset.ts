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
    stage: flags.string({
      char: 's',
      description: 'Stage name to reset data of',
    }),
    force: flags.boolean({
      char: 'f',
      description: 'Force reset data without confirmation',
    }),
  }
  async run() {
    const { stage, force } = this.flags

    await this.definition.load(this.flags)
    const clusterName = this.definition.getStage(stage, true)
    const stageName = stage || this.definition.rawStages.default
    const cluster = this.env.clusterByName(clusterName!, true)!
    const serviceName = this.definition.definition!.service
    this.env.setActiveCluster(cluster)

    if (!force) {
      await this.askForConfirmation(serviceName, stageName)
    }

    await this.reset(serviceName, stageName)
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
