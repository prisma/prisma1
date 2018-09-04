import { Prisma as BaseBinding } from './Prisma'
import { BasePrismaOptions } from './types'

export function makePrismaBindingClass<T>({
  typeDefs,
  endpoint,
  secret,
}: {
  typeDefs: string
  endpoint: string
  secret?: string
}): T {
  return class Binding extends BaseBinding {
    constructor(options: BasePrismaOptions) {
      super({ typeDefs, endpoint, secret, ...options })
    }
  } as any
}
