import Connectors from '../../connectors'
import { Client } from 'pg'
import { connectionDetails } from './connectionDetails'
import { PostgresConnector } from '../../databases/relational/postgres/postgresConnector'
import { DatabaseType } from 'prisma-datamodel'
import { connect } from 'tls'

async function introspect() {
  const client = new Client(connectionDetails)
  return (await Connectors.create(DatabaseType.postgres, client).introspect('DatabaseIntrospector')).renderToDatamodelString()
}

async function testSchema(sql: string) {
  const client = new Client(connectionDetails)
  await client.connect()
  await client.query('DROP SCHEMA IF EXISTS DatabaseIntrospector cascade;')
  await client.query('CREATE SCHEMA DatabaseIntrospector;')
  await client.query('SET search_path TO DatabaseIntrospector;')
  await client.query(sql)

  expect(await introspect()).toMatchSnapshot()

  await client.end()
}

describe('Introspector', () => {
  test('text columns', async () => {
    await testSchema(`CREATE TABLE "Strings" (
      "pk" varchar(55) NOT NULL PRIMARY KEY,
      "a" char(1) DEFAULT NULL UNIQUE,
      "aa" char(1) NOT NULL UNIQUE,
      "b" varchar(255) DEFAULT NULL,
      "c" text DEFAULT 'abc',
      "cc" text DEFAULT 'abc' NOT NULL,
      "d" char(1) NOT NULL,
      "e" varchar(255) NOT NULL,
      "f" text NOT NULL,
      "g" uuid DEFAULT NULL,
      "h" uuid NOT NULL,
      constraint foo unique(b)
      );`)
  })

  test('int columns', async () => {
    await testSchema(`CREATE TABLE "Ints" (
      "a" smallint DEFAULT 3,
      "b" integer DEFAULT NULL,
      "c" bigint DEFAULT NULL,
      "d" smallint NOT NULL,
      "e" integer NOT NULL,
      "f" bigint NOT NULL
      );`)
  })

  test('float columns', async () => {
    await testSchema(`CREATE TABLE "Floats" (
      "a" real DEFAULT 3.4,
      "b" double precision DEFAULT NULL,
      "c" float4 DEFAULT NULL,
      "d" float8 DEFAULT NULL,
      "e" real NOT NULL,
      "f" double precision NOT NULL,
      "g" float4 NOT NULL,
      "h" float8 NOT NULL,
      "i" numeric DEFAULT NULL,
      "j" numeric NOT NULL
      );`)
  })

  test('boolean columns', async () => {
    await testSchema(`CREATE TABLE "Booleans" (
      "a" boolean DEFAULT NULL,
      "b" boolean NOT NULL
      );`)
  })

  test('DateTime columns', async () => {
    await testSchema(`CREATE TABLE "Booleans" (
      "a" timestamp DEFAULT NULL,
      "b" timestamp NOT NULL,
      "c" timestamp NOT NULL DEFAULT now()
      );`)
  })

  test('Json columns', async () => {
    await testSchema(`CREATE TABLE "Booleans" (
      "a" json DEFAULT NULL,
      "b" json NOT NULL
      );`)
  })

  test('Unmapped columns', async () => {
    await testSchema(`CREATE TABLE "Unmapped" (
      "a" jsonb,
      "c" tsvector,
      "d" date[]
      );`)
  })

  test('uuid columns', async () => {
    await testSchema(`CREATE TABLE "UUIDs" (
      "pk" uuid NOT NULL PRIMARY KEY,
      "nk" uuid NOT NULL,
      "tk" uuid
      );`)
  })
})
