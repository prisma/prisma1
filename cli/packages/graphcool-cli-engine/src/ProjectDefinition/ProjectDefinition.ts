import { fsToProject } from './fsToProject'
import { projectToFs } from './projectToFs'
import * as path from 'path'
import { readDefinition } from './yaml'
import chalk from 'chalk'
import { Args, GraphcoolModule, ProjectDefinition } from '../types/common'
import fs from '../fs'
import { Output } from '../Output/index'
import { Config } from '../Config'
import { GraphcoolDefinition, FunctionDefinition } from 'graphcool-json-schema'
import { flatMap } from 'lodash'
import * as yamlParser from 'yaml-ast-parser'
import * as yaml from 'js-yaml'
import { builtinModules } from './builtin-modules'
const debug = require('debug')('project-definition')

export interface FunctionTuple {
  name: string
  fn: FunctionDefinition
}

export class ProjectDefinitionClass {
  static sanitizeDefinition(definition: ProjectDefinition) {
    const modules = definition.modules.map(module => {
      const { name, files, externalFiles } = module
      let content = module.content
      if (module.definition && typeof module.definition === 'object') {
        // parse + stringify trims away `undefined` values, which are not accepted by the yaml parser
        content = yaml.safeDump(JSON.parse(JSON.stringify(module.definition)))
      }
      return { name, content, files, externalFiles }
    })

    return { modules }
  }

  definition: ProjectDefinition | null
  out: Output
  config: Config
  args: Args = {}

  constructor(out: Output, config: Config) {
    this.out = out
    this.config = config
  }

