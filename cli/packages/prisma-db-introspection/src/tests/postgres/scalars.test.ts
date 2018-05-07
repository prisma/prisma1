import { Introspector } from '../../Introspector'
import { Client } from 'pg'
import { connectionDetails } from './connectionDetails'

function introspect(): Promise<{ numTables: number; sdl: string; }> {
  return new Introspector(connectionDetails).introspect('DatabaseIntrospector')
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
      "a" char(1) DEFAULT NULL UNIQUE,
      "b" varchar(255) DEFAULT NULL,
      "c" text DEFAULT 'abc',
      "d" char(1) NOT NULL,
      "e" varchar(255) NOT NULL,
      "f" text NOT NULL,
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
      "b" timestamp NOT NULL
      );`)
  })

  test('Json columns', async () => {
    await testSchema(`CREATE TABLE "Booleans" (
      "a" json DEFAULT NULL,
      "b" json NOT NULL
      );`)
  })
})
