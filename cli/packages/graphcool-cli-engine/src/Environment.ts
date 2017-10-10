import * as yaml from 'js-yaml'
import * as path from 'path'
import fs from './fs'
import { Output } from './Output/index'
import { DockerEnvironment, EnvironmentConfig } from './types'
import { Client } from './Client/Client'
import { Config } from './Config'
import { EnvDoesntExistError } from './errors/EnvDoesntExistError'

export class Environment {
  env: EnvironmentConfig
  out: Output
  client: Client
  config: Config

  constructor(out: Output, config: Config, client: Client) {
    this.out = out
    this.config = config
    this.client = client
  }

  public initEmptyEnvironment() {
    this.env = {
      default: null,
      environments: {},
    }
  }

  get default(): string | DockerEnvironment | null {
    if (this.env.default) {
      return this.env.environments[this.env.default]
    }

    return null
  }

  public load() {
    if (this.config.envPath && fs.existsSync(this.config.envPath)) {
      try {
        this.env = yaml.safeLoad(fs.readFileSync(this.config.envPath, 'utf-8'))
      } catch (e) {
        this.out.error(`Error in .graphcoolrc (${this.config.envPath}): ${e.message}`)
        process.exit(1)
      }

      if (!this.env.environments) {
        this.out.error(`Loaded invalid .graphcoolrc from ${this.config.envPath}: Doesn't contain the 'environments' field`)
      }

      if (!this.env.default) {
        this.out.error(`Loaded invalid .graphcoolrc from ${this.config.envPath}: Doesn't contain the 'default' field`)
      }

      if (this.default && this.isDockerEnv(this.default)) {
        const dockerEnv = (this.default as DockerEnvironment)
        this.config.setLocal(dockerEnv.host)
        this.config.setToken(dockerEnv.token)
        this.client.updateClient()
      }

    } else {
      this.initEmptyEnvironment()
    }
  }

  public save() {
    if (this.config.envPath) {
      const file = yaml.safeDump(this.env)
      fs.writeFileSync(this.config.envPath, file)
    }
  }

  public setEnv(name: string, projectId: string) {
    this.env.environments[name] = projectId
  }

  public setDockerEnv(name: string, dockerEnv: DockerEnvironment) {
    this.env.environments[name] = dockerEnv
  }

  public set(name: string, projectId: string) {
    if (this.isDockerEnv(this.env.environments[name])) {
      (this.env.environments[name] as DockerEnvironment).projectId = projectId
    } else {
      this.env.environments[name] = projectId
    }
  }

  public setDefault(name: string) {
    if (!this.env.environments[name]) {
      this.out.error(new EnvDoesntExistError(name))
    }
    this.env.default = name
  }

  public rename(oldName: string, newName: string) {
    const oldEnv = this.env.environments[oldName]

    if (!oldEnv) {
      this.out.error(new EnvDoesntExistError(oldName))
    }
    delete this.env.environments[oldName]

    this.env.environments[newName] = oldEnv
    if (this.env.default === oldName) {
      this.setDefault(newName)
    }
  }

  public remove(envName: string) {
    const oldEnv = this.env.environments[envName]

    if (!oldEnv) {
      this.out.error(new EnvDoesntExistError(envName))
    }

    delete this.env.environments[envName]
  }

  public deleteIfExist(projectIds: string[]) {
    projectIds.forEach(projectId => {
      const envName = Object.keys(this.env.environments).find(
        name => this.env.environments[name] === projectId,
      )
      if (envName) {
        delete this.env.environments[envName]
      }
      if (this.env.default === envName) {
        this.env.default = null
      }
    })
  }

  public updateDocker(env: DockerEnvironment) {
    this.config.setToken(env.token)
    this.config.setLocal(env.host)
    this.client.updateClient()
  }

  public async getEnvironment({
    project,
    env,
    skipDefault,
  }: {
    project?: string
    env?: string
    skipDefault?: boolean
  }): Promise<{ projectId: string | null; envName: string | null }> {
    let projectId: null | string = null

    if (env) {
      if (typeof this.env.environments[env] === 'string') {
        projectId = this.env.environments[env] as string
      }
      if (this.env.environments[env] && typeof this.env.environments[env] === 'object') {
        projectId = (this.env.environments[env] as DockerEnvironment).projectId || null
        this.updateDocker(this.env.environments[env] as DockerEnvironment)
      }
      return {
        envName: env,
        projectId,
      }
    }

    if (project) {
      const projects = await this.client.fetchProjects()
      const foundProject = projects.find(
        p => p.id === project || p.alias === project,
      )
      projectId = foundProject ? foundProject.id : null
      if (projectId) {
        const resultEnv = this.getEnvironmentName(projectId)

        return {
          projectId,
          envName: resultEnv,
        }
      }
    }

    if (this.default && !skipDefault) {
      return {
        projectId: typeof this.default === 'string' ? this.default : this.default.projectId,
        envName: this.env.default,
      }
    }

    return {
      projectId: null,
      envName: null,
    }
  }

  public isDockerEnv(suspect: any) {
    if (!suspect) {
      return false
    }

    if (typeof suspect === 'object' && suspect.token && suspect.host) {
      return true
    }

    return false
  }

  private getEnvironmentName(projectId: string): string | null {
    return (
      Object.keys(this.env.environments).find(key => {
        const projectEnv = this.env.environments[key]
        return projectEnv === projectId
      }) || null
    )
  }
}
