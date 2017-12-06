import { Config } from '../Config'
import { Output } from '../index'
import { readDefinition } from './yaml'
import { Args } from '../types/common'
import { GraphcoolDefinition } from 'graphcool-json-schema'
import * as fs from 'fs-extra'
import chalk from 'chalk'
import { Environment } from '../Environment'
import { mapValues } from 'lodash'

interface ErrorMessage {
  message: string
}

export class GraphcoolDefinitionClass {
  out: Output
  config: Config
  definition?: GraphcoolDefinition
  typesString?: string
  constructor(out: Output, config: Config) {
    this.out = out
    this.config = config
  }
  async load(env: Environment, args: Args) {
    if (this.config.definitionPath) {
      this.definition = await readDefinition(
        this.config.definitionPath,
        this.out,
        args,
      )
      this.definition.stages = this.resolveStageAliases(this.definition.stages)
      this.ensureOfClusters(this.definition, env)
      this.typesString = this.getTypesString(this.definition)
    } else {
      throw new Error(`Please create a graphcool.yml`)
    }
  }

  private getTypesString(definition: GraphcoolDefinition) {
    const typesPaths = Array.isArray(definition.datamodel)
      ? definition.datamodel
      : [definition.datamodel]

    const errors: ErrorMessage[] = []
    let allTypes = ''
    typesPaths.forEach(typesPath => {
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

  private ensureOfClusters(definition: GraphcoolDefinition, env: Environment) {
    Object.keys(definition.stages).forEach(stageName => {
      const referredCluster = definition.stages[stageName]
      if (!env.clusters.find(c => c.name === referredCluster)) {
        throw new Error(
          `Could not find cluster '${
            referredCluster
          }', which is used in stage '${stageName}'.`,
        )
      }
    })
  }

  private resolveStageAliases = stages =>
    mapValues(stages, target => this.resolveStage(target, stages))

  private resolveStage = (
    stage: string,
    stages: { [key: string]: string },
  ): string => (stage[stage] ? this.resolveStage(stages[stage], stages) : stage)

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
