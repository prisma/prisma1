import { Client as BaseClient } from './Client'
import { BaseClientOptions } from './types'

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
    constructor(options: BaseClientOptions) {
      super({ typeDefs, endpoint, secret, ...options })
    }
  } as any
}
