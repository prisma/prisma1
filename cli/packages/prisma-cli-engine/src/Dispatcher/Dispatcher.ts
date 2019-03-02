/* tslint:disable */
import * as path from 'path'
import { undefault } from '../util'
import { Config } from '../Config'
import { Output } from '../Output/index'
import Plugins from '../Plugin/Plugins'

export class CommandManagerBase {
  config: Config
  constructor(config: Config) {
    this.config = config
  }
  async findCommand(id: string): Promise<any> {
    return null
  }
  async listTopics(prefix?: string): Promise<string[]> {
    return []
  }
  async findTopic(id: string): Promise<any> {
    return null
  }

  require(p: string): any {
    return undefault(require(p))
  }
}

export class BuiltinCommandManager extends CommandManagerBase {
  async findCommand(id) {
    const builtins = {
      version: 'version',
      help: 'help',
    }

    let p = builtins[id]
    if (p) {
      p = path.join(__dirname, '../commands', p)
      return this.require(p)
    }
  }
  async listTopics(prefix?: string) {
    return ['version', 'help']
  }
}

export class CLICommandManager extends CommandManagerBase {
  async findCommand(id) {
    let root = this.config.commandsDir
    if (!root) return
    let p
    try {
      p = require.resolve(path.join(root, ...id.split(':')))
    } catch (err) {
      if (err.code !== 'MODULE_NOT_FOUND') throw err
    }
    if (p) return this.require(p)
  }
}
// TODO look into this later: https://sourcegraph.com/github.com/heroku/cli-engine/-/blob/src/plugins/index.js#L9:33
// not needed right now
//
class PluginCommandManager extends CommandManagerBase {
  async findCommand(id) {
    let out = new Output(this.config)
    let plugins = new Plugins(out)
    await plugins.load()
    const foundCommand = plugins.findCommand(
      id || this.config.defaultCommand || 'help',
    )
    return foundCommand
  }
  async findTopic(id: string) {
    let out = new Output(this.config)
    let plugins = new Plugins(out)
    await plugins.load()
    return plugins.findTopic(id)
  }
}

export class Dispatcher {
  config: Config
  managers: CommandManagerBase[]

  constructor(config: Config) {
    this.config = config
    this.managers = [
      new CLICommandManager(config),
      new BuiltinCommandManager(config),
      new PluginCommandManager(config),
    ]
  }

  async findCommand(
    id: string,
  ): Promise<{
    Command?: any
    plugin?: Plugin
  }> {
    if (!id) return {}
    for (let manager of this.managers) {
      let Command = await manager.findCommand(id)
      if (Command) return { Command }
    }
    return {}
  }

  async findTopic(id: string) {
    if (!id) return {}
    // TODO: Fix this hack for "cluster".
    // Find why cache does not invalidate for cluster command
    if (id.trim() === 'cluster') return null
    for (let manager of this.managers) {
      let topic = await manager.findTopic(id)
      if (topic) return topic
    }
    return null
  }

  async listTopics(prefix?: string) {
    let arrs = await Promise.all(this.managers.map(m => m.listTopics(prefix)))
    return arrs.reduce((next, res) => res.concat(next), [])
  }

  get cmdAskingForHelp(): boolean {
    for (let arg of this.config.argv) {
      if (['--help', '-h'].includes(arg)) return true
      if (arg === '--') return false
    }
    return false
  }
}
