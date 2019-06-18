import { InitPromptResult } from './types'
import { DatabaseType } from 'prisma-datamodel'

// TODO: Resolve SQLite URI properly
const printDatabase = (result: InitPromptResult) => {
  const credentials = result.introspectionResult && result.introspectionResult.credentials
  return `datasource db {
        provider = "${(credentials && credentials.type) || DatabaseType.sqlite}"
        url      = "${(credentials && credentials.uri) || `file:dev.db`}"
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
