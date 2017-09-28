import * as path from 'path'
import { Command } from './Command'
import { Config } from './Config'
import { Output } from './Output'
import { RunOptions } from './types'
import Lock from './Plugin/Lock'
import { Dispatcher } from './Dispatcher/Dispatcher'
import { NotFound } from './NotFound'
import * as nock from 'nock'
import fs from './fs'

const debug = require('debug')('cli')
const handleEPIPE = err => {
  if (err.code !== 'EPIPE') {
    throw err
  }
}

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

  constructor({ config }: { config?: RunOptions } = {}) {
    if (!config) {
      config = {
        mock: false,
      }
    }
    const parentFilename = module.parent!.parent!
      ? module.parent!.parent!.filename
      : module.parent!.filename
    if (!config.initPath) {
      config.initPath = parentFilename
    }
    if (!config.root) {
      const findUp = require('find-up')
      config.root = path.dirname(
        findUp.sync('package.json', {
          cwd: parentFilename,
        }),
      )
    }
    this.config = new Config(config)
  }

  async run() {
    debug('starting run')

    out = new Output(this.config)

    if (this.cmdAskingForHelp) {
      debug('running help command')
      this.cmd = await this.Help.run(this.config)
    } else {
      debug('dispatcher')
      const id = this.getCommandId(this.config.argv.slice(1))
      // if there is a subcommand, cut the first away so the Parser still works correctly
      if (
        this.config.argv[1] &&
        this.config.argv[1].startsWith('-') &&
        id !== 'help' &&
        id !== 'init'
      ) {
        this.config.argv = this.config.argv.slice(1)
      }
      debug(`command id: ${id}, argv: ${this.config.argv}`)
      const dispatcher = new Dispatcher(this.config)
      const result = await dispatcher.findCommand(
        id || this.config.defaultCommand || 'help',
      )
      const { plugin } = result
      const foundCommand = result.Command

      if (foundCommand) {
        const lock = new Lock(out)
        await lock.unread()
        debug('running cmd')
        // TODO remove this
        if (process.env.NOCK_WRITE_RESPONSE_CLI) {
          debug('RECORDING')
          nock.recorder.rec({
            dont_print: true,
          })
        }
        this.cmd = await foundCommand.run(this.config)
        if (process.env.NOCK_WRITE_RESPONSE_CLI) {
          const requests = nock.recorder.play()
          const requestsPath = path.join(process.cwd(), 'requests.js')
          debug('WRITING', requestsPath)
          fs.writeFileSync(requestsPath, requests.join('\n'))
        }
      } else {
        const topic = await dispatcher.findTopic(id)
        if (topic) {
          await this.Help.run(this.config)
        } else {
          return new NotFound(out, this.config.argv).run()
        }
      }
    }

    if (
      !this.config.argv.includes('logs') &&
      !this.config.argv.includes('logs:function')
    ) {
      debug('flushing stdout')
      const { timeout } = require('./util')
      await timeout(this.flush(), 5000)

      debug('exiting')
      out.exit(0)
    }
  }

  flush(): Promise<{} | void> {
    if (global.testing) {
      return Promise.resolve()
    }
    const p = new Promise(resolve => process.stdout.once('drain', resolve))
    process.stdout.write('')
    return p
  }

  get cmdAskingForHelp(): boolean {
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

  get Help() {
    const { default: Help } = require('./commands/help')
    return Help
  }

  private getCommandId(argv: string[]) {
    if (argv.includes('help') || argv.includes('init')) {
      return argv[0]
    }
    if (argv[1] && !argv[1].startsWith('-')) {
      return argv.slice(0, 2).join(':')
    } else {
      const firstFlag = argv.findIndex(param => param.startsWith('-'))
      if (firstFlag === -1) {
        return argv.join(':')
      } else {
        return argv.slice(0, firstFlag).join(':')
      }
    }
  }
}

export function run({ config }: { config?: RunOptions } = {}) {
  if (!config) {
    config = {
      mock: false,
    }
  }
  const cli = new CLI({ config })
  return cli.run()
}
