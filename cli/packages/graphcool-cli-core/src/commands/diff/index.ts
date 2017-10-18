import {
  Command,
  flags,
  Flags,
} from 'graphcool-cli-engine'
import chalk from 'chalk'

export default class Diff extends Command {
  static description = 'Receive service changes'
  static group = 'general'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target to be diffed',
    }),
  }

  async run() {
    const {target} = this.flags

    await this.definition.load(this.flags)
    await this.auth.ensureAuth()

    const { id } = await this.env.getTarget(target)
    const targetName = target || 'default'

    this.out.action.start(
      `Getting diff for ${chalk.bold(id)} with target ${chalk.bold(
        targetName,
      )}.`,
    )

    try {
      const migrationResult = await this.client.push(
        id,
        false,
        true,
        this.definition.definition!,
      )
      this.out.action.stop()

      // no action required
      if (
        (!migrationResult.migrationMessages ||
          migrationResult.migrationMessages.length === 0) &&
        (!migrationResult.errors || migrationResult.errors.length === 0)
      ) {
        this.out.log(
          `Identical project definition for project ${chalk.bold(
            id,
          )} in env ${chalk.bold(targetName)}, no action required.\n`,
        )
        return
      }

      if (migrationResult.migrationMessages.length > 0) {
        this.out.log(
          chalk.blue(
            `Your project ${chalk.bold(id)} of env ${chalk.bold(
              targetName,
            )} has the following changes:`,
          ),
        )

        this.out.migration.printMessages(migrationResult.migrationMessages)
        this.definition.set(migrationResult.projectDefinition)
      }

      if (migrationResult.errors.length > 0) {
        this.out.log(
          chalk.rgb(244, 157, 65)(
            `There are issues with the new project definition:`,
          ),
        )
        this.out.migration.printErrors(migrationResult.errors)
        this.out.log('')
      }

      if (
        migrationResult.errors &&
        migrationResult.errors.length > 0 &&
        migrationResult.errors[0].description.includes(`destructive changes`)
      ) {
        // potentially destructive changes
        this.out.log(
          `Your changes might result in data loss.
            Use ${chalk.cyan(
              `\`graphcool deploy --force\``,
            )} if you know what you're doing!\n`,
        )
      }
    } catch (e) {
      this.out.action.stop()
      this.out.error(e)
    }
  }
}
