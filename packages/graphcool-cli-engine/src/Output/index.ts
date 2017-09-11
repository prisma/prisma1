import { Config } from '../Config'
import StreamOutput, { logToFile } from './StreamOutput'
import * as chalk from 'chalk'
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
import * as inquirer from 'graphcool-inquirer'
import { MigrationPrinter } from './migration'
import * as treeify from 'treeify'
import * as dirTree from 'directory-tree'

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
  app: (s: string) => CustomColors.graphcool(`⬢ ${s}`),
  graphcool: (s: string) => {
    if (!CustomColors.supports) {
      return s
    }
    const has256 = CustomColors.supports.has256 || (process.env.TERM || '').indexOf('256') !== -1
    return has256 ? '\u001b[38;5;104m' + s + styles.reset.open : chalk.magenta(s)
  }
}

function wrap(msg: string): string {
  const linewrap = require('@heroku/linewrap')
  return linewrap(6,
    errtermwidth, {
      skipScheme: 'ansi-color',
      skip: /^\$ .*$/
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

function getErrorMessage(err: any): string {
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
    message = `${util.inspect(err.code)}: ${err.message}`
  } else if (err.message) {
    message = err.message
  }
  return message || util.inspect(err)
}

const arrow = process.platform === 'win32' ? '!' : '▸'

export class Output {
  mock: boolean = true
  config: Config
  action: ActionBase
  stdout: StreamOutput
  stderr: StreamOutput
  prompter: Prompter
  prompt: any
  migration: MigrationPrinter

  constructor(config: Config) {
    this.config = config
    this.mock = config.mock
    this.stdout = new StreamOutput(process.stdout, this)
    this.stderr = new StreamOutput(process.stderr, this)
    this.action = shouldDisplaySpinner(this) ? new SpinnerAction(this) : new SimpleAction(this)
    if (this.mock) {
      chalk.enabled = false
      CustomColors.supports = false
    }
    this.prompter = new Prompter(this)
    this.prompt = inquirer.createPromptModule({ output: process.stdout })
    this.migration = new MigrationPrinter(this)
  }

  get color(): chalk & { graphcool: (s: string) => string } {
    return new Proxy(chalk, {
      get: (chalkProxy, name) => {
        if (CustomColors[name]) {
          return CustomColors[name]
        }
        return chalkProxy[name]
      }
    })
  }

  log(data, ...args: any[]) {
    this.stdout.log(data, ...args)
  }

  getStyledJSON(obj: any) {
    const json = JSON.stringify(obj, null, 2)
    if (chalk.enabled) {
      const cardinal = require('cardinal')
      const theme = require('cardinal/themes/jq')
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

  error(err: Error | string, exitCode: number | false = 1) {
    if (this.mock && typeof err !== 'string' && exitCode !== false) {
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
        this.stderr.log(bangify(wrap(getErrorMessage(err)), this.color.red(arrow)))
      }
    } catch (e) {
      console.error('error displaying error')
      console.error(e)
      console.error(err)
    }
    if (exitCode !== false) {
      this.exit(exitCode)
    }
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
          this.stderr.log(bangify(wrap(prefix + getErrorMessage(err)), this.color.yellow(arrow)))
        }
      } catch (e) {
        console.error('error displaying warning')
        console.error(e)
        console.error(err)
      }
    }, this.color.bold.yellow('!'))
  }

  logError(err: Error | string) {
    logToFile(util.inspect(err) + '\n', this.errlog)
  }

  oldprompt(name: string, options?: PromptOptions) {
    return this.prompter.prompt(name, options)
  }

  table<T = { height?: number }>(data: T[], options: TableOptions<T>) {
    const table = require('./table')
    return table(this, data, options)
  }

  exit(code: number = 0) {
    if (this.config.debug) {
      console.error(`Exiting with code: ${code}`)
    }
    if (this.mock) {
      throw new ExitError(code);
    } else {
      process.exit(code)
    }
  }

  tree(dirPath: string, padding = false) {
    const tree = dirTree(dirPath)
    const convertedTree = treeConverter(tree)
    const printedTree = treeify.asTree(convertedTree, true)
    this.log(chalk.blue(printedTree.split('\n').map(l => (padding ? '   ' : '') + l).join('\n')))
  }
}

function treeConverter(tree) {
  if (!tree.children) {
    return tree.name
  }
  return tree.children.reduce((acc, curr) => {
    if (!curr.children) {
      return {...acc, [curr.name]: null}
    }
    return {...acc, [curr.name]: treeConverter(curr)}
  }, {})
}
