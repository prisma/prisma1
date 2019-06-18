import { InitPromptResult } from './types'

// TODO: Prisma correct DB configuration
const printDatabase = (result: InitPromptResult) => {
  return `datasource db {
        provider = "sqlite"
        url      = "file:dev.db"
        default  = true
    }`
}

export const defaultPrismaConfig = (result: InitPromptResult) => `
${printDatabase(result)}

generator photon {
    provider = "photonjs"
    output   = "node_modules/@generated/photon"
}

model User {
    id    String  @default(cuid()) @id
    email String  @unique
    name  String?
    posts Post[]
}
  
model Post {
    id        String   @default(cuid()) @id
    published Boolean
    title     String
    content   String?
    author    User?
}
`
