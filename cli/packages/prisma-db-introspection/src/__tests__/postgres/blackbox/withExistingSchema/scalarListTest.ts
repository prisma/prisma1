import Connectors from '../../../../connectors'
import { Client } from 'pg'
import { connectionDetails } from '../connectionDetails'
import { PostgresConnector } from '../../../../databases/relational/postgres/postgresConnector'
import { DatabaseType, DefaultParser } from 'prisma-datamodel'
import { connect } from 'tls'

const existingSchema = `
type User {
    id: ID! @unique
    name355: String! @db(name: "name300")
    car: Car
    houses: [House!]!
}
  
type Car {
    id: ID! @unique
}
  
type House {
    id: ID! @unique
} 
`

async function introspect(client: Client) {
  const existing = DefaultParser.create(DatabaseType.postgres).parseFromSchemaString(existingSchema)
  return (await Connectors.create(DatabaseType.postgres, client).introspect(
    'service$stage',
  )).renderToNormalizedDatamodelString(existing)
}

async function testSchema(sql: string) {
  const client = new Client(connectionDetails)
  await client.connect()
  await client.query('DROP SCHEMA IF EXISTS "service$stage" cascade;')
  await client.query(sql)

  expect(await introspect(client)).toMatchSnapshot()

  await client.end()
}

