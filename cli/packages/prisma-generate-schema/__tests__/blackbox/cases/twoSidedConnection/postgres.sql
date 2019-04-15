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
-- Name: schema-generator$twoSidedConnection; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$twoSidedConnection";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: AWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithA" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithB" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithC" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithIdWithA" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithIdWithB" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithIdWithC" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithoutIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithoutIdWithA" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithoutIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithoutIdWithB" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithoutIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."AWithoutIdWithC" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithA" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithB" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithC" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithIdWithA" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithIdWithB" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithIdWithC" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithoutIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithoutIdWithA" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithoutIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithoutIdWithB" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BWithoutIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."BWithoutIdWithC" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithA" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithB" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithC" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithIdWithA" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithIdWithB" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithIdWithC" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithoutIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithoutIdWithA" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithoutIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithoutIdWithB" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CWithoutIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."CWithoutIdWithC" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _AWithAToAWithIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithAToAWithIdWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithAToAWithoutIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithAToAWithoutIdWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithBToBWithIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithBToBWithIdWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithBToBWithoutIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithBToBWithoutIdWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithCToCWithIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithCToCWithIdWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithCToCWithoutIdWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithCToCWithoutIdWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithIdWithBToBWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithIdWithBToBWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithIdWithCToCWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithIdWithCToCWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithoutIdWithBToBWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithoutIdWithBToBWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithoutIdWithCToCWithA; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_AWithoutIdWithCToCWithA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BWithBToBWithIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_BWithBToBWithIdWithB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BWithBToBWithoutIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_BWithBToBWithoutIdWithB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BWithCToCWithIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_BWithCToCWithIdWithB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BWithCToCWithoutIdWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_BWithCToCWithoutIdWithB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BWithIdWithCToCWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_BWithIdWithCToCWithB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BWithoutIdWithCToCWithB; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_BWithoutIdWithCToCWithB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CWithCToCWithIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_CWithCToCWithIdWithC" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CWithCToCWithoutIdWithC; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_CWithCToCWithoutIdWithC" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$twoSidedConnection"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: AWithA AWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithA"
    ADD CONSTRAINT "AWithA_pkey" PRIMARY KEY (id);


--
-- Name: AWithB AWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithB"
    ADD CONSTRAINT "AWithB_pkey" PRIMARY KEY (id);


--
-- Name: AWithC AWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithC"
    ADD CONSTRAINT "AWithC_pkey" PRIMARY KEY (id);


--
-- Name: AWithIdWithA AWithIdWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithIdWithA"
    ADD CONSTRAINT "AWithIdWithA_pkey" PRIMARY KEY (id);


--
-- Name: AWithIdWithB AWithIdWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithIdWithB"
    ADD CONSTRAINT "AWithIdWithB_pkey" PRIMARY KEY (id);


--
-- Name: AWithIdWithC AWithIdWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithIdWithC"
    ADD CONSTRAINT "AWithIdWithC_pkey" PRIMARY KEY (id);


--
-- Name: AWithoutIdWithA AWithoutIdWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithoutIdWithA"
    ADD CONSTRAINT "AWithoutIdWithA_pkey" PRIMARY KEY (id);


--
-- Name: AWithoutIdWithB AWithoutIdWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithoutIdWithB"
    ADD CONSTRAINT "AWithoutIdWithB_pkey" PRIMARY KEY (id);


--
-- Name: AWithoutIdWithC AWithoutIdWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."AWithoutIdWithC"
    ADD CONSTRAINT "AWithoutIdWithC_pkey" PRIMARY KEY (id);


--
-- Name: BWithA BWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithA"
    ADD CONSTRAINT "BWithA_pkey" PRIMARY KEY (id);


--
-- Name: BWithB BWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithB"
    ADD CONSTRAINT "BWithB_pkey" PRIMARY KEY (id);


--
-- Name: BWithC BWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithC"
    ADD CONSTRAINT "BWithC_pkey" PRIMARY KEY (id);


--
-- Name: BWithIdWithA BWithIdWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithIdWithA"
    ADD CONSTRAINT "BWithIdWithA_pkey" PRIMARY KEY (id);


--
-- Name: BWithIdWithB BWithIdWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithIdWithB"
    ADD CONSTRAINT "BWithIdWithB_pkey" PRIMARY KEY (id);


