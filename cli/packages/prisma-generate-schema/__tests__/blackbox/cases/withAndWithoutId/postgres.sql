--
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
-- Name: schema-generator$withAndWithoutId; Type: SCHEMA; Schema: -; Owner: prisma
--

CREATE SCHEMA "schema-generator$withAndWithoutId";


ALTER SCHEMA "schema-generator$withAndWithoutId" OWNER TO prisma;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: TypeWithId; Type: TABLE; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

CREATE TABLE "schema-generator$withAndWithoutId"."TypeWithId" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$withAndWithoutId"."TypeWithId" OWNER TO prisma;

--
-- Name: TypeWithoutId; Type: TABLE; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

CREATE TABLE "schema-generator$withAndWithoutId"."TypeWithoutId" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$withAndWithoutId"."TypeWithoutId" OWNER TO prisma;

--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

CREATE TABLE "schema-generator$withAndWithoutId"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$withAndWithoutId"."_RelayId" OWNER TO prisma;

--
-- Data for Name: TypeWithId; Type: TABLE DATA; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

COPY "schema-generator$withAndWithoutId"."TypeWithId" (id, field, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: TypeWithoutId; Type: TABLE DATA; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

COPY "schema-generator$withAndWithoutId"."TypeWithoutId" (id, field, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: _RelayId; Type: TABLE DATA; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

COPY "schema-generator$withAndWithoutId"."_RelayId" (id, "stableModelIdentifier") FROM stdin;
\.


--
-- Name: TypeWithId TypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$withAndWithoutId"."TypeWithId"
    ADD CONSTRAINT "TypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: TypeWithoutId TypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$withAndWithoutId"."TypeWithoutId"
    ADD CONSTRAINT "TypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$withAndWithoutId; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$withAndWithoutId"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

