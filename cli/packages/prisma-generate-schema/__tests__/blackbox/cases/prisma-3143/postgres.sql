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
-- Name: schema-generator$prisma-3143; Type: SCHEMA; Schema: -; Owner: prisma
--

CREATE SCHEMA "schema-generator$prisma-3143";


ALTER SCHEMA "schema-generator$prisma-3143" OWNER TO prisma;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Story; Type: TABLE; Schema: schema-generator$prisma-3143; Owner: prisma
--

CREATE TABLE "schema-generator$prisma-3143"."Story" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$prisma-3143"."Story" OWNER TO prisma;

--
-- Name: UserSpace; Type: TABLE; Schema: schema-generator$prisma-3143; Owner: prisma
--

CREATE TABLE "schema-generator$prisma-3143"."UserSpace" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$prisma-3143"."UserSpace" OWNER TO prisma;

--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$prisma-3143; Owner: prisma
--

CREATE TABLE "schema-generator$prisma-3143"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$prisma-3143"."_RelayId" OWNER TO prisma;

--
-- Name: _StoriesByUserSpace; Type: TABLE; Schema: schema-generator$prisma-3143; Owner: prisma
--

CREATE TABLE "schema-generator$prisma-3143"."_StoriesByUserSpace" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$prisma-3143"."_StoriesByUserSpace" OWNER TO prisma;

--
-- Data for Name: Story; Type: TABLE DATA; Schema: schema-generator$prisma-3143; Owner: prisma
--

COPY "schema-generator$prisma-3143"."Story" (id, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: UserSpace; Type: TABLE DATA; Schema: schema-generator$prisma-3143; Owner: prisma
--

COPY "schema-generator$prisma-3143"."UserSpace" (id, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: _RelayId; Type: TABLE DATA; Schema: schema-generator$prisma-3143; Owner: prisma
--

COPY "schema-generator$prisma-3143"."_RelayId" (id, "stableModelIdentifier") FROM stdin;
\.


--
-- Data for Name: _StoriesByUserSpace; Type: TABLE DATA; Schema: schema-generator$prisma-3143; Owner: prisma
--

COPY "schema-generator$prisma-3143"."_StoriesByUserSpace" (id, "A", "B") FROM stdin;
\.


--
-- Name: Story Story_pkey; Type: CONSTRAINT; Schema: schema-generator$prisma-3143; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$prisma-3143"."Story"
    ADD CONSTRAINT "Story_pkey" PRIMARY KEY (id);


--
-- Name: UserSpace UserSpace_pkey; Type: CONSTRAINT; Schema: schema-generator$prisma-3143; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$prisma-3143"."UserSpace"
    ADD CONSTRAINT "UserSpace_pkey" PRIMARY KEY (id);


--
-- Name: _StoriesByUserSpace _StoriesByUserSpace_pkey; Type: CONSTRAINT; Schema: schema-generator$prisma-3143; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$prisma-3143"."_StoriesByUserSpace"
    ADD CONSTRAINT "_StoriesByUserSpace_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$prisma-3143; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$prisma-3143"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _StoriesByUserSpace_A; Type: INDEX; Schema: schema-generator$prisma-3143; Owner: prisma
--

CREATE INDEX "_StoriesByUserSpace_A" ON "schema-generator$prisma-3143"."_StoriesByUserSpace" USING btree ("A");


--
-- Name: _StoriesByUserSpace_AB_unique; Type: INDEX; Schema: schema-generator$prisma-3143; Owner: prisma
--

CREATE UNIQUE INDEX "_StoriesByUserSpace_AB_unique" ON "schema-generator$prisma-3143"."_StoriesByUserSpace" USING btree ("A", "B");


--
-- Name: _StoriesByUserSpace_B; Type: INDEX; Schema: schema-generator$prisma-3143; Owner: prisma
--

CREATE INDEX "_StoriesByUserSpace_B" ON "schema-generator$prisma-3143"."_StoriesByUserSpace" USING btree ("B");


--
-- Name: _StoriesByUserSpace _StoriesByUserSpace_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$prisma-3143; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$prisma-3143"."_StoriesByUserSpace"
    ADD CONSTRAINT "_StoriesByUserSpace_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$prisma-3143"."Story"(id) ON DELETE CASCADE;


--
-- Name: _StoriesByUserSpace _StoriesByUserSpace_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$prisma-3143; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$prisma-3143"."_StoriesByUserSpace"
    ADD CONSTRAINT "_StoriesByUserSpace_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$prisma-3143"."UserSpace"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

