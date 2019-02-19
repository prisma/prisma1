import { Cluster, PrismaDefinitionClass } from 'prisma-yml'
import { GraphQLClient } from 'graphql-request'
import IDatabaseClient from '../IDatabaseClient'
import { DatabaseType } from 'prisma-datamodel'

const SERVICE_NAME = 'prisma-temporary-service'
const SERVICE_STAGE = 'temporary-stage'
const SERVICE_SECRET = 'prisma-instrospection-secret'

export class PrismaDBClient implements IDatabaseClient {
  cluster: Cluster
  client: GraphQLClient
  definition: PrismaDefinitionClass
  databaseType: DatabaseType

  constructor(definition: PrismaDefinitionClass) {
    this.definition = definition
  }

  async query(query: string, variables: string[]): Promise<any[]> {
    const finalQuery = this.replace(query, variables)
    const databases = await this.getDatabases()

    if (!databases || !databases[0]) {
      throw new Error(`Prisma Config doesn't have any database connection`)
    }

    const res = await this.client.request(
      `
      mutation executeRaw($query: String! $database: PrismaDatabase) {
        rows: executeRaw(
          database: $database
          query: $query
        )
      }
    `,
      {
        query: finalQuery,
        database: databases[0],
      },
    )

    return (res as any).rows as any[]
  }

  async getDatabases(): Promise<string[]> {
    const result = await this.client.request<any>(
      `{
      __type(name: "PrismaDatabase") {
        kind
        enumValues {
          name
        }
      }
    }`,
    )

    if (result && result.__type && result.__type.enumValues) {
      return result.__type.enumValues.map(v => v.name)
    }

    return []
  }

  protected async setDatabaseType(): Promise<void> {
    const {
      data: {
        serverInfo: { primaryConnector },
      },
    } = await this.cluster
      .request(
        `{
          serverInfo {
            primaryConnector
          }
        }`,
      )
      .then(res => res.json())

    const typeMap = {
      mysql: DatabaseType.mysql,
      postgres: DatabaseType.postgres,
      mongo: DatabaseType.mongo,
    }

    const databaseType = typeMap[primaryConnector]
    if (!databaseType) {
      throw new Error(`Could not identify primaryConnector ${primaryConnector} as database type`)
    }

    this.databaseType = databaseType
  }

  replace(query: string, variables: string[] = []): string {
    let queryString = query

    for (const [index, variable] of variables.entries()) {
      const pattern = this.databaseType === DatabaseType.postgres ? `\\$${index + 1}` : '\\?'
      const regex = new RegExp(pattern, 'g')
      queryString = queryString.replace(regex, `'${variable}'`)
    }

    return queryString
  }

  async connect() {
    await this.setDatabaseType()
    const cluster = await this.definition.getCluster()
    if (!cluster) {
      throw new Error('Could not get Prisma server for introspection')
    }
    await this.cluster
      .request(
        `mutation($input: AddProjectInput!) {
          addProject(input: $input) {
            clientMutationId
          }
        }`,
        {
          input: {
            name: SERVICE_NAME,
            stage: SERVICE_STAGE,
            secrets: [SERVICE_SECRET],
          },
        },
      )
      .then(res => res.json())

    const endpoint = this.cluster.getApiEndpoint(SERVICE_NAME, SERVICE_STAGE)
    const secretsBackup = this.definition.secrets
    this.definition.secrets = [SERVICE_SECRET]
    const token = this.definition.getToken(SERVICE_NAME, SERVICE_STAGE)
    this.definition.secrets = secretsBackup
    this.client = new GraphQLClient(endpoint, {
      headers: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : {},
    })
  }

  async end() {
    try {
      await this.cluster
        .request(
          `mutation($input: DeleteProjectInput!) {
            deleteProject(input: $input) {
              clientMutationId
            }
          }`,
          {
            input: {
              name: SERVICE_NAME,
              stage: SERVICE_STAGE,
            },
          },
        )
        .then(res => res.json())
    } catch (e) {
      // ignore error
    }
  }
}
