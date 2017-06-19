import {
  Resolver,
  GraphcoolConfig,
  GraphcoolConfigOptionName
} from '../types'

import { graphcoolConfigFilePath } from './constants'

export class Config {
  configs: GraphcoolConfig
  resolver: Resolver

  constructor(resolver: Resolver) {
    this.resolver = resolver
    this.configs = {}
  }

  set(updates: GraphcoolConfig) {
    this.configs = Object.assign(this.configs, updates)
  }

  unset(name: GraphcoolConfigOptionName) {
    delete this.configs[name]
  }

  get(name: GraphcoolConfigOptionName): any {
    return this.configs[name]
  }

  load() {
    if (!this.resolver.exists(graphcoolConfigFilePath)) {
      return
    }

    const configContent = this.resolver.read(graphcoolConfigFilePath)
    this.configs = JSON.parse(configContent)
  }

  save() {
    this.resolver.write(graphcoolConfigFilePath, JSON.stringify(this.configs, null, 2))
  }
}
