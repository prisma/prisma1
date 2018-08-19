import { Exists, PrismaOptions } from './types'
import { sign } from 'jsonwebtoken'
import { makePrismaLink } from './link'
import { buildExistsInfo } from './info'
import { SharedLink } from './SharedLink'
import { getTypesAndWhere } from './utils'
import { getCachedTypeDefs, getCachedRemoteSchema } from './cache'
import { Binding } from '.'
import { BatchedGraphQLClient } from 'http-link-dataloader'
import { print } from 'graphql'
const debug = require('debug')('prisma')

const sharedLink = new SharedLink()

export class Prisma extends Binding {
  exists: Exists
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

    this.exists = this.buildExists()
    this.token = token
    this.client = new BatchedGraphQLClient(endpoint, {
      headers: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : {},
    })
  }

  execute(document, variables) {
    debug('execute')
    const query = print(document)
    debug('printed')

    return this.client.request(query, variables) as any
  }

  private buildExists(): Exists {
    const queryType = this.schema.getQueryType()
    if (!queryType) {
      return {}
    }
    if (queryType) {
      const types = getTypesAndWhere(queryType)

      return types.reduce((acc, { type, pluralFieldName }) => {
        return {
          ...acc,
          [type]: args =>
            this.delegate(
              'query',
              pluralFieldName,
              { where: args },
              buildExistsInfo(pluralFieldName, this.schema),
            ).then(res => res.length > 0),
        }
      }, {})
    }

    return {}
  }
}
