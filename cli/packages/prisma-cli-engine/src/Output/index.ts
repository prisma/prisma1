import { Config } from '../Config'
import StreamOutput, { logToFile } from './StreamOutput'
import chalk, { Chalk } from 'chalk'
import * as styles from 'ansi-styles'
import { ActionBase, shouldDisplaySpinner } from './actions/ActionBase'
import Prompter, { PromptOptions } from './Prompter'
import * as supports from 'supports-color'
import { SpinnerAction } from './actions/SpinnerAction'
import { SimpleAction } from './actions/SimpleAction'
import * as path from 'path'
import * as util from 'util'
import { errtermwidth } from './actions/screen'
import { TableOptions } from './table'
import ExitError from '../errors/ExitError'
import * as inquirer from 'inquirer'
import { MigrationPrinter } from './migration'
import * as treeify from 'treeify'
import * as dirTree from 'directory-tree'
import * as marked from 'marked'
import * as TerminalRenderer from 'marked-terminal'
import * as Charm from 'charm'
import { padEnd, repeat, set, uniqBy, values } from 'lodash'
import { Project, Stages } from '../types/common'
import * as Raven from 'raven'
import { getStatusChecker } from '../StatusChecker'
const debug = require('debug')('output')

marked.setOptions({
  renderer: new TerminalRenderer(),
})

export const CustomColors = {
  // map gray -> dim because it's not solarized compatible
  supports,
  gray: (s: string) => chalk.dim(s),
  grey: (s: string) => chalk.dim(s),
  attachment: (s: string) => chalk.cyan(s),
  addon: (s: string) => chalk.yellow(s),
  configVar: (s: string) => chalk.green(s),
  release: (s: string) => chalk.blue.bold(s),
  cmd: (s: string) => chalk.green.bold(s),
  app: (s: string) => CustomColors.prisma(`⬢ ${s}`),
  prisma: (s: string) => {
    if (!CustomColors.supports) {
      return s
    }
    const has256 =
      CustomColors.supports.has256 ||
      (process.env.TERM || '').indexOf('256') !== -1
    return has256
      ? '\u001b[38;5;104m' + s + styles.reset.open
      : chalk.magenta(s)
  },
}

function wrap(msg: string): string {
  const linewrap = require('@heroku/linewrap')
  return linewrap(6, errtermwidth, {
    skipScheme: 'ansi-color',
    skip: /^\$ .*$/,
  })(msg)
}

function bangify(msg: string, c: string): string {
  const lines = msg.split('\n')
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    lines[i] = ' ' + c + line.substr(2, line.length)
  }
  return lines.join('\n')
}

function extractMessage(response): string {
  try {
    return response.errors![0].message
  } catch (e) {
    return `GraphQL Error (Code: ${response.status})`
  }
}

const arrow = process.platform === 'win32' ? '!' : '▸'

export class Output {
  mock: boolean = false
  config: Config
  action: ActionBase
  stdout: StreamOutput
  stderr: StreamOutput
  prompter: Prompter
  prompt: any
  migration: MigrationPrinter
  charm: any

  constructor(config: Config) {
    this.config = config
    if (config && config.mock) {
      this.mock = config.mock
    }
    this.stdout = new StreamOutput(process.stdout, this)
    this.stderr = new StreamOutput(process.stderr, this)
    this.action = shouldDisplaySpinner(this)
      ? new SpinnerAction(this)
      : new SimpleAction(this)
    if (this.mock) {
      chalk.enabled = false
      CustomColors.supports = false
    }
    this.prompter = new Prompter(this)
    this.prompt =
      (this.config &&
        this.config.mockInquirer &&
        this.config.mockInquirer.prompt) ||
      inquirer.createPromptModule({
        output: process.stdout,
      })
    this.migration = new MigrationPrinter(this)
    this.charm = Charm()
    this.charm.pipe(process.stdout)
  }

