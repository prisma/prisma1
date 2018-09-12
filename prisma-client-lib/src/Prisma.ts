import { Exists, PrismaOptions } from './types'
import { sign } from 'jsonwebtoken'
import { makePrismaLink } from './link'
import { SharedLink } from './SharedLink'
import { getTypesAndWhere } from './utils'
import { getCachedTypeDefs, getCachedRemoteSchema } from './cache'
import { Client } from './Client'
import { BatchedGraphQLClient } from 'http-link-dataloader'

const sharedLink = new SharedLink()

export class Prisma extends Client {
  $exists: Exists
  token: string
  client: BatchedGraphQLClient

  constructor({
    typeDefs,
    endpoint,
    secret,
    fragmentReplacements,
    debug,
  }: PrismaOptions) {
    if (!typeDefs) {
      throw new Error('No `typeDefs` provided when calling `new Prisma()`')
    }

    if (typeDefs.endsWith('.graphql')) {
      typeDefs = getCachedTypeDefs(typeDefs)
    }

    if (endpoint === undefined) {
      throw new Error(
        `No Prisma endpoint found. Please provide the \`endpoint\` constructor option.`,
      )
    }

    if (!endpoint!.startsWith('http')) {
      throw new Error(`Invalid Prisma endpoint provided: ${endpoint}`)
    }

    fragmentReplacements = fragmentReplacements || []

    debug = debug || false

    const token = secret ? sign({}, secret!) : undefined
    const link = makePrismaLink({ endpoint: endpoint!, token, debug })

    const remoteSchema = getCachedRemoteSchema(typeDefs, sharedLink)

    const before = () => {
      sharedLink.setInnerLink(link)
    }

    super({
      schema: remoteSchema,
      fragmentReplacements,
      before,
      debug,
    })

    this.$exists = this.buildExists()
    this.token = token
    this.client = new BatchedGraphQLClient(endpoint, {
      headers: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : {},
    })
  }

  private buildExists(): Exists {
    const queryType = this.schema.getQueryType()
    if (!queryType) {
      return {}
    }
    if (queryType) {
      const types = getTypesAndWhere(queryType)

      return types.reduce((acc, { type, pluralFieldName }) => {
        const firstLetterLowercaseTypeName =
          type[0].toLowerCase() + type.slice(1)
        return {
          ...acc,
          [firstLetterLowercaseTypeName]: args => {
            // TODO: when the fragment api is there, only add one field
            return this[pluralFieldName]({ where: args }).then(
              res => res.length > 0,
            )
          },
        }
      }, {})
    }

    return {}
  }
}
