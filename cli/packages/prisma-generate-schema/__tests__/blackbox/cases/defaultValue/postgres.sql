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
-- Name: schema-generator$defaultValue; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$defaultValue";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: A; Type: TABLE; Schema: schema-generator$defaultValue; Owner: -
--

CREATE TABLE "schema-generator$defaultValue"."A" (
    id character varying(25) NOT NULL,
    a integer,
    b integer NOT NULL,
    c text NOT NULL,
    d text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithId; Type: TABLE; Schema: schema-generator$defaultValue; Owner: -
--

CREATE TABLE "schema-generator$defaultValue"."AWithId" (
    id character varying(25) NOT NULL,
    a integer,
    b integer NOT NULL,
    c text NOT NULL,
    d text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: B; Type: TABLE; Schema: schema-generator$defaultValue; Owner: -
--

CREATE TABLE "schema-generator$defaultValue"."B" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _AToB; Type: TABLE; Schema: schema-generator$defaultValue; Owner: -
--

CREATE TABLE "schema-generator$defaultValue"."_AToB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithIdToB; Type: TABLE; Schema: schema-generator$defaultValue; Owner: -
--

CREATE TABLE "schema-generator$defaultValue"."_AWithIdToB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$defaultValue; Owner: -
--

CREATE TABLE "schema-generator$defaultValue"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: AWithId AWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."AWithId"
    ADD CONSTRAINT "AWithId_pkey" PRIMARY KEY (id);


--
-- Name: A A_pkey; Type: CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."A"
    ADD CONSTRAINT "A_pkey" PRIMARY KEY (id);


--
-- Name: B B_pkey; Type: CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."B"
    ADD CONSTRAINT "B_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _AToB_A; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE INDEX "_AToB_A" ON "schema-generator$defaultValue"."_AToB" USING btree ("A");


--
-- Name: _AToB_AB_unique; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE UNIQUE INDEX "_AToB_AB_unique" ON "schema-generator$defaultValue"."_AToB" USING btree ("A", "B");


--
-- Name: _AToB_B; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE INDEX "_AToB_B" ON "schema-generator$defaultValue"."_AToB" USING btree ("B");


--
-- Name: _AWithIdToB_A; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE INDEX "_AWithIdToB_A" ON "schema-generator$defaultValue"."_AWithIdToB" USING btree ("A");


--
-- Name: _AWithIdToB_AB_unique; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE UNIQUE INDEX "_AWithIdToB_AB_unique" ON "schema-generator$defaultValue"."_AWithIdToB" USING btree ("A", "B");


--
-- Name: _AWithIdToB_B; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE INDEX "_AWithIdToB_B" ON "schema-generator$defaultValue"."_AWithIdToB" USING btree ("B");


--
-- Name: schema-generator$defaultValue.A.a._UNIQUE; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$defaultValue.A.a._UNIQUE" ON "schema-generator$defaultValue"."A" USING btree (a);


--
-- Name: schema-generator$defaultValue.A.c._UNIQUE; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$defaultValue.A.c._UNIQUE" ON "schema-generator$defaultValue"."A" USING btree (c);


--
-- Name: schema-generator$defaultValue.AWithId.a._UNIQUE; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$defaultValue.AWithId.a._UNIQUE" ON "schema-generator$defaultValue"."AWithId" USING btree (a);


--
-- Name: schema-generator$defaultValue.AWithId.c._UNIQUE; Type: INDEX; Schema: schema-generator$defaultValue; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$defaultValue.AWithId.c._UNIQUE" ON "schema-generator$defaultValue"."AWithId" USING btree (c);


--
-- Name: _AToB _AToB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."_AToB"
    ADD CONSTRAINT "_AToB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$defaultValue"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToB _AToB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."_AToB"
    ADD CONSTRAINT "_AToB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$defaultValue"."B"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdToB _AWithIdToB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."_AWithIdToB"
    ADD CONSTRAINT "_AWithIdToB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$defaultValue"."AWithId"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdToB _AWithIdToB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$defaultValue; Owner: -
--

ALTER TABLE ONLY "schema-generator$defaultValue"."_AWithIdToB"
    ADD CONSTRAINT "_AWithIdToB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$defaultValue"."B"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

