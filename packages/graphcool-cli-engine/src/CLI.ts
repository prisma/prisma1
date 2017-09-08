import * as path from 'path'
import { Command } from './Command'
import {Config} from './Config'
import {Output} from './Output'
import { RunOptions } from './types';
import Lock from './Plugin/Lock'
import { Dispatcher } from './Dispatcher/Dispatcher'
import { NotFound } from './NotFound'

const debug = require('debug')('cli')
const handleEPIPE = err => { if (err.code !== 'EPIPE') {
   throw err
}}

let out: Output
if (!global.testing) {
  process.once('SIGINT', () => {
    if (out) {
      if (out.action.task) {
        out.action.stop(out.color.red('ctrl-c'))
      }
      out.exit(1)
    } else {
      process.exit(1)
    }
  })
  const handleErr = err => {
    if (!out) {
      throw err
    }
    out.error(err)
  }
  process.once('uncaughtException', handleErr)
  process.once('unhandledRejection', handleErr)
  process.stdout.on('error', handleEPIPE)
  process.stderr.on('error', handleEPIPE)
}

process.env.CLI_ENGINE_VERSION = require('../package.json').version

export class CLI {
  config: Config
  cmd: Command

  constructor ({config}: {config?: RunOptions} = {}) {
    if (!config) {
      config = {
        mock: false
      }
    }
    if (!config.initPath) {
      config.initPath = module.parent!.parent!.filename
    }
    if (!config.root) {
      const findUp = require('find-up')
      config.root = path.dirname(findUp.sync('package.json', {
        cwd: module.parent!.parent!.filename
      }))
    }
    this.config = new Config(config)
  }

  async run () {
    debug('starting run')

    out = new Output(this.config)

    if (this.cmdAskingForHelp) {
      debug('running help command')
      this.cmd = await this.Help.run(this.config)
    } else {
      debug('dispatcher')
      // CONTINUE!!!
      // here the magic happens!!!
      // here we have to convert the spaces to colons until the first flag
      debug(`argv:`, this.config.argv)
      const id = this.config.argv[1]
      const dispatcher = new Dispatcher(this.config)
      const result = await dispatcher.findCommand(id || this.config.defaultCommand || 'help')
      const {plugin} = result
      const foundCommand = result.Command

      if (foundCommand) {
        const lock = new Lock(out)
        await lock.unread()
        debug('running cmd')
        this.cmd = await foundCommand.run(this.config)
      } else {
        const topic = await dispatcher.findTopic(id)
        if (topic) {
          await this.Help.run(this.config)
        } else {
          return new NotFound(out, this.config.argv).run()
        }
      }
    }

    debug('flushing stdout')
    const {timeout} = require('./util')
    await timeout(this.flush(), 10000)
    debug('exiting')
    out.exit(0)
  }

  flush (): Promise<{} | void> {
    if (global.testing) {
      return Promise.resolve()
    }
    const p = new Promise(resolve => process.stdout.once('drain', resolve))
    process.stdout.write('')
    return p
  }

  get cmdAskingForHelp (): boolean {
    for (const arg of this.config.argv) {
      if (['--help', '-h'].includes(arg)) {
        return true
      }
      if (arg === '--') {
        return false
      }
    }
    return false
  }

  get Help () {
    const {default: Help} = require('./commands/help')
    return Help
  }
}

export function run ({config}: {config?: RunOptions} = {}) {
  if (!config) {
    config = {
      mock: false
    }
  }
  const cli = new CLI({config})
  return cli.run()
}
