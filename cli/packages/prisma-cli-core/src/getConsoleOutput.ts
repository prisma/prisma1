import chalk from 'chalk'
import * as path from 'path'

export default function getConsoleOutput(
  root: string,
  verbose: boolean,
  buffer: any,
) {
  const TITLE_INDENT = verbose ? '  ' : '    '
  const CONSOLE_INDENT = TITLE_INDENT + '  '

  return buffer.reduce((output, { type, message, origin }) => {
    origin = path.relative(root, origin)
    message = message
      .split(/\n/)
      .map(line => CONSOLE_INDENT + line)
      .join('\n')

    let typeMessage = 'console.' + type
    if (type === 'warn') {
      message = chalk.yellow(message)
      typeMessage = chalk.yellow(typeMessage)
    } else if (type === 'error') {
      message = chalk.red(message)
      typeMessage = chalk.red(typeMessage)
    }

    return (
      output +
      TITLE_INDENT +
      chalk.dim(typeMessage) +
      ' ' +
      chalk.dim(origin) +
      '\n' +
      message +
      '\n'
    )
  }, '')
}
