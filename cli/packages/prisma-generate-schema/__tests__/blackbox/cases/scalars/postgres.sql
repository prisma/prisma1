--
-- PostgreSQL database dump
--

-- Dumped from database version 10.1
-- Dumped by pg_dump version 11.1

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
-- Name: schema-generator$scalars; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$scalars";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: LotsOfRequiredScalars; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfRequiredScalars" (
    id character varying(25) NOT NULL,
    "int" integer NOT NULL,
    string text NOT NULL,
    "float" numeric(65,30) NOT NULL,
    "dateTime" timestamp(3) without time zone NOT NULL,
    json text NOT NULL,
    "boolean" boolean NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: LotsOfRequiredScalarsWithID; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfRequiredScalarsWithID" (
    id character varying(25) NOT NULL,
    "int" integer NOT NULL,
    string text NOT NULL,
    "float" numeric(65,30) NOT NULL,
    "dateTime" timestamp(3) without time zone NOT NULL,
    json text NOT NULL,
    "boolean" boolean NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: LotsOfScalarLists; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarLists" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: LotsOfScalarListsWithID; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarListsWithID" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: LotsOfScalarListsWithID_boolean; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarListsWithID_boolean" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value boolean NOT NULL
);


--
-- Name: LotsOfScalarListsWithID_dateTime; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarListsWithID_dateTime" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value timestamp(3) without time zone NOT NULL
);


--
-- Name: LotsOfScalarListsWithID_float; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarListsWithID_float" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value numeric(65,30) NOT NULL
);


--
-- Name: LotsOfScalarListsWithID_int; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarListsWithID_int" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value integer NOT NULL
);


--
-- Name: LotsOfScalarListsWithID_json; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarListsWithID_json" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: LotsOfScalarListsWithID_string; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarListsWithID_string" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: LotsOfScalarLists_boolean; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarLists_boolean" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value boolean NOT NULL
);


--
-- Name: LotsOfScalarLists_dateTime; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarLists_dateTime" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value timestamp(3) without time zone NOT NULL
);


--
-- Name: LotsOfScalarLists_float; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarLists_float" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value numeric(65,30) NOT NULL
);


--
-- Name: LotsOfScalarLists_int; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarLists_int" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value integer NOT NULL
);


