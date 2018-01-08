import { readDefinition } from './yaml'
import { GraphcoolDefinition } from 'graphcool-json-schema'
import * as fs from 'fs-extra'
import { mapValues } from 'lodash'
import * as yamlParser from 'yaml-ast-parser'
import * as dotenv from 'dotenv'
import * as path from 'path'
import * as jwt from 'jsonwebtoken'
import { Args } from './types/common'
import { StageNotFound } from './errors/StageNotFound'
import { Environment } from './Environment'
import { IOutput } from './Output'
import { Cluster } from './Cluster'

interface ErrorMessage {
  message: string
}

export interface EnvVars {
  [key: string]: string | undefined
}

export class GraphcoolDefinitionClass {
  definition?: GraphcoolDefinition
  typesString?: string
  secrets: string[] | null
  definitionPath?: string | null
  definitionDir: string
  env: Environment
  out?: IOutput
  envVars: any
  private definitionString: string
  constructor(
    env: Environment,
    definitionPath?: string | null,
    envVars: EnvVars = process.env,
    out?: IOutput,
  ) {
    this.secrets = null
    this.definitionPath = definitionPath
    if (definitionPath) {
      this.definitionDir = path.dirname(definitionPath)
    }
    this.env = env
    this.out = out
    this.envVars = envVars
  }
  async load(args: Args, envPath?: string) {
    dotenv.config({ path: envPath })
    if (this.definitionPath) {
      this.definition = await readDefinition(
        this.definitionPath,
        args,
        this.out,
        this.envVars,
      )
      this.definitionString = fs.readFileSync(this.definitionPath, 'utf-8')
      this.typesString = this.getTypesString(this.definition)
      const secrets = this.definition.secret
      this.secrets = secrets ? secrets.replace(/\s/g, '').split(',') : null
      this.validate()
    }
  }

  validate() {
    const disableAuth = this.definition!.disableAuth
    if (this.secrets === null && !disableAuth) {
      throw new Error(
        'Please either provide a secret in your graphcool.yml or disableAuth: true',
      )
    }

    // shared clusters need a workspace
    const clusterName = this.getClusterName()
    if (
      clusterName &&
      this.env.sharedClusters.includes(clusterName) &&
      !this.getWorkspace()
    ) {
      throw new Error(
        `You provided the cluster ${clusterName}, but it needs to be prepended with the workspace you want to deploy to`,
      )
    }
    this.env.sharedClusters
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

  getCluster(): Cluster | undefined {
    const clusterName = this.getClusterName()
    if (clusterName) {
      const cluster = this.env.clusterByName(clusterName)
      if (!cluster && clusterName !== 'local') {
        throw new Error(
          `Cluster ${cluster}, that is provided in the graphcoo.yml could not be found.`,
        )
      }
      return cluster
    }

    return undefined
  }

  getTypesString(definition: GraphcoolDefinition) {
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

  getClusterName(): string | null {
    if (this.definition && this.definition.cluster) {
      return this.definition!.cluster!.split('/').slice(-1)[0]
    }
    return null
  }

  getWorkspace(): string | null {
    if (this.definition && this.definition.cluster) {
      const splitted = this.definition!.cluster!.split('/')
      if (splitted.length > 1) {
        return splitted[0]
      }
    }

    return null
  }

  async addCluster(cluster: string, args: any) {
    if (!this.definition!.cluster) {
      this.definition!.cluster = cluster
      const newString = this.definitionString + `\ncluster: ${cluster}`
      fs.writeFileSync(this.definitionPath!, newString)
      await this.load(args)
    }
  }
}