  get color(): any {
    return new Proxy(chalk, {
      get: (chalkProxy, name) => {
        if (CustomColors[name]) {
          return CustomColors[name]
        }
        return chalkProxy[name]
      },
    })
  }

  log(data, ...args: any[]) {
    this.stdout.log(data, ...args)
  }

  getStyledJSON(obj: any, subtle: boolean = false) {
    const json = JSON.stringify(obj, null, 2)
    if (chalk.enabled) {
      const cardinal = require('cardinal')
      const theme = require(subtle ? './subtle' : 'cardinal/themes/jq')
      return cardinal.highlight(json, { json: true, theme })
    } else {
      return json
    }
  }

  async done(...rest: void[]): Promise<void> {
    this.action.stop()
  }

  debug(obj: string) {
    if (this.config.debug) {
      this.action.pause(() => console.log(obj))
    }
  }

  get errlog(): string {
    return path.join(this.config.cacheDir, 'error.log')
  }

  get autoupdatelog(): string {
    return path.join(this.config.cacheDir, 'autoupdate.log')
  }

  async error(err: Error | string, exitCode: number | false = 1) {
    if (process.env.NODE_ENV === 'test') {
      console.error(err)
      throw err
    }
    try {
      if (typeof err === 'string') {
        err = new Error(err)
      }
      this.logError(err)
      if (this.action.task) {
        this.action.stop(this.color.bold.red('!'))
      }
      if (this.config.debug) {
        this.stderr.log(err.stack || util.inspect(err))
      } else {
        this.stderr.log(
          this.isGraphQLError(err)
            ? this.getGraphQLErrorMessage(err)
            : bangify(wrap(this.getErrorMessage(err)), this.color.red(arrow)),
        )
        const instruction =
          process.env.SHELL && process.env.SHELL!.endsWith('fish')
            ? '$ set -x DEBUG "*"'
            : '$ export DEBUG="*"'
        this.stderr.log(
          `\nGet in touch if you need help: https://slack.prisma.io
To get more detailed output, run ${chalk.dim(instruction)}`,
        )
      }
    } catch (e) {
      console.error('error displaying error')
      console.error(e)
      console.error(err)
    }
    // make sure error is logged first, then execute raven
    const statusChecker = getStatusChecker()
    if (statusChecker) {
      statusChecker.checkStatus(
        process.argv[2],
        {},
        {},
        process.argv.slice(2),
        err,
      )
    }
    await new Promise(r => {
      Raven.captureException(err, () => r())
    })
    if (exitCode !== false) {
      this.exit(exitCode)
    }
  }

  isGraphQLError(err) {
    return err.message && err.request && err.response
  }

  warn(err: Error | string, prefix?: string) {
    this.action.pause(() => {
      try {
        prefix = prefix ? `${prefix} ` : ''
        err = typeof err === 'string' ? new Error(err) : err
        this.logError(err)
        if (this.config.debug) {
          if (process.stderr.write(`WARNING: ${prefix}`)) {
            this.stderr.log(err.stack || util.inspect(err))
          }
        } else {
          this.stderr.log(
            bangify(
              wrap(prefix + this.getErrorMessage(err)),
              this.color.yellow(arrow),
            ) + '\n',
          )
        }
      } catch (e) {
        console.error('error displaying warning')
        console.error(e)
        console.error(err)
      }
    }, this.color.bold.yellow('!'))
  }

  getErrorPrefix(fileName: string, type: 'error' | 'warning' = 'error') {
    const method = type === 'error' ? 'red' : 'yellow'
    return chalk[method](`[${type.toUpperCase()}] in ${chalk.bold(fileName)}: `)
  }

  logError(err: Error | string) {
    logToFile(util.inspect(err) + '\n', this.errlog)
  }

  printMarkdown(markdown: string) {
    this.log(marked(markdown))
  }

  oldprompt(name: string, options?: PromptOptions) {
    return this.prompter.prompt(name, options)
  }

