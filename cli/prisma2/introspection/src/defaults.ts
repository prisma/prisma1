export const defaultPrismaConfig = `
datasource db {
    provider = "sqlite"
    url      = "file:dev.db"
    default  = true
}

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