--
-- Name: BWithIdWithC BWithIdWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithIdWithC"
    ADD CONSTRAINT "BWithIdWithC_pkey" PRIMARY KEY (id);


--
-- Name: BWithoutIdWithA BWithoutIdWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithoutIdWithA"
    ADD CONSTRAINT "BWithoutIdWithA_pkey" PRIMARY KEY (id);


--
-- Name: BWithoutIdWithB BWithoutIdWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithoutIdWithB"
    ADD CONSTRAINT "BWithoutIdWithB_pkey" PRIMARY KEY (id);


--
-- Name: BWithoutIdWithC BWithoutIdWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."BWithoutIdWithC"
    ADD CONSTRAINT "BWithoutIdWithC_pkey" PRIMARY KEY (id);


--
-- Name: CWithA CWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithA"
    ADD CONSTRAINT "CWithA_pkey" PRIMARY KEY (id);


--
-- Name: CWithB CWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithB"
    ADD CONSTRAINT "CWithB_pkey" PRIMARY KEY (id);


--
-- Name: CWithC CWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithC"
    ADD CONSTRAINT "CWithC_pkey" PRIMARY KEY (id);


--
-- Name: CWithIdWithA CWithIdWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithIdWithA"
    ADD CONSTRAINT "CWithIdWithA_pkey" PRIMARY KEY (id);


--
-- Name: CWithIdWithB CWithIdWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithIdWithB"
    ADD CONSTRAINT "CWithIdWithB_pkey" PRIMARY KEY (id);


--
-- Name: CWithIdWithC CWithIdWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithIdWithC"
    ADD CONSTRAINT "CWithIdWithC_pkey" PRIMARY KEY (id);


--
-- Name: CWithoutIdWithA CWithoutIdWithA_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithoutIdWithA"
    ADD CONSTRAINT "CWithoutIdWithA_pkey" PRIMARY KEY (id);


--
-- Name: CWithoutIdWithB CWithoutIdWithB_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithoutIdWithB"
    ADD CONSTRAINT "CWithoutIdWithB_pkey" PRIMARY KEY (id);


--
-- Name: CWithoutIdWithC CWithoutIdWithC_pkey; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."CWithoutIdWithC"
    ADD CONSTRAINT "CWithoutIdWithC_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _AWithAToAWithIdWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithAToAWithIdWithA_A" ON "schema-generator$twoSidedConnection"."_AWithAToAWithIdWithA" USING btree ("A");


--
-- Name: _AWithAToAWithIdWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithAToAWithIdWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithAToAWithIdWithA" USING btree ("A", "B");


--
-- Name: _AWithAToAWithIdWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithAToAWithIdWithA_B" ON "schema-generator$twoSidedConnection"."_AWithAToAWithIdWithA" USING btree ("B");


--
-- Name: _AWithAToAWithoutIdWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithAToAWithoutIdWithA_A" ON "schema-generator$twoSidedConnection"."_AWithAToAWithoutIdWithA" USING btree ("A");


--
-- Name: _AWithAToAWithoutIdWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithAToAWithoutIdWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithAToAWithoutIdWithA" USING btree ("A", "B");


--
-- Name: _AWithAToAWithoutIdWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithAToAWithoutIdWithA_B" ON "schema-generator$twoSidedConnection"."_AWithAToAWithoutIdWithA" USING btree ("B");


--
-- Name: _AWithBToBWithIdWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithBToBWithIdWithA_A" ON "schema-generator$twoSidedConnection"."_AWithBToBWithIdWithA" USING btree ("A");


--
-- Name: _AWithBToBWithIdWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithBToBWithIdWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithBToBWithIdWithA" USING btree ("A", "B");


--
-- Name: _AWithBToBWithIdWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithBToBWithIdWithA_B" ON "schema-generator$twoSidedConnection"."_AWithBToBWithIdWithA" USING btree ("B");


--
-- Name: _AWithBToBWithoutIdWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithBToBWithoutIdWithA_A" ON "schema-generator$twoSidedConnection"."_AWithBToBWithoutIdWithA" USING btree ("A");


--
-- Name: _AWithBToBWithoutIdWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithBToBWithoutIdWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithBToBWithoutIdWithA" USING btree ("A", "B");


