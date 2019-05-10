import { Output } from '../Output/index'
import { Config } from '../Config'
import { ParsedCommand, ParsedTopic, PluginPath } from './PluginPath'
import Cache, { Group } from './Cache'

export type PluginType = 'builtin' | 'core' | 'user' | 'link'

export interface ParsedPlugin {
  topics?: ParsedTopic[]
  commands?: ParsedCommand[]
  groups?: Group[]
}

export class Manager {
  out: Output
  config: Config
  cache: Cache

  constructor({
    out,
    config,
    cache,
  }: {
    out: Output
    config: Config
    cache: Cache
  }) {
    this.out = out
    this.config = config
    this.cache = cache
  }

  async list(): Promise<PluginPath[]> {
    throw new Error('abstract method Manager.list')
  }

  async handleNodeVersionChange() {
    // user and linked will override
  }
}
