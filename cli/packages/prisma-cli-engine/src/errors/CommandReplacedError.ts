import chalk from 'chalk'

export class CommandReplacedError extends Error {
  constructor(oldCmd: string, newCmd: string) {
    super(
      `The ${chalk.bold(oldCmd)} command has been replaced by the ${chalk.bold(
        newCmd,
      )} command.
Get more info with ${chalk.bold.green(`${newCmd} --help`)}`,
    )
  }
}