--
-- Name: _AWithBToBWithoutIdWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithBToBWithoutIdWithA_B" ON "schema-generator$twoSidedConnection"."_AWithBToBWithoutIdWithA" USING btree ("B");


--
-- Name: _AWithCToCWithIdWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithCToCWithIdWithA_A" ON "schema-generator$twoSidedConnection"."_AWithCToCWithIdWithA" USING btree ("A");


--
-- Name: _AWithCToCWithIdWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithCToCWithIdWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithCToCWithIdWithA" USING btree ("A", "B");


--
-- Name: _AWithCToCWithIdWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithCToCWithIdWithA_B" ON "schema-generator$twoSidedConnection"."_AWithCToCWithIdWithA" USING btree ("B");


--
-- Name: _AWithCToCWithoutIdWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithCToCWithoutIdWithA_A" ON "schema-generator$twoSidedConnection"."_AWithCToCWithoutIdWithA" USING btree ("A");


--
-- Name: _AWithCToCWithoutIdWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithCToCWithoutIdWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithCToCWithoutIdWithA" USING btree ("A", "B");


--
-- Name: _AWithCToCWithoutIdWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithCToCWithoutIdWithA_B" ON "schema-generator$twoSidedConnection"."_AWithCToCWithoutIdWithA" USING btree ("B");


--
-- Name: _AWithIdWithBToBWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithIdWithBToBWithA_A" ON "schema-generator$twoSidedConnection"."_AWithIdWithBToBWithA" USING btree ("A");


--
-- Name: _AWithIdWithBToBWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithIdWithBToBWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithIdWithBToBWithA" USING btree ("A", "B");


--
-- Name: _AWithIdWithBToBWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithIdWithBToBWithA_B" ON "schema-generator$twoSidedConnection"."_AWithIdWithBToBWithA" USING btree ("B");


--
-- Name: _AWithIdWithCToCWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithIdWithCToCWithA_A" ON "schema-generator$twoSidedConnection"."_AWithIdWithCToCWithA" USING btree ("A");


--
-- Name: _AWithIdWithCToCWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithIdWithCToCWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithIdWithCToCWithA" USING btree ("A", "B");


--
-- Name: _AWithIdWithCToCWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithIdWithCToCWithA_B" ON "schema-generator$twoSidedConnection"."_AWithIdWithCToCWithA" USING btree ("B");


--
-- Name: _AWithoutIdWithBToBWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithoutIdWithBToBWithA_A" ON "schema-generator$twoSidedConnection"."_AWithoutIdWithBToBWithA" USING btree ("A");


--
-- Name: _AWithoutIdWithBToBWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithoutIdWithBToBWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithoutIdWithBToBWithA" USING btree ("A", "B");


--
-- Name: _AWithoutIdWithBToBWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithoutIdWithBToBWithA_B" ON "schema-generator$twoSidedConnection"."_AWithoutIdWithBToBWithA" USING btree ("B");


--
-- Name: _AWithoutIdWithCToCWithA_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithoutIdWithCToCWithA_A" ON "schema-generator$twoSidedConnection"."_AWithoutIdWithCToCWithA" USING btree ("A");


--
-- Name: _AWithoutIdWithCToCWithA_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AWithoutIdWithCToCWithA_AB_unique" ON "schema-generator$twoSidedConnection"."_AWithoutIdWithCToCWithA" USING btree ("A", "B");


--
-- Name: _AWithoutIdWithCToCWithA_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_AWithoutIdWithCToCWithA_B" ON "schema-generator$twoSidedConnection"."_AWithoutIdWithCToCWithA" USING btree ("B");


--
-- Name: _BWithBToBWithIdWithB_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithBToBWithIdWithB_A" ON "schema-generator$twoSidedConnection"."_BWithBToBWithIdWithB" USING btree ("A");


--
-- Name: _BWithBToBWithIdWithB_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BWithBToBWithIdWithB_AB_unique" ON "schema-generator$twoSidedConnection"."_BWithBToBWithIdWithB" USING btree ("A", "B");


--
-- Name: _BWithBToBWithIdWithB_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithBToBWithIdWithB_B" ON "schema-generator$twoSidedConnection"."_BWithBToBWithIdWithB" USING btree ("B");


--
-- Name: _BWithBToBWithoutIdWithB_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithBToBWithoutIdWithB_A" ON "schema-generator$twoSidedConnection"."_BWithBToBWithoutIdWithB" USING btree ("A");


