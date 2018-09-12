import { Prisma as BaseClient } from './Prisma'
import { BasePrismaOptions } from './types'

export function makePrismaClientClass<T>({
  typeDefs,
  endpoint,
  secret,
}: {
  typeDefs: string
  endpoint: string
  secret?: string
}): T {
  return class Client extends BaseClient {
    constructor(options: BasePrismaOptions) {
      super({ typeDefs, endpoint, secret, ...options })
    }
  } as any
}