  public async load(args: Args) {
    this.args = args
    if (this.config.definitionPath && fs.pathExistsSync(this.config.definitionPath)) {
      this.definition = await fsToProject(this.config.definitionDir, this.out, args)
      if (process.env.GRAPHCOOL_DUMP_LOADED_DEFINITION) {
        const definitionJsonPath = path.join(this.config.definitionDir, 'loaded-definition.json')
        fs.writeFileSync(definitionJsonPath, JSON.stringify(this.definition, null, 2))
      }
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

    if (process.env.GRAPHCOOL_DUMP_SAVED_DEFINITION) {
      const definitionJsonPath = path.join(this.config.definitionDir, 'definition.json')
      fs.writeFileSync(definitionJsonPath, JSON.stringify(this.definition, null, 2))
    }
  }

  public async saveTypes() {
    const definition = await readDefinition(
      this.definition!.modules[0]!.content,
      this.out,
      this.config.definitionPath!,
      this.args,
    )
    const types = this.definition!.modules[0].files[definition.types]
    this.out.log(chalk.blue(`Written ${definition.types}`))
    fs.writeFileSync(
      path.join(this.config.definitionDir, definition.types),
      types,
    )
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
          fn: module.definition!.functions![name],
        }
      }
    }
    return null
  }

  public insertModule(moduleName: string, relativePath: string) {
    const file = this.definition!.modules[0].content
    const insertion = `\n  ${moduleName}: ${relativePath}`
    return this.insertToDefinition(file, 'modules', insertion)
  }

  comment(str: string) {
    return str.split('\n').map(l => `# ${l}`).join('\n')
  }

  addTemplateNotes(str: string, templateName: string) {
    return `\n# added by ${templateName} template: (please uncomment)\n` + str + '\n\n'
  }

  public mergeDefinition(newDefinitionYaml: string, templateName: string, useComments: boolean = true): string {
    let newDefinition = this.definition!.modules[0].content
    const newYaml = yamlParser.safeLoad(newDefinitionYaml)

    const whiteList = ['functions', 'permissions']

    newYaml.mappings.filter(m => whiteList.includes(m.key.value)).forEach(mapping => {
      const key = mapping.key.value
      let beginning = this.getBeginningPosition(newDefinition, key)
      let values = this.extractValues(newDefinitionYaml, newYaml, key, beginning > -1)
      values = useComments ? this.comment(values) : values
      values = this.addTemplateNotes(values, templateName)
      beginning = beginning === -1 ? newDefinition.length - 1 : beginning
      newDefinition = newDefinition.slice(0, beginning + 1) + values + newDefinition.slice(beginning + 1)
    })

    return newDefinition
  }

  public mergeTypes(newTypes: string, templateName: string) {
    const typesPath = this.definition!.modules[0].definition!.types
    const oldTypes = this.definition!.modules[0].files[typesPath]

    return oldTypes + this.addTemplateNotes(this.comment(newTypes), templateName)
  }

  public async checkNodeModules(throwError: boolean) {
    if (!this.config.definitionPath) {
      this.out.error('Could not find graphcool.yml')
    }
    const definitionFile = fs.readFileSync(this.config.definitionPath!, 'utf-8')
    const json = yaml.safeLoad(definitionFile) as GraphcoolDefinition
    const functions = this.extractFunctions(json.functions)
    const functionsWithRequire = functions.reduce((acc, fn) => {
      const src = typeof fn.fn.handler.code === 'string' ? fn.fn.handler.code : fn.fn.handler.code!.src
      const file = fs.readFileSync(src, 'utf-8')
      const requireRegex = /(?:(?:var|const)\s*([\s\S]*?)\s*=\s*)?require\(['"]([^'"]+)['"](?:, ['"]([^'"]+)['"])?\);?/
      const importRegex = /\bimport\s+(?:[\s\S]+\s+from\s+)?[\'"]([^"\']+)["\']/
      const statements = file.split('\n').reduce((racc, line, index) => {
        const requireMatch = requireRegex.exec(line)
        const importMatch = importRegex.exec(line)
        if (requireMatch || importMatch) {
          const srcPath = requireMatch ? requireMatch[1] : importMatch![1]

          if (srcPath) {
            if (!builtinModules.includes(srcPath) && !srcPath.startsWith('./')) {
              const result = {
                line, index: index + 1, srcPath,
              }

              return racc.concat(result)
            }
          }
        }

        return racc
      }, [] as any)
      if (statements.length > 0) {
        return acc.concat({
          statements,
          fn,
          src,
        })
      }

      return acc
    }, [] as any)

    if (functionsWithRequire.length > 0) {
      if (
        !fs.pathExistsSync(path.join(this.config.definitionDir, 'node_modules'))
      ) {
        const imports = functionsWithRequire.map(fn => {
          return `${chalk.bold(fn.fn.name)} (${fn.src}):\n` + fn.statements.map(s => `  ${s.index}: ${chalk.dim(s.line)}`).join('\n')
        }).join('\n')
        const text = `You have import/require statements in your functions without a node_modules folder:
${imports}

Please make sure you specified all needed dependencies in your package.json and run npm install.`
        if (throwError) {
          this.out.error(text)
        } else {
          this.out.warn(text)
        }
      }
    }
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

    const mapping = obj.mappings.find(m => m.key.value === key)
    const end = mapping.endPosition


    const newFile = file.slice(0, end) + insertion + file.slice(end)
    const valueStart = mapping.value.startPosition
    const valueEnd = mapping.value.endPosition
    if (mapping.value && valueEnd - valueStart < 4) {
      return newFile.slice(0, valueStart) + newFile.slice(valueEnd)
    }

    return file
  }

  private extractValues(file: string, obj: any, key: string, valuesOnly: boolean) {
    const mapping = obj.mappings.find(m => m.key.value === key)

    if (!mapping) {
      this.out.error(`Could not find mapping for key ${key}`)
    }

    const start = valuesOnly ? mapping.key.endPosition + 1 : mapping.startPosition
    return file.slice(start, mapping.endPosition)
  }

  private getBeginningPosition(file: string, key: string): number {
    const obj = yamlParser.safeLoad(file)
    const mapping = obj.mappings.find(m => m.key.value === key)
    return mapping ? mapping.key.endPosition + 1 : -1
  }


  get functions(): FunctionTuple[] {
    if (!this.definition) {
      return []
    }
    return this.extractFunctions(this.definition!.modules[0].definition!.functions)
  }

  private extractFunctions(functions?: { [p: string]: FunctionDefinition }) {
    if (!functions) {
      return []
    } else {
      return Object.keys(functions)
        .filter(name => functions[name].handler.code)
        .map(name => {
          return {
            name,
            fn: functions[name],
          }
        })
    }
  }

}