--
-- Name: _BWithBToBWithoutIdWithB_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BWithBToBWithoutIdWithB_AB_unique" ON "schema-generator$twoSidedConnection"."_BWithBToBWithoutIdWithB" USING btree ("A", "B");


--
-- Name: _BWithBToBWithoutIdWithB_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithBToBWithoutIdWithB_B" ON "schema-generator$twoSidedConnection"."_BWithBToBWithoutIdWithB" USING btree ("B");


--
-- Name: _BWithCToCWithIdWithB_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithCToCWithIdWithB_A" ON "schema-generator$twoSidedConnection"."_BWithCToCWithIdWithB" USING btree ("A");


--
-- Name: _BWithCToCWithIdWithB_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BWithCToCWithIdWithB_AB_unique" ON "schema-generator$twoSidedConnection"."_BWithCToCWithIdWithB" USING btree ("A", "B");


--
-- Name: _BWithCToCWithIdWithB_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithCToCWithIdWithB_B" ON "schema-generator$twoSidedConnection"."_BWithCToCWithIdWithB" USING btree ("B");


--
-- Name: _BWithCToCWithoutIdWithB_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithCToCWithoutIdWithB_A" ON "schema-generator$twoSidedConnection"."_BWithCToCWithoutIdWithB" USING btree ("A");


--
-- Name: _BWithCToCWithoutIdWithB_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BWithCToCWithoutIdWithB_AB_unique" ON "schema-generator$twoSidedConnection"."_BWithCToCWithoutIdWithB" USING btree ("A", "B");


--
-- Name: _BWithCToCWithoutIdWithB_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithCToCWithoutIdWithB_B" ON "schema-generator$twoSidedConnection"."_BWithCToCWithoutIdWithB" USING btree ("B");


--
-- Name: _BWithIdWithCToCWithB_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithIdWithCToCWithB_A" ON "schema-generator$twoSidedConnection"."_BWithIdWithCToCWithB" USING btree ("A");


--
-- Name: _BWithIdWithCToCWithB_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BWithIdWithCToCWithB_AB_unique" ON "schema-generator$twoSidedConnection"."_BWithIdWithCToCWithB" USING btree ("A", "B");


--
-- Name: _BWithIdWithCToCWithB_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithIdWithCToCWithB_B" ON "schema-generator$twoSidedConnection"."_BWithIdWithCToCWithB" USING btree ("B");


--
-- Name: _BWithoutIdWithCToCWithB_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithoutIdWithCToCWithB_A" ON "schema-generator$twoSidedConnection"."_BWithoutIdWithCToCWithB" USING btree ("A");


--
-- Name: _BWithoutIdWithCToCWithB_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BWithoutIdWithCToCWithB_AB_unique" ON "schema-generator$twoSidedConnection"."_BWithoutIdWithCToCWithB" USING btree ("A", "B");


--
-- Name: _BWithoutIdWithCToCWithB_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_BWithoutIdWithCToCWithB_B" ON "schema-generator$twoSidedConnection"."_BWithoutIdWithCToCWithB" USING btree ("B");


--
-- Name: _CWithCToCWithIdWithC_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_CWithCToCWithIdWithC_A" ON "schema-generator$twoSidedConnection"."_CWithCToCWithIdWithC" USING btree ("A");


--
-- Name: _CWithCToCWithIdWithC_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_CWithCToCWithIdWithC_AB_unique" ON "schema-generator$twoSidedConnection"."_CWithCToCWithIdWithC" USING btree ("A", "B");


--
-- Name: _CWithCToCWithIdWithC_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_CWithCToCWithIdWithC_B" ON "schema-generator$twoSidedConnection"."_CWithCToCWithIdWithC" USING btree ("B");


--
-- Name: _CWithCToCWithoutIdWithC_A; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_CWithCToCWithoutIdWithC_A" ON "schema-generator$twoSidedConnection"."_CWithCToCWithoutIdWithC" USING btree ("A");


--
-- Name: _CWithCToCWithoutIdWithC_AB_unique; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_CWithCToCWithoutIdWithC_AB_unique" ON "schema-generator$twoSidedConnection"."_CWithCToCWithoutIdWithC" USING btree ("A", "B");


--
-- Name: _CWithCToCWithoutIdWithC_B; Type: INDEX; Schema: schema-generator$twoSidedConnection; Owner: -
--

