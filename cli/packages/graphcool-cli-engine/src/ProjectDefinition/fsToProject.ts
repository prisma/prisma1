import * as path from 'path'
import chalk from 'chalk'
import { readDefinition } from './yaml'
import fs from '../fs'
import { Args, GraphcoolModule, ProjectDefinition } from '../types/common'
import { Output } from '../Output/index'
import { FunctionDefinition, GraphcoolDefinition } from 'graphcool-json-schema'

interface ErrorMessage {
  message: string
}

export async function fsToProject(
  inputDir: string,
  out: Output,
  args: Args
): Promise<ProjectDefinition> {
  const definitionPath = path.join(inputDir, 'graphcool.yml')
  const rootModule = await fsToModule(definitionPath, out, 'root', args)
  const modules: any[] = [rootModule]

  const definition: GraphcoolDefinition = await readDefinition(
    rootModule.content,
    out,
    definitionPath,
    args
  )

  if (definition.hasOwnProperty('modules')) {
    out.error(`Your graphcool.yml contains modules. Please remove them in order to deploy.
Modules are deprecated and replaced by templates.
Read more about templates here: https://github.com/graphcool/graphcool/issues/720`)
  }

  // if (definition.modules) {
  //   for (const moduleName of Object.keys(definition.modules)) {
  //     const modulePath = definition.modules[moduleName]
  //     const resolvedModulePath = path.join(inputDir, modulePath)
  //     const module = await fsToModule(resolvedModulePath, out, moduleName, args)
  //     modules.push({
  //       ...module,
  //       name: moduleName,
  //     })
  //   }
  // }

  return {
    modules,
  }
}

function normalizePath(filePath: string) {
  return filePath.split(':')[0]
}

export async function fsToModule(
  moduleDefinitionPath: string,
  out: Output,
  moduleName: string = 'root',
  args: Args
): Promise<GraphcoolModule> {
  const inputDir = path.dirname(moduleDefinitionPath)
  const content = fs.readFileSync(moduleDefinitionPath, 'utf-8')

  const module: GraphcoolModule = {
    name: '',
    content,
    files: {},
    baseDir: inputDir,
  }

  let files = {}
  const errors: ErrorMessage[] = []

  const definition: GraphcoolDefinition = await readDefinition(
    content,
    out,
    moduleDefinitionPath,
    args
  )
  const typesPath = path.join(inputDir, definition.types)

  if (fs.existsSync(typesPath)) {
    const types = fs.readFileSync(typesPath, 'utf-8')
    files = {
      ...files,
      [definition.types]: types,
    }
  } else {
    errors.push({
      message: `The types definition file "${typesPath}" could not be found.`,
    })
  }

  if (definition.permissions) {
    definition.permissions.forEach(permission => {
      if (permission.query && isGraphQLFile(permission.query)) {
        const queryPath = path.join(inputDir, normalizePath(permission.query))
        if (fs.existsSync(queryPath)) {
          const permissionQuery = fs.readFileSync(queryPath, 'utf-8')
          files = {
            ...files,
            [normalizePath(permission.query)]: permissionQuery,
          }
        } else {
          errors.push({
            message: `The file ${permission.query} for permission query ${permission.operation} does not exist`,
          })
        }
      }
    })
  }

  if (definition.functions) {
    Object.keys(definition.functions).forEach(funcName => {
      const func: FunctionDefinition = definition.functions![funcName]
      if (func.handler.code) {
        if (typeof func.handler.code === 'string') {
          if (!isFunctionFile(func.handler.code)) {
            errors.push({
              message: `The handler ${func.handler.code} for function ${funcName} is not a valid function path. It must end with .js or .ts and be in the current working directory.`,
            })
          }
          const handlerPath = path.join(inputDir, func.handler.code)
          if (fs.existsSync(handlerPath)) {
            const functionCode = fs.readFileSync(handlerPath, 'utf-8')
            files = {
              ...files,
              [func.handler.code]: functionCode,
            }
          } else {
            errors.push({
              message: `The file ${func.handler.code} for function ${funcName} does not exist`,
            })
          }
        } else {
          if (!isFunctionFile(func.handler.code.src)) {
            errors.push({
              message: `The handler ${func.handler.code
                .src} for function ${funcName} is not a valid function path. It must end with .js or .ts and be in the current working directory.`,
            })
          }
          const handlerPath = path.join(inputDir, func.handler.code.src)
          if (fs.existsSync(handlerPath)) {
            const functionCode = fs.readFileSync(handlerPath, 'utf-8')
            files = {
              ...files,
              [func.handler.code.src]: functionCode,
            }
          } else {
            errors.push({
              message: `The file ${func.handler.code
                .src} for function ${funcName} does not exist`,
            })
          }
        }
      }

      if (func.query && isGraphQLFile(func.query)) {
        const queryPath = path.join(inputDir, func.query)

        if (fs.existsSync(queryPath)) {
          const file = fs.readFileSync(queryPath, 'utf-8')
          files = {
            ...files,
            [func.query]: file,
          }
        } else {
          errors.push({
            message: `The file ${func.query} for the subscription query of function ${funcName} does not exist`,
          })
        }
      }

      if (func.schema && isGraphQLFile(func.schema)) {
        const queryPath = path.join(inputDir, func.schema)

        if (fs.existsSync(queryPath)) {
          const file = fs.readFileSync(queryPath, 'utf-8')
          files = {
            ...files,
            [func.schema]: file,
          }
        } else {
          errors.push({
            message: `The file ${func.schema} for the resolver of function ${funcName} does not exist`,
          })
        }
      }
    })
  }

  if (errors.length > 0) {
    out.log(
      chalk.bold(
        'The following errors occured while reading the graphcool.yml project definition:',
      ),
    )
    const messages = errors.map(e => `  ${chalk.red(e.message)}`).join('\n')
    out.log(messages + '\n')
    process.exit(1)
  }

  return {
    ...module,
    definition,
    files,
  }
}

function isFile(type) {
  return content => {
    return new RegExp(`\.${type}$`).test(content) && !content.startsWith('../')
  }
}

const isGraphQLFile = isFile('graphql(:.*)?')
const isFunctionFile = file => isFile('js')(file) || isFile('ts')(file)
