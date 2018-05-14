import { Prisma } from 'prisma-binding'

export interface Context {
  db: Prisma
  request: any
}

export interface FileMeta {
  name: string
  size: number
  contentType: string
  secret: string
}
