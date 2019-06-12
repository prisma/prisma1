import { PostgresConnector } from 'prisma-db-introspection'
import { generateClient } from '@prisma/photon'
import { isdlToDatamodel2 } from '@prisma/lift'
import { Introspect } from '@prisma/introspection'
import { PhotonGenerate } from '@prisma/photon'
import { ISDL } from 'prisma-datamodel'
import { Client } from 'pg'

const client = new Client({
  connectionString: 'postgres://localhost:5432/prisma-dev',
})

tests().map(t => {
  beforeAll(async () => {
    await client.connect()
  })

  afterAll(async () => {
    await client.end()
  })

  test(
    t.name,
    async () => {
      await client.query(t.after)
      await client.query(t.before)
      const isdl = await inspect(client, 'public')
      await generate(isdl)
      await client.query(t.after)
    },
    30000,
  )
})

async function inspect(client: Client, schema: string): Promise<ISDL> {
  const connector = new PostgresConnector(client)
  const result = await connector.introspect(schema)
  return result.getNormalizedDatamodel()
}

async function generate(isdl: ISDL) {
  const datamodel = await isdlToDatamodel2(isdl, [
    {
      name: 'pg',
      connectorType: 'postgres',
      url: 'postgres://localhost:5432/prisma-dev',
      config: {},
    },
  ])
  await generateClient(datamodel, process.cwd(), './out')
}

function tests() {
  return [
    {
      name: 'team.find({ id: 2 })',
      before: `
        create table if not exists teams (
          id serial primary key not null,
          name text not null unique
        );
        insert into teams (name) values ('a');
        insert into teams (name) values ('b');
      `,
      after: `
        drop table if exists teams cascade;
      `,
      expect: {},
    },
  ]
}