--
-- Name: LotsOfScalarLists_json; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarLists_json" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: LotsOfScalarLists_string; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarLists_string" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: LotsOfScalars; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalars" (
    id character varying(25) NOT NULL,
    "int" integer,
    string text,
    "float" numeric(65,30),
    "dateTime" timestamp(3) without time zone,
    json text,
    "boolean" boolean,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: LotsOfScalarsWithID; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."LotsOfScalarsWithID" (
    id character varying(25) NOT NULL,
    "int" integer,
    string text,
    "float" numeric(65,30),
    "dateTime" timestamp(3) without time zone,
    json text,
    "boolean" boolean,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$scalars; Owner: -
--

CREATE TABLE "schema-generator$scalars"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: LotsOfRequiredScalarsWithID LotsOfRequiredScalarsWithID_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfRequiredScalarsWithID"
    ADD CONSTRAINT "LotsOfRequiredScalarsWithID_pkey" PRIMARY KEY (id);


--
-- Name: LotsOfRequiredScalars LotsOfRequiredScalars_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfRequiredScalars"
    ADD CONSTRAINT "LotsOfRequiredScalars_pkey" PRIMARY KEY (id);


--
-- Name: LotsOfScalarListsWithID_boolean LotsOfScalarListsWithID_boolean_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_boolean"
    ADD CONSTRAINT "LotsOfScalarListsWithID_boolean_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarListsWithID_dateTime LotsOfScalarListsWithID_dateTime_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_dateTime"
    ADD CONSTRAINT "LotsOfScalarListsWithID_dateTime_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarListsWithID_float LotsOfScalarListsWithID_float_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_float"
    ADD CONSTRAINT "LotsOfScalarListsWithID_float_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarListsWithID_int LotsOfScalarListsWithID_int_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_int"
    ADD CONSTRAINT "LotsOfScalarListsWithID_int_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarListsWithID_json LotsOfScalarListsWithID_json_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_json"
    ADD CONSTRAINT "LotsOfScalarListsWithID_json_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarListsWithID LotsOfScalarListsWithID_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID"
    ADD CONSTRAINT "LotsOfScalarListsWithID_pkey" PRIMARY KEY (id);


--
-- Name: LotsOfScalarListsWithID_string LotsOfScalarListsWithID_string_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_string"
    ADD CONSTRAINT "LotsOfScalarListsWithID_string_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarLists_boolean LotsOfScalarLists_boolean_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_boolean"
    ADD CONSTRAINT "LotsOfScalarLists_boolean_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarLists_dateTime LotsOfScalarLists_dateTime_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_dateTime"
    ADD CONSTRAINT "LotsOfScalarLists_dateTime_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarLists_float LotsOfScalarLists_float_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_float"
    ADD CONSTRAINT "LotsOfScalarLists_float_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarLists_int LotsOfScalarLists_int_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_int"
    ADD CONSTRAINT "LotsOfScalarLists_int_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarLists_json LotsOfScalarLists_json_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_json"
    ADD CONSTRAINT "LotsOfScalarLists_json_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarLists LotsOfScalarLists_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists"
    ADD CONSTRAINT "LotsOfScalarLists_pkey" PRIMARY KEY (id);


--
-- Name: LotsOfScalarLists_string LotsOfScalarLists_string_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_string"
    ADD CONSTRAINT "LotsOfScalarLists_string_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: LotsOfScalarsWithID LotsOfScalarsWithID_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarsWithID"
    ADD CONSTRAINT "LotsOfScalarsWithID_pkey" PRIMARY KEY (id);


--
-- Name: LotsOfScalars LotsOfScalars_pkey; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalars"
    ADD CONSTRAINT "LotsOfScalars_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: LotsOfScalarListsWithID_boolean LotsOfScalarListsWithID_boolean_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_boolean"
    ADD CONSTRAINT "LotsOfScalarListsWithID_boolean_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarListsWithID"(id);


--
-- Name: LotsOfScalarListsWithID_dateTime LotsOfScalarListsWithID_dateTime_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_dateTime"
    ADD CONSTRAINT "LotsOfScalarListsWithID_dateTime_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarListsWithID"(id);


--
-- Name: LotsOfScalarListsWithID_float LotsOfScalarListsWithID_float_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_float"
    ADD CONSTRAINT "LotsOfScalarListsWithID_float_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarListsWithID"(id);


--
-- Name: LotsOfScalarListsWithID_int LotsOfScalarListsWithID_int_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_int"
    ADD CONSTRAINT "LotsOfScalarListsWithID_int_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarListsWithID"(id);


--
-- Name: LotsOfScalarListsWithID_json LotsOfScalarListsWithID_json_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_json"
    ADD CONSTRAINT "LotsOfScalarListsWithID_json_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarListsWithID"(id);


--
-- Name: LotsOfScalarListsWithID_string LotsOfScalarListsWithID_string_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarListsWithID_string"
    ADD CONSTRAINT "LotsOfScalarListsWithID_string_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarListsWithID"(id);


--
-- Name: LotsOfScalarLists_boolean LotsOfScalarLists_boolean_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_boolean"
    ADD CONSTRAINT "LotsOfScalarLists_boolean_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarLists"(id);


--
-- Name: LotsOfScalarLists_dateTime LotsOfScalarLists_dateTime_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_dateTime"
    ADD CONSTRAINT "LotsOfScalarLists_dateTime_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarLists"(id);


--
-- Name: LotsOfScalarLists_float LotsOfScalarLists_float_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_float"
    ADD CONSTRAINT "LotsOfScalarLists_float_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarLists"(id);


--
-- Name: LotsOfScalarLists_int LotsOfScalarLists_int_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_int"
    ADD CONSTRAINT "LotsOfScalarLists_int_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarLists"(id);


--
-- Name: LotsOfScalarLists_json LotsOfScalarLists_json_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_json"
    ADD CONSTRAINT "LotsOfScalarLists_json_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarLists"(id);


--
-- Name: LotsOfScalarLists_string LotsOfScalarLists_string_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$scalars; Owner: -
--

ALTER TABLE ONLY "schema-generator$scalars"."LotsOfScalarLists_string"
    ADD CONSTRAINT "LotsOfScalarLists_string_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$scalars"."LotsOfScalarLists"(id);


--
-- PostgreSQL database dump complete
--

