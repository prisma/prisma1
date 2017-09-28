import { fsToProject } from './fsToProject'
import { projectToFs } from './projectToFs'
import * as path from 'path'
import { readDefinition } from './yaml'
import * as chalk from 'chalk'
import { GraphcoolModule, ProjectDefinition } from '../types'
import fs from '../fs'
import { Output } from '../Output/index'
import { Config } from '../Config'
import { GraphcoolDefinition, FunctionDefinition } from 'graphcool-json-schema'
const debug = require('debug')('project-definition')
import { flatMap } from 'lodash'
import * as yamlParser from 'yaml-ast-parser'
import * as yaml from 'js-yaml'

export class ProjectDefinitionClass {
  static sanitizeDefinition(definition: ProjectDefinition) {
    const modules = definition.modules.map(module => {
      const { name, files } = module
      let content = module.content
      if (module.definition && typeof module.definition === 'object') {
        content = yaml.safeDump(module.definition)
      }
      return { name, content, files }
    })

    return { modules }
  }

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
    projectToFs(
      this.definition!,
      this.config.definitionDir,
      this.out,
      files,
      silent,
    )

    // if (process.env.DEBUG && process.env.DEBUG!.includes('*')) {
    //   const definitionJsonPath = path.join(this.config.definitionDir, 'definition.json')
    //   fs.writeFileSync(definitionJsonPath, JSON.stringify(this.definition, null, 2))
    // }
  }

  public async saveTypes() {
    const definition = await readDefinition(
      this.definition!.modules[0]!.content,
      this.out,
      'root',
    )
    const types = this.definition!.modules[0].files[definition.types]
    this.out.log(chalk.blue(`Written ${definition.types}`))
    fs.writeFileSync(
      path.join(this.config.definitionDir, definition.types),
      types,
    )
  }

  public async injectEnvironment() {
    if (this.definition) {
      this.definition.modules = await Promise.all(
        this.definition.modules.map(async module => {
          const moduleName =
            module.name && module.name.length > 0 ? module.name : 'root'
          const ymlDefinitinon: GraphcoolDefinition = await readDefinition(
            module.content,
            this.out,
            moduleName,
          )
          if (ymlDefinitinon.functions && ymlDefinitinon.functions) {
            Object.keys(ymlDefinitinon.functions).forEach(fnName => {
              const fn = ymlDefinitinon.functions[fnName]
              if (fn.handler.code) {
                let newFile = module.files[fn.handler.code.src]
                if (fn.handler.code.environment) {
                  const file = module.files[fn.handler.code.src]
                  newFile = this.injectEnvironmentToFile(
                    file,
                    fn.handler.code.environment,
                  )
                }

                newFile = `'use latest'\n` + newFile

                module.files[fn.handler.code.src] = newFile
              }

              ymlDefinitinon.functions[fnName] = fn
            })
          }

          return module
        }),
      )
    }
  }

  public insertModule(moduleName: string, relativePath: string) {
    const file = this.definition!.modules[0].content
    const insertion = `\n  ${moduleName}: ${relativePath}`
    return this.insertToDefinition(file, 'modules', insertion)
  }

  public set(definition: ProjectDefinition | null) {
    this.definition = definition
  }

  public getFunctionAndModule(
    name: string,
  ): { fn: FunctionDefinition; module: GraphcoolModule } | null {
    if (this.definition && this.definition.modules) {
      const functions: FunctionDefinition[] = flatMap(
        this.definition.modules,
        (m: GraphcoolModule) => {
          return m.definition && m.definition.functions
            ? m.definition.functions
            : []
        },
      ) as any
      const module = this.definition.modules.find(
        m =>
          (m.definition &&
            m.definition.functions &&
            Object.keys(m.definition.functions).includes(name)) ||
          false,
      )
      if (module) {
        return {
          module,
          fn: module.definition!.functions[name],
        }
      }
    }
    return null
  }

  private injectEnvironmentToFile(
    file: string,
    environment: { [envVar: string]: string },
  ): string {
    // get first function line
    const lines = file.split('\n')
    Object.keys(environment).forEach(key => {
      const envVar = environment[key]
      lines.splice(0, 0, `process.env['${key}'] = '${envVar}';`)
    })
    return lines.join('\n')
  }

  private insertToDefinition(file: string, key: string, insertion: string) {
    const obj = yamlParser.safeLoad(file)

    const modulesMapping = obj.mappings.find(m => m.key.value === key)
    const end = modulesMapping.endPosition

    const newFile = file.slice(0, end) + insertion + file.slice(end)
    const valueStart = modulesMapping.value.startPosition
    const valueEnd = modulesMapping.value.endPosition
    if (modulesMapping.value && valueEnd - valueStart < 4) {
      return newFile.slice(0, valueStart) + newFile.slice(valueEnd)
    }

    return file
  }
}
