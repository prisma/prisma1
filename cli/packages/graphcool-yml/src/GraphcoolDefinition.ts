import { readDefinition } from './yaml'
import { GraphcoolDefinition } from 'graphcool-json-schema'
import * as fs from 'fs-extra'
import { mapValues } from 'lodash'
import * as yamlParser from 'yaml-ast-parser'
import * as dotenv from 'dotenv'
import * as path from 'path'
import * as jwt from 'jsonwebtoken'
import { Args, Stages } from './types/common'
import { StageNotFound } from './errors/StageNotFound'
import { Environment } from './Environment'
import { IOutput } from './Output'

interface ErrorMessage {
  message: string
}

export class GraphcoolDefinitionClass {
  definition?: GraphcoolDefinition
  typesString?: string
  secrets: string[] | null
  rawStages: Stages
  definitionPath?: string | null
  definitionDir: string
  env: Environment
  out?: IOutput
  private definitionString: string
  constructor(env: Environment, definitionPath?: string | null, out?: IOutput) {
    this.secrets = null
    this.definitionPath = definitionPath
    if (definitionPath) {
      this.definitionDir = path.dirname(definitionPath)
    }
    this.env = env
    this.out = out
  }
  async load(args: Args, envPath?: string) {
    dotenv.config({ path: envPath })
    if (this.definitionPath) {
      this.definition = await readDefinition(
        this.definitionPath,
        args,
        this.out,
      )
      this.definitionString = fs.readFileSync(this.definitionPath, 'utf-8')
      this.rawStages = this.definition.stages
      this.definition.stages = this.resolveStageAliases(this.definition.stages)
      this.typesString = this.getTypesString(this.definition)
      const secrets = process.env.GRAPHCOOL_SECRET || this.definition.secret
      this.secrets = secrets ? secrets.replace(/\s/g, '').split(',') : null
      this.ensureOfClusters(this.definition, this.env)
      const disableAuth =
        typeof process.env.GRAPHCOOL_DISABLE_AUTH !== 'undefined'
          ? this.readBool(process.env.GRAPHCOOL_DISABLE_AUTH)
          : this.definition.disableAuth
      if (this.secrets === null && !disableAuth) {
        throw new Error(
          'Please either provide a secret in your graphcool.yml or disableAuth: true',
        )
      }
    }
  }

  private ensureOfClusters(definition: GraphcoolDefinition, env: Environment) {
    Object.keys(definition.stages).forEach(stageName => {
      const referredCluster = definition.stages[stageName]
      if (!env.clusters.find(c => c.name === referredCluster)) {
        throw new Error(
          `Could not find cluster '${referredCluster}', which is used in stage '${stageName}'.`,
        )
      }
    })
  }

  readBool(value?: string) {
    if (value) {
      const trimmed = value.trim()
      if (trimmed === 'true') {
        return true
      }
      if (trimmed === 'false') {
        return false
      }
    }

    return false
  }
  getToken(serviceName: string, stageName: string): string | undefined {
    if (this.secrets) {
      const data = {
        data: {
          service: `${serviceName}@${stageName}`,
          roles: ['admin'],
        },
      }
      return jwt.sign(data, this.secrets[0], {
        expiresIn: '7d',
      })
    }

    return undefined
  }

  getStage(name: string, throws: boolean = false): string | undefined {
    const stage =
      this.definition &&
      (this.definition.stages[name] || this.definition.stages.default)
    if (!throws) {
      return stage
    }

    if (!stage) {
      throw new StageNotFound(name)
    }

    return stage
  }

  setStage(name: string, clusterName: string) {
    let defaultString = ''
    if (Object.keys(this.rawStages).length === 0) {
      defaultString = `\n  default: ${name}`
    }
    this.definitionString = this.insertToDefinition(
      this.definitionString,
      'stages',
      `${defaultString}\n  ${name}: ${clusterName}`,
    )
  }

  insertToDefinition(file: string, key: string, insertion: string) {
    const obj = yamlParser.safeLoad(file)

    const mapping = obj.mappings.find(m => m.key.value === key)
    if (mapping) {
      const end = mapping.endPosition

      const newFile = file.slice(0, end) + insertion + file.slice(end)
      const valueStart = mapping.value.startPosition
      const valueEnd = mapping.value.endPosition
      if (mapping.value && valueEnd - valueStart < 4) {
        return newFile.slice(0, valueStart) + newFile.slice(valueEnd)
      }

      return newFile
    } else {
      return file + `\n${key}: ` + insertion
    }
  }

  save() {
    fs.writeFileSync(this.definitionPath!, this.definitionString)
  }

  private getTypesString(definition: GraphcoolDefinition) {
    const typesPaths = Array.isArray(definition.datamodel)
      ? definition.datamodel
      : [definition.datamodel]

    const errors: ErrorMessage[] = []
    let allTypes = ''
    typesPaths.forEach(unresolvedTypesPath => {
      const typesPath = path.join(this.definitionDir, unresolvedTypesPath)
      if (fs.existsSync(typesPath)) {
        const types = fs.readFileSync(typesPath, 'utf-8')
        allTypes += types + '\n'
      } else {
        throw new Error(
          `The types definition file "${typesPath}" could not be found.`,
        )
      }
    })

    return allTypes
  }

  private resolveStageAliases = stages =>
    mapValues(stages, target => this.resolveStage(target, stages))

  private resolveStage = (
    stage: string,
    stages: { [key: string]: string },
  ): string =>
    stages[stage] ? this.resolveStage(stages[stage], stages) : stage

  get default(): string | null {
    if (
      this.definition &&
      this.definition.stages &&
      this.definition.stages.default
    ) {
      return this.definition.stages.default
    }

    return null
  }
}
