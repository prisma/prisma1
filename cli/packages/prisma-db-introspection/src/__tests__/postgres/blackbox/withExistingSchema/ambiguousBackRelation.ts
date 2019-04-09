import Connectors from '../../../../connectors'
import { Client } from 'pg'
import { connectionDetails } from '../connectionDetails'
import { PostgresConnector } from '../../../../databases/relational/postgres/postgresConnector'
import { DatabaseType, DefaultParser } from 'prisma-datamodel'
import { connect } from 'tls'

const existingSchema = `
type User @db(name: "users") {
    id: ID! @unique
    asked: [Question] @relation(name: "question_asked")
    answered: [Question] @relation(name: "question_answer")
}
  
type Question @db(name: "questions") {
    id: ID! @unique
    asker: User! @relation(name: "question_asked") @db(name: "asker_id")
    answerer: User @relation(name: "question_answer") @db(name: "answerer_id")
}
`

async function introspect(client: Client) {
  const existing = DefaultParser.create(
    DatabaseType.postgres,
  ).parseFromSchemaString(existingSchema)
  return (await Connectors.create(DatabaseType.postgres, client).introspect(
    'test',
  )).renderToNormalizedDatamodelString(existing)
}

async function testSchema(sql: string) {
  const client = new Client(connectionDetails)
  await client.connect()
  await client.query('DROP SCHEMA IF EXISTS "test" cascade;')
  await client.query(sql)

  expect(await introspect(client)).toMatchSnapshot()

  await client.end()
}

describe('Introspector', () => {
  test('Type with scalar lists and existing schema', async () => {
    await testSchema(`     
        CREATE SCHEMA "test";
            
        CREATE TABLE "test".questions (
            id integer NOT NULL,
            asker_id integer NOT NULL,
            answerer_id integer
        );

        CREATE TABLE "test".users (
            id integer NOT NULL
        );

        ALTER TABLE ONLY "test".questions
        ADD CONSTRAINT questions_pk PRIMARY KEY (id);

        ALTER TABLE ONLY "test".users
        ADD CONSTRAINT users_pk PRIMARY KEY (id);
        
        ALTER TABLE ONLY "test".questions
        ADD CONSTRAINT questions_fk1 FOREIGN KEY (asker_id) REFERENCES users(id);

        ALTER TABLE ONLY "test".questions
        ADD CONSTRAINT questions_fk2 FOREIGN KEY (answerer_id) REFERENCES users(id);
    `)
  })
})
