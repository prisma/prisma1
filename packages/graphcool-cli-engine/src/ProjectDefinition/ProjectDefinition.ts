import {fsToProject} from './fsToProject'
import {projectToFs} from './projectToFs'
import * as path from 'path'
import { readDefinition } from './yaml'
import * as chalk from 'chalk'
import { ProjectDefinition } from '../types'
import fs from '../fs'
import { Output } from '../Output/index'
import { Config } from '../Config'
import { GraphcoolDefinition } from 'graphcool-json-schema'
const debug = require('debug')('project-definition')

export class ProjectDefinitionClass {
  definition: ProjectDefinition | null
  out: Output
  config: Config

  constructor(out: Output, config: Config) {
    this.out = out
    this.config = config
  }

  public async load() {
    if (fs.existsSync(path.join(this.config.definitionDir, 'graphcool.yml'))) {
      this.definition = await fsToProject(this.config.definitionDir, this.out)
      // if (process.env.DEBUG && process.env.DEBUG!.includes('*')) {
      //   const definitionJsonPath = path.join(this.config.definitionDir, 'definition.json')
      //   fs.writeFileSync(definitionJsonPath, JSON.stringify(this.definition, null, 2))
      // }
    }
  }

  public async save(files?: string[], silent?: boolean) {
    projectToFs(this.definition!, this.config.definitionDir, this.out, files, silent)

    // if (process.env.DEBUG && process.env.DEBUG!.includes('*')) {
    //   const definitionJsonPath = path.join(this.config.definitionDir, 'definition.json')
    //   fs.writeFileSync(definitionJsonPath, JSON.stringify(this.definition, null, 2))
    // }
  }

  public async saveTypes() {
    const definition = await readDefinition(this.definition!.modules[0]!.content, this.out)
    const types = this.definition!.modules[0].files[definition.types]
    this.out.log(chalk.blue(`Written ${definition.types}`))
    fs.writeFileSync(path.join(this.config.definitionDir, definition.types), types)
  }

  public async injectEnvironment() {
    if (this.definition) {
      this.definition.modules = await Promise.all(this.definition.modules.map(async module => {
        const ymlDefinitinon: GraphcoolDefinition = await readDefinition(module.content, this.out)
        Object.keys(ymlDefinitinon.functions).forEach(fnName => {
          const fn = ymlDefinitinon.functions[fnName]
          if (fn.handler.code && fn.handler.code.environment) {
            const file = module.files[fn.handler.code.src]
            module.files[fn.handler.code.src] = this.injectEnvironmentToFile(file, fn.handler.code.environment)
            debug(`Injected env vars to file:`)
            debug(`BEFORE`)
            debug(file)
            debug('AFTER')
            debug(module.files[fn.handler.code.src])
          }

          ymlDefinitinon.functions[fnName] = fn
        })

        return module
      }))

    }
  }

  public set(definition: ProjectDefinition | null) {
    this.definition = definition
  }

  private injectEnvironmentToFile(file: string, environment: {[envVar: string]: string}): string {
    // get first function line
    const lines = file.split('\n')
    Object.keys(environment).forEach(key => {
      const envVar = environment[key]
      lines.splice(0, 0 , `process.env['${key}'] = '${envVar}';`)
    })
    return lines.join('\n')
  }
}
