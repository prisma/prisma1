import 'source-map-support/register'
import * as path from 'path'
import * as fs from 'fs-extra'
import Lock from './Lock'
import { Arg, Flag } from '../Flags/index'
import { Config } from '../Config'
import { Output } from '../Output/index'
import { Manager } from './Manager'
import Plugin from './Plugin'
import { PluginPath } from './PluginPath'

export interface CachedCommand {
  id: string
  topic: string
  command?: string
  aliases?: string[]
  args: Arg[]
  flags: { [name: string]: Flag<any> }
  description?: string
  help?: string
  usage?: string
  hidden: boolean
  variableArgs?: boolean
  group: string
}

export interface CachedTopic {
  id: string
  topic: string
  description?: string
  hidden: boolean
  group: string
}

export interface CachedPlugin {
  name: string
  path: string
  version: string
  commands: CachedCommand[]
  topics: CachedTopic[]
  groups: Group[]
}

export interface Group {
  key: string
  name: string
  deprecated?: boolean
}

export interface CacheData {
  version: string
  node_version: string | null
  plugins: { [path: string]: CachedPlugin }
}

export default class Cache {
  static updated = false
  config: Config
  out: Output
  /* tslint:disable-next-line */
  private _cache: CacheData

  constructor(output: Output) {
    this.out = output
    this.config = output.config
  }

  initialize() {
    this._cache = {
      version: this.config.version,
      plugins: {},
      node_version: null,
    }
  }

  clear() {
    this._cache = {
      version: this.config.version,
      plugins: {},
      node_version: this._cache.node_version,
    }
    try {
      fs.removeSync(this.config.requireCachePath)
    } catch (e) {
      //
    }
  }

  get file(): string {
    return path.join(this.config.cacheDir, 'plugins.json')
  }

  get cache(): CacheData {
    if (this._cache) {
      return this._cache
    }

    try {
      this._cache = fs.readJSONSync(this.file)
    } catch (err) {
      if (err.code !== 'ENOENT') {
        this.out.debug(err)
      }
      this.initialize()
    }
    if (
      this._cache.version !== this.config.version ||
      process.env.GRAPHCOOL_CLI_CLEAR_CACHE
    ) {
      this.clear()
    }
    return this._cache
  }

  plugin(pluginPath: string): CachedPlugin | undefined {
    return this.cache.plugins[pluginPath]
  }

  updatePlugin(pluginPath: string, plugin: CachedPlugin) {
    ;(this.constructor as any).updated = true
    this.cache.plugins[pluginPath] = plugin
  }

  deletePlugin(...paths: string[]) {
    for (const pluginPath of paths) {
      delete this.cache.plugins[pluginPath]
    }
    this.save()
  }

  async fetch(pluginPath: PluginPath): Promise<CachedPlugin> {
    const c = this.plugin(pluginPath.path)
    if (c) {
      return c
    }
    try {
      const cachedPlugin = await pluginPath.convertToCached()
      this.updatePlugin(pluginPath.path, cachedPlugin)
      return cachedPlugin
    } catch (err) {
      if (pluginPath.type === 'builtin') {
        throw err
      }
      if (await pluginPath.repair(err)) {
        return this.fetch(pluginPath)
      }
      this.out.warn(`Error parsing plugin ${pluginPath.path}`)
      this.out.warn(err)
      return {
        name: pluginPath.path,
        path: pluginPath.path,
        version: '',
        commands: [],
        topics: [],
        groups: [],
      }
    }
  }

  async fetchManagers(...managers: Manager[]): Promise<Plugin[]> {
    const plugins: Plugin[] = []
    if (this.cache.node_version !== process.version) {
      const lock = new Lock(this.out)

      const downgrade = await lock.upgrade()
      for (const manager of managers) {
        await manager.handleNodeVersionChange()
      }
      await downgrade()

      this.cache.node_version = process.version as string
      ;(this.constructor as any).updated = true
    }

    for (const manager of managers) {
      const paths = await manager.list()
      for (const pluginPath of paths) {
        const plugin = await this.fetch(pluginPath)
        if (plugins.find(p => p.name === plugin.name)) {
          continue
        }
        plugins.push(new Plugin(this.out, pluginPath, plugin))
      }
    }

    this.save()

    return plugins
  }

  save() {
    if (!(this.constructor as any).updated) {
      return
    }
    try {
      fs.writeJSONSync(this.file, this.cache)
    } catch (err) {
      this.out.warn(err)
    }
  }
}