  table<T = { height?: number }>(data: T[], options?: TableOptions<T>) {
    const table = require('./table')
    return table(this, data, options)
  }

  exit(code: number = 0) {
    debug(`Exiting with code: ${code}`)
    if (this.mock && process.env.NODE_ENV === 'test') {
      throw new ExitError(code)
    } else {
      process.exit(code)
    }
  }

  filesTree(files: string[]) {
    const tree = filesToTree(files)
    const printedTree = treeify.asTree(tree, true)
    this.log(chalk.dim(printedTree.split('\n').join('\n')))
  }

  tree(dirPath: string, padding = false) {
    const tree = dirTree(dirPath)
    const convertedTree = treeConverter(tree)
    const printedTree = treeify.asTree(convertedTree, true)
    this.log(
      chalk.blue(
        printedTree
          .split('\n')
          .map(l => (padding ? '  ' : '') + l)
          .join('\n'),
      ),
    )
  }

  up(y: number = 1) {
    for (let i = 0; i < y; i++) {
      this.charm.delete('line', 1)
      this.charm.up(1)
    }
  }

  printPadded(
    arr1: string[][],
    spaceLeft: number = 0,
    spaceBetween: number = 1,
    header?: string[],
  ) {
    const inputRows = arr1
    if (header) {
      inputRows.unshift(header)
    }
    const leftCol = inputRows.map(a => a[0])
    const rightCol = inputRows.map(a => a[1])
    const maxLeftCol = leftCol.reduce(
      (acc, curr) => Math.max(acc, curr.length),
      -1,
    )
    const maxRightCol = rightCol.reduce(
      (acc, curr) => Math.max(acc, curr.length),
      -1,
    )
    const paddedLeftCol = leftCol.map(
      v => repeat(' ', spaceLeft) + padEnd(v, maxLeftCol + spaceBetween),
    )

    const rows = paddedLeftCol.map((l, i) => l + arr1[i][1])

    if (header) {
      const divider = `${repeat('─', maxLeftCol)}${repeat(
        ' ',
        spaceBetween,
      )}${repeat('─', maxRightCol)}`
      rows.splice(1, 0, divider)
    }

    return rows.join('\n')
  }
  getGraphQLErrorMessage(err: any) {
    if (this.mock) {
      return (
        `\n${chalk.bold.red('ERROR: ' + extractMessage(err.response))}\n\n` +
        chalk.bold('Request') +
        this.getStyledJSON(err.request, true) +
        chalk.bold('Response') +
        this.getStyledJSON(err.response, true)
      )
    } else {
      return (
        `\n${chalk.bold.red('ERROR: ' + extractMessage(err.response))}\n\n` +
        this.getStyledJSON(err.response, true)
      )
    }
  }
  getErrorMessage(err: any): string {
    let message

    if (err.body) {
      // API error
      if (err.body.message) {
        message = util.inspect(err.body.message)
      } else if (err.body.error) {
        message = util.inspect(err.body.error)
      }
    }
    // Unhandled error
    if (err.message && err.code) {
      message = `${chalk.bold(util.inspect(err.code))}: ${err.message}`
    } else if (err.message) {
      message = err.message
    }
    return message || util.inspect(err)
  }
}

function treeConverter(tree) {
  if (!tree.children) {
    return tree.name
  }
  return tree.children.reduce((acc, curr) => {
    if (!curr.children) {
      return { ...acc, [curr.name]: null }
    }
    return { ...acc, [curr.name]: treeConverter(curr) }
  }, {})
}

function filesToTree(files: string[]) {
  const fileNames = files.map(l => {
    if (l.startsWith('./')) {
      return l.slice(2)
    }

    return l
  })

  const obj = {}

  fileNames.forEach(fileName => {
    const setPath = fileName.split('/')
    set(obj, setPath, null)
  })

  return obj
}
