import { Client as BaseClient } from './Client'
import { BaseClientOptions, Model } from './types'

export function makePrismaClientClass<T>({
  typeDefs,
  endpoint,
  secret,
  models,
}: {
  typeDefs: string
  endpoint: string
  secret?: string
  models: Model[]
}): T {
  return class Client extends BaseClient {
    constructor(options: BaseClientOptions) {
      super({ typeDefs, endpoint, secret, models, ...options })
    }
  } as any
}
