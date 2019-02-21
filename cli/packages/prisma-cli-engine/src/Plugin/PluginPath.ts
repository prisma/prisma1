import 'source-map-support/register'
import { Output } from '../Output/index'
import { ParsedPlugin, PluginType } from './Manager'
import { Config } from '../Config'
import { CachedCommand, CachedPlugin, CachedTopic, Group } from './Cache'
import { Arg, Flag } from '../Flags/index'
import * as path from 'path'

const debug = require('debug')('cli-engine:plugins:manager')

export interface PluginPathOptions {
  output: Output
  type: PluginType
  path: string
  tag?: string
}

export interface ParsedTopic {
  id: string
  name?: string
  topic?: string
  description?: string
  hidden?: boolean
  group: string
}

export interface ParsedCommand {
  id: string
  topic: string
  command?: string
  aliases?: string[]
  variableArgs?: boolean
  args: Arg[]
  flags: { [name: string]: Flag<any> }
  description?: string
  help?: string
  usage?: string
  hidden?: boolean
  default?: boolean
  group: string
}

export class PluginPath {
  out: Output
  config: Config
  path: string
  type: PluginType
  tag: string | void

  constructor(options: PluginPathOptions) {
    this.out = options.output
    this.path = options.path
    this.type = options.type
    this.tag = options.tag

    this.config = this.out.config
    // process.env.CACHE_REQUIRE_PATHS_FILE = this.config.requireCachePath
    // require('cache-require-paths')
  }

  async convertToCached(): Promise<CachedPlugin> {
    const plugin: ParsedPlugin = await this.require()

    const getAliases = (c: ParsedCommand) => {
      const aliases = c.aliases || []
      if (c.default) {
        this.out.warn(`default setting on ${c.topic} is deprecated`)
        aliases.push(c.topic)
      }
      return aliases
    }

    if (!plugin.commands) {
      throw new Error('no commands found')
    }

    const commands: CachedCommand[] = plugin.commands.map(
      (c: ParsedCommand): CachedCommand => ({
        id: c.id || this.makeID(c),
        topic: c.topic,
        command: c.command,
        description: c.description,
        args: c.args,
        variableArgs: c.variableArgs,
        help: c.help,
        usage: c.usage,
        hidden: !!c.hidden,
        aliases: getAliases(c),
        flags: c.flags,
        group: c.group,
      }),
    )

    const topics: CachedTopic[] = (plugin.topics || []).map(
      (t: ParsedTopic): CachedTopic => ({
        id: t.id || '',
        topic: t.topic || '',
        description: t.description,
        hidden: !!t.hidden,
        group: t.group,
      }),
    )

    for (const command of commands) {
      if (topics.find(t => t.id === command.topic)) {
        continue
      }
      const topic: CachedTopic = {
        id: command.topic,
        topic: command.topic,
        group: command.group,
        hidden: true,
      }
      topics.push(topic)
    }

    const groups = plugin.groups || []

    const { name, version } = this.pjson()
    return { name, path: this.path, version, commands, topics, groups }
  }

  // TODO rm any hack
  undefaultTopic(t: ParsedTopic | { default: ParsedTopic } | any): ParsedTopic {
    if (t.default) {
      t = t.default as any
    }
    // normalize v5 exported topic
    if (!t.topic) {
      t.topic = t.name || ''
    }
    if (!t.id) {
      t.id = t.topic
    }
    return t
  }

  undefaultCommand(
    c: ParsedCommand | { default: ParsedCommand },
  ): ParsedCommand {
    if (c.default && typeof c.default !== 'boolean') {
      return c.default as any
    }
    return c as any
  }

  async require(): Promise<ParsedPlugin> {
    let required
    try {
      debug('requiring', this.path)
      required = require(this.path)
      debug('required')
    } catch (err) {
      if (await this.repair(err)) {
        return this.require()
      } else {
        throw err
      }
    }

    const exportedTopic: ParsedTopic =
      required.topic && this.undefaultTopic(required.topic)
    const exportedTopics: ParsedTopic[] =
      required.topics && required.topics.map(t => this.undefaultTopic(t))
    const topics: ParsedTopic[] = this.parsePjsonTopics()
      .concat(exportedTopics || [])
      .concat(exportedTopic || [])
    const commands: ParsedCommand[] =
      required.commands && required.commands.map(t => this.undefaultCommand(t))
    const groups: Group[] = required.groups || []

    return { topics, commands, groups }
  }

  parsePjsonTopics() {
    // flow$ignore
    const topics = (this.pjson()['cli-engine'] || {}).topics
    return this.transformPjsonTopics(topics)
  }

  transformPjsonTopics(topics: any, prefix?: string) {
    const flatten = require('lodash.flatten')
    return flatten(this._transformPjsonTopics(topics))
  }

  _transformPjsonTopics(topics: any, prefix?: string) {
    if (!topics) {
      return []
    }
    return Object.keys(topics || {}).map(k => {
      const t = topics[k]
      const id = prefix ? `${prefix}:${k}` : k
      const topic = {
        ...t,
        id,
        topic: id,
      }
      if (t.subtopics) {
        return [topic].concat(this._transformPjsonTopics(t.subtopics, topic.id))
      }
      return topic
    })
  }

  makeID(o: any): string {
    return [o.topic || o.name, o.command].filter(s => s).join(':')
  }

  pjson(): { name: string; version: string } {
    if (this.type === 'builtin') {
      return { name: 'builtin', version: this.config.version }
    }

    return require(path.join(this.path, 'package.json'))
  }

  async repair(err: Error): Promise<boolean> {
    debug(err)
    return false
  }
}
