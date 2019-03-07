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

  // async install(name: string, tag: string = 'latest') {
  //   const downgrade = await this.lock.upgrade()
  //
  //   await this.load()
  //   if (this.plugins.find(p => p.name === name && p.tag === tag)) {
  //     throw new Error(`Plugin ${name} is already installed`)
  //   }
  //
  //   const path = await this.user.install(name, tag)
  //   this.clearCache(path)
  //   await downgrade()
  // }
  //
  // async update() {
  //   if (this.user.list().length === 0) return
  //   this.out.action.start(`${this.config.name}: Updating plugins`)
  //   let downgrade = await this.lock.upgrade()
  //   await this.user.update()
  //   this.clearCache(...(await this.user.list()).map(p => p.path))
  //   await downgrade()
  // }
  //
  // async uninstall(name: string) {
  //   await this.load()
  //   let plugin = this.plugins.filter(p => ['user', 'link'].includes(p.type)).find(p => p.name === name)
  //   if (!plugin) throw new Error(`${name} is not installed`)
  //   let downgrade = await this.lock.upgrade()
  //   switch (plugin.type) {
  //     case 'user': {
  //       if (!this.config.debug) this.out.action.start(`Uninstalling plugin ${name}`)
  //       await this.user.remove(name)
  //       break
  //     }
  //     case 'link': {
  //       if (!this.config.debug) this.out.action.start(`Unlinking plugin ${name}`)
  //       this.linked.remove(plugin.path)
  //       break
  //     }
  //   }
  //   this.clearCache(plugin.path)
  //   await downgrade()
  //   this.out.action.stop()
  // }

  // addPackageToPJSON(name: string, version: string = '*') {
  //   this.user.addPackageToPJSON(name, version)
  // }
  //
  // async addLinkedPlugin(p: string) {
  //   let downgrade = await this.lock.upgrade()
  //
  //   await this.load()
  //   await this.linked.add(p)
  //   this.clearCache(p)
  //   await downgrade()
  // }

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