CREATE INDEX "_CWithCToCWithoutIdWithC_B" ON "schema-generator$twoSidedConnection"."_CWithCToCWithoutIdWithC" USING btree ("B");


--
-- Name: _AWithAToAWithIdWithA _AWithAToAWithIdWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithAToAWithIdWithA"
    ADD CONSTRAINT "_AWithAToAWithIdWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithAToAWithIdWithA _AWithAToAWithIdWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithAToAWithIdWithA"
    ADD CONSTRAINT "_AWithAToAWithIdWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."AWithIdWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithAToAWithoutIdWithA _AWithAToAWithoutIdWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithAToAWithoutIdWithA"
    ADD CONSTRAINT "_AWithAToAWithoutIdWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithAToAWithoutIdWithA _AWithAToAWithoutIdWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithAToAWithoutIdWithA"
    ADD CONSTRAINT "_AWithAToAWithoutIdWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."AWithoutIdWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithBToBWithIdWithA _AWithBToBWithIdWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithBToBWithIdWithA"
    ADD CONSTRAINT "_AWithBToBWithIdWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithB"(id) ON DELETE CASCADE;


--
-- Name: _AWithBToBWithIdWithA _AWithBToBWithIdWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithBToBWithIdWithA"
    ADD CONSTRAINT "_AWithBToBWithIdWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."BWithIdWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithBToBWithoutIdWithA _AWithBToBWithoutIdWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithBToBWithoutIdWithA"
    ADD CONSTRAINT "_AWithBToBWithoutIdWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithB"(id) ON DELETE CASCADE;


--
-- Name: _AWithBToBWithoutIdWithA _AWithBToBWithoutIdWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithBToBWithoutIdWithA"
    ADD CONSTRAINT "_AWithBToBWithoutIdWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."BWithoutIdWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithCToCWithIdWithA _AWithCToCWithIdWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithCToCWithIdWithA"
    ADD CONSTRAINT "_AWithCToCWithIdWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithC"(id) ON DELETE CASCADE;


--
-- Name: _AWithCToCWithIdWithA _AWithCToCWithIdWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithCToCWithIdWithA"
    ADD CONSTRAINT "_AWithCToCWithIdWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithIdWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithCToCWithoutIdWithA _AWithCToCWithoutIdWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithCToCWithoutIdWithA"
    ADD CONSTRAINT "_AWithCToCWithoutIdWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithC"(id) ON DELETE CASCADE;


--
-- Name: _AWithCToCWithoutIdWithA _AWithCToCWithoutIdWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithCToCWithoutIdWithA"
    ADD CONSTRAINT "_AWithCToCWithoutIdWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithoutIdWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdWithBToBWithA _AWithIdWithBToBWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithIdWithBToBWithA"
    ADD CONSTRAINT "_AWithIdWithBToBWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithIdWithB"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdWithBToBWithA _AWithIdWithBToBWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithIdWithBToBWithA"
    ADD CONSTRAINT "_AWithIdWithBToBWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."BWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdWithCToCWithA _AWithIdWithCToCWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithIdWithCToCWithA"
    ADD CONSTRAINT "_AWithIdWithCToCWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithIdWithC"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdWithCToCWithA _AWithIdWithCToCWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithIdWithCToCWithA"
    ADD CONSTRAINT "_AWithIdWithCToCWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithoutIdWithBToBWithA _AWithoutIdWithBToBWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithoutIdWithBToBWithA"
    ADD CONSTRAINT "_AWithoutIdWithBToBWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithoutIdWithB"(id) ON DELETE CASCADE;


--
-- Name: _AWithoutIdWithBToBWithA _AWithoutIdWithBToBWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithoutIdWithBToBWithA"
    ADD CONSTRAINT "_AWithoutIdWithBToBWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."BWithA"(id) ON DELETE CASCADE;


--
-- Name: _AWithoutIdWithCToCWithA _AWithoutIdWithCToCWithA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithoutIdWithCToCWithA"
    ADD CONSTRAINT "_AWithoutIdWithCToCWithA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."AWithoutIdWithC"(id) ON DELETE CASCADE;


--
-- Name: _AWithoutIdWithCToCWithA _AWithoutIdWithCToCWithA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_AWithoutIdWithCToCWithA"
    ADD CONSTRAINT "_AWithoutIdWithCToCWithA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithA"(id) ON DELETE CASCADE;


