import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import * as path from 'path'

export interface CacheData {
  serviceClusterCache: CacheEntry[]
}

export interface CacheEntry {
  service: string
  stage: string
  cluster: string
}

export class ClusterCache {
  cachePath: string
  cache: CacheEntry[]
  constructor(cachePath: string) {
    this.cachePath = cachePath
    this.cache = []
    this.load()
  }

  load() {
    if (fs.pathExistsSync(this.cachePath)) {
      const file = fs.readFileSync(this.cachePath)
      const content = yaml.safeLoad(file)
    }
  }
  addCacheEntry(entry: CacheEntry) {
    const exists = this.cache.find(function(e) {
      return (
        e.service === entry.service &&
        e.stage === entry.stage &&
        e.cluster === entry.cluster
      )
    })
    if (!exists) {
      this.cache.push(entry)
    }
  }
  save() {
    const json = {
      serviceClusterCache: this.cache,
    }
    const file = yaml.safeDump(JSON.parse(JSON.stringify(json)))
    fs.mkdirpSync(path.dirname(this.cachePath))
    fs.writeFileSync(this.cachePath, file)
  }
  getStagesByService(service: string) {
    return this.cache.filter(function(e) {
      return e.service === service
    })
  }
}
