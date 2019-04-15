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
-- Name: schema-generator$withAndWithoutId; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$withAndWithoutId";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: TypeWithId; Type: TABLE; Schema: schema-generator$withAndWithoutId; Owner: -
--

CREATE TABLE "schema-generator$withAndWithoutId"."TypeWithId" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: TypeWithoutId; Type: TABLE; Schema: schema-generator$withAndWithoutId; Owner: -
--

CREATE TABLE "schema-generator$withAndWithoutId"."TypeWithoutId" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$withAndWithoutId; Owner: -
--

CREATE TABLE "schema-generator$withAndWithoutId"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: TypeWithId TypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$withAndWithoutId; Owner: -
--

ALTER TABLE ONLY "schema-generator$withAndWithoutId"."TypeWithId"
    ADD CONSTRAINT "TypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: TypeWithoutId TypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$withAndWithoutId; Owner: -
--

ALTER TABLE ONLY "schema-generator$withAndWithoutId"."TypeWithoutId"
    ADD CONSTRAINT "TypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$withAndWithoutId; Owner: -
--

ALTER TABLE ONLY "schema-generator$withAndWithoutId"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