describe('Introspector', () => {
  test('Type with scalar lists and existing schema', async () => {
    await testSchema(`--
    -- PostgreSQL database dump
    --
    
    -- Dumped from database version 10.1
    -- Dumped by pg_dump version 10.5
    
    SET statement_timeout = 0;
    SET lock_timeout = 0;
    SET idle_in_transaction_session_timeout = 0;
    SET client_encoding = 'UTF8';
    SET standard_conforming_strings = on;
    SELECT pg_catalog.set_config('search_path', '', false);
    SET check_function_bodies = false;
    SET client_min_messages = warning;
    SET row_security = off;
    
    --
    -- Name: service$stage; Type: SCHEMA; Schema: -; Owner: -
    --
    
    CREATE SCHEMA "service$stage";
    
    
    SET default_tablespace = '';
    
    SET default_with_oids = false;
    
    --
    -- Name: Car; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."Car" (
        id character varying(25) NOT NULL,
        name text NOT NULL,
        "updatedAt" timestamp(3) without time zone NOT NULL,
        "createdAt" timestamp(3) without time zone NOT NULL
    );
    
    
    --
    -- Name: House; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."House" (
        id character varying(25) NOT NULL,
        windows integer NOT NULL,
        "updatedAt" timestamp(3) without time zone NOT NULL,
        "createdAt" timestamp(3) without time zone NOT NULL
    );
    
    
    --
    -- Name: User; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."User" (
        id character varying(25) NOT NULL,
        name355 text NOT NULL,
        "updatedAt" timestamp(3) without time zone NOT NULL,
        "createdAt" timestamp(3) without time zone NOT NULL
    );
    
    
    --
    -- Name: User_scalarIntList; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."User_scalarIntList" (
        "nodeId" character varying(25) NOT NULL,
        "position" integer NOT NULL,
        value integer NOT NULL
    );
    
    
    --
    -- Name: User_scalarStringList; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."User_scalarStringList" (
        "nodeId" character varying(25) NOT NULL,
        "position" integer NOT NULL,
        value text NOT NULL
    );
    
    
    --
    -- Name: _CarToUser; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."_CarToUser" (
        id character(25) NOT NULL,
        "A" character varying(25) NOT NULL,
        "B" character varying(25) NOT NULL
    );
    
    
    --
    -- Name: _HouseToUser; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."_HouseToUser" (
        id character(25) NOT NULL,
        "A" character varying(25) NOT NULL,
        "B" character varying(25) NOT NULL
    );
    
    
    --
    -- Name: _RelayId; Type: TABLE; Schema: service$stage; Owner: -
    --
    
    CREATE TABLE "service$stage"."_RelayId" (
        id character varying(36) NOT NULL,
        "stableModelIdentifier" character varying(25) NOT NULL
    );
    
    
    --
    -- Name: Car Car_pkey; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."Car"
        ADD CONSTRAINT "Car_pkey" PRIMARY KEY (id);
    
    
    --
    -- Name: House House_pkey; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."House"
        ADD CONSTRAINT "House_pkey" PRIMARY KEY (id);
    
    
    --
    -- Name: User User_pkey; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."User"
        ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);
    
    
    --
    -- Name: User_scalarIntList User_scalarIntList_pkey; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."User_scalarIntList"
        ADD CONSTRAINT "User_scalarIntList_pkey" PRIMARY KEY ("nodeId", "position");
    
    
    --
    -- Name: User_scalarStringList User_scalarStringList_pkey; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."User_scalarStringList"
        ADD CONSTRAINT "User_scalarStringList_pkey" PRIMARY KEY ("nodeId", "position");
    
    
    --
    -- Name: _CarToUser _CarToUser_pkey; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."_CarToUser"
        ADD CONSTRAINT "_CarToUser_pkey" PRIMARY KEY (id);
    
    
    --
    -- Name: _HouseToUser _HouseToUser_pkey; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."_HouseToUser"
        ADD CONSTRAINT "_HouseToUser_pkey" PRIMARY KEY (id);
    
    
    --
    -- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."_RelayId"
        ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);
    
    
    --
    -- Name: _CarToUser_AB_unique; Type: INDEX; Schema: service$stage; Owner: -
    --
    
    CREATE UNIQUE INDEX "_CarToUser_AB_unique" ON "service$stage"."_CarToUser" USING btree ("A", "B");
    
    
    --
    -- Name: _CarToUser_B; Type: INDEX; Schema: service$stage; Owner: -
    --
    
    CREATE INDEX "_CarToUser_B" ON "service$stage"."_CarToUser" USING btree ("B");
    
    
    --
    -- Name: _HouseToUser_AB_unique; Type: INDEX; Schema: service$stage; Owner: -
    --
    
    CREATE UNIQUE INDEX "_HouseToUser_AB_unique" ON "service$stage"."_HouseToUser" USING btree ("A", "B");
    
    
    --
    -- Name: _HouseToUser_B; Type: INDEX; Schema: service$stage; Owner: -
    --
    
    CREATE INDEX "_HouseToUser_B" ON "service$stage"."_HouseToUser" USING btree ("B");
    
    
    --
    -- Name: User_scalarIntList User_scalarIntList_nodeId_fkey; Type: FK CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."User_scalarIntList"
        ADD CONSTRAINT "User_scalarIntList_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "service$stage"."User"(id);
    
    
    --
    -- Name: User_scalarStringList User_scalarStringList_nodeId_fkey; Type: FK CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."User_scalarStringList"
        ADD CONSTRAINT "User_scalarStringList_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "service$stage"."User"(id);
    
    
    --
    -- Name: _CarToUser _CarToUser_A_fkey; Type: FK CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."_CarToUser"
        ADD CONSTRAINT "_CarToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "service$stage"."Car"(id) ON DELETE CASCADE;
    
    
    --
    -- Name: _CarToUser _CarToUser_B_fkey; Type: FK CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."_CarToUser"
        ADD CONSTRAINT "_CarToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "service$stage"."User"(id) ON DELETE CASCADE;
    
    
    --
    -- Name: _HouseToUser _HouseToUser_A_fkey; Type: FK CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."_HouseToUser"
        ADD CONSTRAINT "_HouseToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "service$stage"."House"(id) ON DELETE CASCADE;
    
    
    --
    -- Name: _HouseToUser _HouseToUser_B_fkey; Type: FK CONSTRAINT; Schema: service$stage; Owner: -
    --
    
    ALTER TABLE ONLY "service$stage"."_HouseToUser"
        ADD CONSTRAINT "_HouseToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "service$stage"."User"(id) ON DELETE CASCADE;
    
    
    --
    -- PostgreSQL database dump complete
    --
    `)
  })
})
