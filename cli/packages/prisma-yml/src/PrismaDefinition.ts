import { readDefinition } from './yaml'
import { PrismaDefinition } from 'prisma-json-schema'
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
import { FunctionInput, Header } from './types/rc'
import { URL } from 'url'
import chalk from 'chalk'
import { clusterEndpointMap, clusterEndpointMapReverse } from './constants'
import { replaceYamlValue } from './utils/yamlComment'
import { DefinitionMigrator } from './utils/DefinitionMigrator'

interface ErrorMessage {
  message: string
}

export interface EnvVars {
  [key: string]: string | undefined
}

export class PrismaDefinitionClass {
  definition?: PrismaDefinition
  rawJson?: any
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
    if (envPath) {
      if (!fs.pathExistsSync(envPath)) {
        envPath = path.join(process.cwd(), envPath)
      }

      if (!fs.pathExistsSync(envPath)) {
        throw new Error(`--env-file path '${envPath}' does not exist`)
      }
    }
    dotenv.config({ path: envPath })
    if (this.definitionPath) {
      await this.loadDefinition(args)
      const migrator = new DefinitionMigrator(this)
      const migrated = migrator.migrate(this.definitionPath)
      // if there was a migration, reload the definition
      if (migrated) {
        await this.loadDefinition(args)
      }

      this.validate()
    } else {
      throw new Error(
        `Couldn’t find \`prisma.yml\` file. Are you in the right directory?`,
      )
    }
  }

  private async loadDefinition(args) {
    const { definition, rawJson } = await readDefinition(
      this.definitionPath!,
      args,
      this.out,
      this.envVars,
    )
    this.definition = definition
    this.rawJson = rawJson
    this.definitionString = fs.readFileSync(this.definitionPath!, 'utf-8')
    this.typesString = this.getTypesString(this.definition)
    const secrets = this.definition.secret
    this.secrets = secrets ? secrets.replace(/\s/g, '').split(',') : null
  }

  get endpoint(): string | undefined {
    return (
      (this.definition && this.definition.endpoint) ||
      process.env.PRISMA_MANAGEMENT_API_ENDPOINT
    )
  }

  get clusterBaseUrl(): string | undefined {
    if (!this.definition || this.definition.cluster || !this.endpoint) {
      return undefined
    }
    const { clusterBaseUrl } = parseEndpoint(this.endpoint)
    return clusterBaseUrl
  }

  get service(): string | undefined {
    if (!this.definition) {
      return undefined
    }
    if (this.definition.service) {
      return this.definition.service
    }
    if (!this.endpoint) {
      return undefined
    }
    const { service } = parseEndpoint(this.endpoint)
    return service
  }

  get stage(): string | undefined {
    if (!this.definition) {
      return undefined
    }
    if (this.definition.stage) {
      return this.definition.stage
    }
    if (!this.endpoint) {
      return undefined
    }
    const { stage } = parseEndpoint(this.endpoint)
    return stage
  }

  get cluster(): string | undefined {
    if (!this.definition) {
      return undefined
    }
    if (this.definition.cluster) {
      return this.definition.cluster
    }
    if (!this.endpoint) {
      return undefined
    }
    const { clusterName } = parseEndpoint(this.endpoint)
    return clusterName
  }

  validate() {
    // const disableAuth = this.definition!.disableAuth
    // if (this.secrets === null && !disableAuth) {
    //   throw new Error(
    //     'Please either provide a secret in your prisma.yml or disableAuth: true',
    //   )
    // }

    // if (!this.service) {
    //   throw new Error(`Please provide a service property in your prisma.yml`)
    // }

    // if (!this.stage) {
    //   throw new Error(`Please provide a stage property in your prisma.yml`)
    // }

    // if (!this.cluster) {
    //   throw new Error(
    //     `Please either provide a cluster or endpoint property in your prisma.yml`,
    //   )
    // }

    // shared clusters need a workspace
    const clusterName = this.getClusterName()
    const cluster = this.env.clusterByName(clusterName!)!
    if (
      clusterName &&
      cluster &&
      (cluster.shared || cluster.isPrivate) &&
      !this.getWorkspace() &&
      clusterName !== 'shared-public-demo'
    ) {
      throw new Error(
        `Your \`cluster\` property in the prisma.yml is missing the workspace slug.
Make sure that your \`cluster\` property looks like this: ${chalk.bold(
          '<workspace>/<cluster-name>',
        )}. You can also remove the cluster property from the prisma.yml
and execute ${chalk.bold.green(
          'prisma deploy',
        )} again, to get that value auto-filled.`,
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

  getCluster(throws: boolean = false): Cluster | undefined {
    const clusterName = this.getClusterName()
    if (clusterName) {
      const cluster = this.env.clusterByName(clusterName)
      if (!cluster && clusterName !== 'local') {
        if (throws) {
          throw new Error(
            `Cluster ${clusterName}, that is provided in the prisma.yml could not be found.
If it is a private cluster, make sure that you're logged in with ${chalk.bold.green(
              'prisma login',
            )}`,
          )
        }
      } else {
        return cluster
      }
    }

    if (this.definition && this.endpoint) {
      const {
        clusterBaseUrl,
        isPrivate,
        local,
        shared,
        workspaceSlug,
        clusterName,
      } = parseEndpoint(this.endpoint)
      if (clusterBaseUrl) {
        const cluster = new Cluster(
          this.out!,
          clusterName,
          clusterBaseUrl,
          shared ? this.env.cloudSessionKey : undefined,
          local,
          shared,
          isPrivate,
          workspaceSlug,
        )
        this.env.removeCluster(clusterName)
        this.env.addCluster(cluster)
        return cluster
      }
    }

    return undefined
  }

  getTypesString(definition: PrismaDefinition) {
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
    return this.cluster || null
  }

  getWorkspace(): string | null {
    if (this.definition && this.definition.cluster) {
      const splitted = this.definition!.cluster!.split('/')
      if (splitted.length > 1) {
        return splitted[0]
      }
    }

    if (this.definition && this.endpoint) {
      const { workspaceSlug } = parseEndpoint(this.endpoint)
      if (workspaceSlug) {
        return workspaceSlug
      }
    }

    return null
  }

  getDeployName() {
    const cluster = this.getCluster()
    return concatName(cluster!, this.definition!.service!, this.getWorkspace())
  }

  getSubscriptions(): FunctionInput[] {
    if (this.definition && this.definition.subscriptions) {
      return Object.keys(this.definition!.subscriptions!).map(name => {
        const subscription = this.definition!.subscriptions![name]

        const url =
          typeof subscription.webhook === 'string'
            ? subscription.webhook
            : subscription.webhook.url
        const headers =
          typeof subscription.webhook === 'string'
            ? []
            : transformHeaders(subscription.webhook.headers)

        let query = subscription.query
        if (subscription.query.endsWith('.graphql')) {
          const queryPath = path.join(this.definitionDir, subscription.query)
          if (!fs.pathExistsSync(queryPath)) {
            throw new Error(
              `Subscription query ${queryPath} provided in subscription "${name}" in prisma.yml does not exist.`,
            )
          }
          query = fs.readFileSync(queryPath, 'utf-8')
        }

        return {
          name,
          query,
          headers,
          url,
        }
      })
    }
    return []
  }

  async addCluster(cluster: string, args: any) {
    if (!this.definition!.cluster) {
      this.definition!.cluster = cluster
      const newString = this.definitionString + `\ncluster: ${cluster}`
      fs.writeFileSync(this.definitionPath!, newString)
      await this.load(args)
    }
  }

  replaceEndpoint(newEndpoint) {
    this.definitionString = replaceYamlValue(
      this.definitionString,
      'endpoint',
      newEndpoint,
    )
    fs.writeFileSync(this.definitionPath!, this.definitionString)
  }

  getEndpoint(serviceInput?: string, stageInput?: string) {
    const cluster = this.getCluster()
    const service = serviceInput || this.service
    const stage = stageInput || this.stage
    const workspace = this.getWorkspace()

    if (service && stage && cluster) {
      return getEndpoint(cluster!, service!, stage!, workspace)
    }

    return null
  }
}

export function concatName(
  cluster: Cluster,
  name: string,
  workspace: string | null,
) {
  if (cluster.shared) {
    const workspaceString = workspace ? `${workspace}~` : ''
    return `${workspaceString}${name}`
  }

  return name
}

function transformHeaders(headers?: { [key: string]: string }): Header[] {
  if (!headers) {
    return []
  }
  return Object.keys(headers).map(key => ({
    name: key,
    value: headers[key],
  }))
}

export function parseEndpoint(
  endpoint: string,
): {
  service: string
  clusterBaseUrl: string
  stage: string
  isPrivate: boolean
  local: boolean
  shared: boolean
  workspaceSlug: string | undefined
  clusterName: string
} {
  const url = new URL(endpoint)
  const splittedPath = url.pathname.split('/')
  const shared =
    url.origin.includes('eu1.prisma') || url.origin.includes('us1.prisma')
  const isPrivate = !shared
  // assuming, that the pathname always starts with a leading /, we always can ignore the first element of the split array
  const service =
    splittedPath.length > 3 ? splittedPath[2] : splittedPath[1] || 'default'
  const stage =
    splittedPath.length > 3 ? splittedPath[3] : splittedPath[2] || 'default'
  const workspaceSlug = splittedPath.length > 3 ? splittedPath[1] : undefined
  return {
    clusterBaseUrl: url.origin,
    service,
    stage,
    local: isLocal(url.origin),
    isPrivate,
    shared,
    workspaceSlug,
    clusterName: getClusterName(url.origin),
  }
}

export function getEndpoint(
  cluster: Cluster,
  service: string,
  stage: string,
  workspace?: string | null,
) {
  let url = cluster.baseUrl
  if (service === 'default' && stage === 'default' && !workspace) {
    return url
  }
  if (stage === 'default' && !workspace) {
    return `${url}/${service}`
  }
  if (workspace) {
    return `${url}/${workspace}/${service}/${stage}`
  }

  return `${url}/${service}/${stage}`
}

export function getEndpointFromRawProps(
  clusterWorkspace: string,
  service: string,
  stage: string,
) {
  const splitted = clusterWorkspace.split('/')
  const cluster = splitted.length > 1 ? splitted[1] : splitted[0]
  const workspace = splitted.length > 1 ? splitted[0] : undefined
}

function getClusterName(origin): string {
  if (clusterEndpointMapReverse[origin]) {
    return clusterEndpointMapReverse[origin]
  }

  if (origin.endsWith('prisma.sh')) {
    return origin.split('_')[0].replace(/https?:\/\//, '')
  }

  if (isLocal(origin)) {
    return 'local'
  }

  return 'default'
}

const isLocal = origin =>
  origin.includes('localhost') || origin.includes('127.0.0.1')
