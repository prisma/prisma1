import { ISDL, DatabaseType } from 'prisma-datamodel'

export interface INormalizer {
  normalize(model: ISDL)
}
