import * as yaml from 'js-yaml'
import * as path from 'path'
import fs from './fs'
import { Output } from './Output/index'
import { EnvironmentConfig } from './types'
import { Client } from './Client/Client'
import EnvDoesntExistError from './errors/EnvDoesntExistError'

const envPath = path.join(process.cwd(), '.graphcoolrc')

class Environment {
  env: EnvironmentConfig
  out: Output
  client: Client

  constructor(out: Output, client: Client) {
    this.out = out
  }

  public initEmptyEnvironment() {
    this.env = {
      default: null,
      environments: {}
    }
  }

  get default(): string | null {
    if (this.env.default) {
      return this.env.environments[this.env.default]
    }

    return null
  }

  public load() {
    if (fs.existsSync(envPath)) {
      try {
        this.env = yaml.safeLoad(fs.readFileSync(envPath, 'utf-8'))
      } catch (e) {
        this.out.error(`Error in .graphcoolrc: `)
        this.out.error(e.message)
        process.exit(1)
      }
    } else {
      this.initEmptyEnvironment()
    }
  }

  public save() {
    const file = yaml.safeDump(this.env)
    fs.writeFileSync(envPath, file)
  }

  public setEnv(name: string, projectId: string) {
    this.env.environments[name] = projectId
  }

  public async set(name: string, projectId: string) {
    const version = await this.client.getProjectVersion(projectId)

    this.env.environments[name] = projectId
  }

  public setDefault(name: string) {
    if (!this.env.environments[name]) {
      throw new Error(`Environment ${name} doesn't exist in local .graphcoolrc definition`)
    }
    this.env.default = name
  }

  public rename(oldName: string, newName: string) {
    const oldEnv = this.env.environments[oldName]

    if (!oldEnv) {
      throw new Error(`Environment ${oldName} doesn't exist`)
    }

    delete this.env.environments[oldName]
    if (this.env.default === oldName) {
      this.setDefault(newName)
    }
    this.env.environments[newName] = oldEnv
  }

  public remove(envName: string) {
    const oldEnv = this.env.environments[envName]

    if (!oldEnv) {
      throw new Error(`Environment ${envName} doesn't exist`)
    }

    delete this.env.environments[envName]
  }

  public deleteIfExist(projectIds: string[]) {
    projectIds.forEach(projectId => {
      const envName = Object.keys(this.env.environments).find(envName => this.env.environments[envName] === projectId)
      if (envName) {
        delete this.env.environments[envName]
      }
    })
  }

  public async getProjectId({project, env, skipDefault}: {project?: string, env?: string, skipDefault?: boolean}): Promise<string | null> {
    if (project) {
      const projects = await this.client.fetchProjects()
      const foundProject = projects.find(p => p.id === project || p.alias === project)
      if (!foundProject) {
        throw new Error(`Project with alias or id "${project}" could not be found in this account`)
      }
      return foundProject.id
    }

    if (env) {
      const projectId = this.env.environments[env]

      if (!projectId) {
        throw new EnvDoesntExistError(env)
      }

      return projectId
    }

    if (this.default && !skipDefault) {
      return this.default
    }

    return null
  }

  public getEnvironmentName(projectId: string): {envName: string} | null {
    const envName = Object.keys(this.env.environments).find(key => {
      const projectEnv = this.env.environments[key]
      return projectEnv === projectId
    })

    if (envName) {
      return {
        envName,
      }
    }

    return null
  }

}
