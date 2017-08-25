import * as fs from 'fs'
import * as yaml from 'js-yaml'
import { EnvironmentConfig, Environments, ProjectEnvironment } from '../types'
import * as path from 'path'
import client from './Client'

const envPath = path.join(process.cwd(), '.graphcool.env')

class Environment {
  env: EnvironmentConfig

  private initEmptyEnvironment() {
    this.env = {
      default: null,
      environments: {}
    }
  }

  get default(): ProjectEnvironment | null {
    if (this.env.default) {
      return this.env.environments[this.env.default]
    }

    return null
  }

  public load() {
    if (fs.existsSync(envPath)) {
      this.env = yaml.safeLoad(fs.readFileSync(envPath, 'utf-8'))
    } else {
      this.initEmptyEnvironment()
    }
  }

  public save() {
    const file = yaml.safeDump(this.env)
    fs.writeFileSync(envPath, file)
  }

  public async set(name: string, projectId: string) {
    const version = await client.getProjectVersion(projectId)

    this.env.environments[name] = {
      projectId,
      version,
    }
  }

  public rename(oldName: string, newName: string) {

  }

  public remove(oldName: string, newName: string) {

  }

}

const env = new Environment()
env.load()

export default env
