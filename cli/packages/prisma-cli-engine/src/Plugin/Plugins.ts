import Plugin from './Plugin'
import * as uniqby from 'lodash.uniqby'
import Lock from './Lock'
import { Output } from '../Output/index'
import { Config } from '../Config'
import BuiltinPlugins from './BuiltInPlugins'
import CorePlugins from './CorePlugins'
import Cache, { CachedCommand, CachedTopic, Group } from './Cache'
import { Command } from '../Command'
import { Topic } from '../Topic'

export default class Plugins {
  builtin: BuiltinPlugins
  // linked: LinkedPlugins
  // user: UserPlugins
  core: CorePlugins
  plugins: Plugin[]
  cache: Cache
  out: Output
  lock: Lock
  loaded: boolean
  config: Config

  constructor(output: Output) {
    this.out = output
    this.config = output.config
    this.cache = new Cache(output)

    this.builtin = new BuiltinPlugins(this)
    // this.linked = new LinkedPlugins(this)
    // this.user = new UserPlugins(this)
    this.core = new CorePlugins(this)
    this.lock = new Lock(this.out)
  }

  async load() {
    if (this.loaded) {
      return
    }
    this.plugins = await this.cache.fetchManagers(
      // this.linked,
      // this.user,
      this.core,
      this.builtin,
    )
    this.loaded = true
  }

  get commands(): CachedCommand[] {
    let commands: CachedCommand[] = []
    for (const plugin of this.plugins) {
      try {
        commands = commands.concat(plugin.commands)
      } catch (err) {
        this.out.warn(err, `error reading plugin ${plugin.name}`)
      }
    }
    return commands
  }

  async list() {
    await this.load()
    return this.plugins
  }

  isPluginInstalled(name: string): boolean {
    return !!this.plugins.find(p => p.name === name)
  }

  async findPluginWithCommand(id: string): Promise<Plugin | undefined> {
    for (const plugin of await this.list()) {
      if (await plugin.findCommand(id)) {
        return plugin
      }
    }
  }

  async findCommand(id: string): Promise<typeof Command | undefined> {
    for (const plugin of this.plugins) {
      const c = await plugin.findCommand(id)
      if (c) {
        return c
      }
      return
    }
  }

  async commandsForTopic(topic: string): Promise<Array<typeof Command>> {
    let commands = this.plugins.reduce(
      (t, p) => {
        try {
          return t.concat(
            p.commands
              .filter(c => c.topic === topic)
              .map(c => p.findCommand(c.id)),
          )
        } catch (err) {
          this.out.warn(err, `error reading plugin ${p.name}`)
          return t
        }
      },
      [] as any,
    )
    commands = await Promise.all(commands)
    return uniqby(commands, 'id')
  }

  async subtopicsForTopic(id: string): Promise<CachedTopic[] | undefined> {
    if (!id) {
      return
    }
    for (const plugin of this.plugins) {
      const foundTopic = await plugin.findTopic(id)
      if (foundTopic) {
        return plugin.topics.filter(t => {
          if (!t.id) {
            return false
          }
          if (t.id === id) {
            return false
          }
          const re = new RegExp(`^${id}`)
          return !!t.id.match(re)
        })
      }
    }
  }

  async findTopic(id: string): Promise<typeof Topic | undefined> {
    if (!id) {
      return
    }
    for (const plugin of this.plugins) {
      const t = await plugin.findTopic(id)
      if (t) {
        return t
      }
    }
  }

  clearCache(...paths: string[]) {
    this.cache.deletePlugin(...paths)
  }

  get topics(): CachedTopic[] {
    return uniqby(
      this.plugins.reduce((t, p) => t.concat(p.topics), [] as any),
      'id',
    )
  }

  get groups(): Group[] {
    return this.plugins
      .filter(p => p.groups)
      .reduce((t, p) => t.concat(p.groups), [] as any)
  }
}