--
-- Name: _BWithBToBWithIdWithB _BWithBToBWithIdWithB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithBToBWithIdWithB"
    ADD CONSTRAINT "_BWithBToBWithIdWithB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."BWithB"(id) ON DELETE CASCADE;


--
-- Name: _BWithBToBWithIdWithB _BWithBToBWithIdWithB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithBToBWithIdWithB"
    ADD CONSTRAINT "_BWithBToBWithIdWithB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."BWithIdWithB"(id) ON DELETE CASCADE;


--
-- Name: _BWithBToBWithoutIdWithB _BWithBToBWithoutIdWithB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithBToBWithoutIdWithB"
    ADD CONSTRAINT "_BWithBToBWithoutIdWithB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."BWithB"(id) ON DELETE CASCADE;


--
-- Name: _BWithBToBWithoutIdWithB _BWithBToBWithoutIdWithB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithBToBWithoutIdWithB"
    ADD CONSTRAINT "_BWithBToBWithoutIdWithB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."BWithoutIdWithB"(id) ON DELETE CASCADE;


--
-- Name: _BWithCToCWithIdWithB _BWithCToCWithIdWithB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithCToCWithIdWithB"
    ADD CONSTRAINT "_BWithCToCWithIdWithB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."BWithC"(id) ON DELETE CASCADE;


--
-- Name: _BWithCToCWithIdWithB _BWithCToCWithIdWithB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithCToCWithIdWithB"
    ADD CONSTRAINT "_BWithCToCWithIdWithB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithIdWithB"(id) ON DELETE CASCADE;


--
-- Name: _BWithCToCWithoutIdWithB _BWithCToCWithoutIdWithB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithCToCWithoutIdWithB"
    ADD CONSTRAINT "_BWithCToCWithoutIdWithB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."BWithC"(id) ON DELETE CASCADE;


--
-- Name: _BWithCToCWithoutIdWithB _BWithCToCWithoutIdWithB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithCToCWithoutIdWithB"
    ADD CONSTRAINT "_BWithCToCWithoutIdWithB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithoutIdWithB"(id) ON DELETE CASCADE;


--
-- Name: _BWithIdWithCToCWithB _BWithIdWithCToCWithB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithIdWithCToCWithB"
    ADD CONSTRAINT "_BWithIdWithCToCWithB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."BWithIdWithC"(id) ON DELETE CASCADE;


--
-- Name: _BWithIdWithCToCWithB _BWithIdWithCToCWithB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithIdWithCToCWithB"
    ADD CONSTRAINT "_BWithIdWithCToCWithB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithB"(id) ON DELETE CASCADE;


--
-- Name: _BWithoutIdWithCToCWithB _BWithoutIdWithCToCWithB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithoutIdWithCToCWithB"
    ADD CONSTRAINT "_BWithoutIdWithCToCWithB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."BWithoutIdWithC"(id) ON DELETE CASCADE;


--
-- Name: _BWithoutIdWithCToCWithB _BWithoutIdWithCToCWithB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_BWithoutIdWithCToCWithB"
    ADD CONSTRAINT "_BWithoutIdWithCToCWithB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithB"(id) ON DELETE CASCADE;


--
-- Name: _CWithCToCWithIdWithC _CWithCToCWithIdWithC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_CWithCToCWithIdWithC"
    ADD CONSTRAINT "_CWithCToCWithIdWithC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."CWithC"(id) ON DELETE CASCADE;


--
-- Name: _CWithCToCWithIdWithC _CWithCToCWithIdWithC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_CWithCToCWithIdWithC"
    ADD CONSTRAINT "_CWithCToCWithIdWithC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithIdWithC"(id) ON DELETE CASCADE;


--
-- Name: _CWithCToCWithoutIdWithC _CWithCToCWithoutIdWithC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_CWithCToCWithoutIdWithC"
    ADD CONSTRAINT "_CWithCToCWithoutIdWithC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$twoSidedConnection"."CWithC"(id) ON DELETE CASCADE;


--
-- Name: _CWithCToCWithoutIdWithC _CWithCToCWithoutIdWithC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$twoSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$twoSidedConnection"."_CWithCToCWithoutIdWithC"
    ADD CONSTRAINT "_CWithCToCWithoutIdWithC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$twoSidedConnection"."CWithoutIdWithC"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

