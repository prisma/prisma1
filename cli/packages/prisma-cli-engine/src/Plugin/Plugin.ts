import { Output } from '../Output/index'
import { CachedCommand, CachedPlugin, CachedTopic, Group } from './Cache'
import { Config } from '../Config'
import { Topic } from '../Topic'
import { PluginPath } from './PluginPath'

const debug = require('debug')('plugin')

export default class Plugin {
  pluginPath: PluginPath
  cachedPlugin: CachedPlugin
  config: Config
  out: Output

  constructor(out: Output, pluginPath: PluginPath, cachedPlugin: CachedPlugin) {
    this.config = out.config
    this.out = out
    this.pluginPath = pluginPath
    this.cachedPlugin = cachedPlugin
  }

  get tag(): string | void {
    return this.pluginPath.tag
  }

  get type(): string {
    return this.pluginPath.type
  }

  get path(): string {
    return this.pluginPath.path
  }

  get name(): string {
    return this.cachedPlugin.name
  }

  get version(): string {
    return this.cachedPlugin.version
  }

  get commands(): CachedCommand[] {
    return this.cachedPlugin.commands
  }

  get groups(): Group[] {
    return this.cachedPlugin.groups
  }

  get topics(): CachedTopic[] {
    return this.cachedPlugin.topics
  }

  async findCommand(id: string): Promise<any> {
    if (!id) {
      return
    }
    const foundCommand = this.commands.find(
      c => c.id === id || (c.aliases || []).includes(id),
    )
    if (!foundCommand) {
      return
    }
    const { topic, command } = foundCommand
    debug('requiring command')
    const p = await this.pluginPath.require()
    debug('required command')
    const Command = (p.commands || []).find(
      d => topic === d.topic && command === d.command,
    )
    return Command
  }

  async findTopic(id: string): Promise<any> {
    const topic = this.topics.find(t => t.id === id)
    if (!topic) {
      return
    }
    const plugin = await this.pluginPath.require()
    const foundTopic = (plugin.topics || []).find(t => [t.id].includes(id))
    if (!foundTopic) {
      return
    }
    return typeof foundTopic === 'function'
      ? foundTopic
      : this.buildTopic(topic)
  }

  buildTopic(t: CachedTopic): any {
    /* tslint:disable */
    return class extends Topic {
      static topic = t.id
      static description = t.description
      static hidden = t.hidden
      static group = t.group
    }
  }
}
