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
-- Name: schema-generator$relationNames; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$relationNames";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: A; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."A" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: B; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."B" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: C; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."C" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _AToC; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."_AToC" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BToC; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."_BToC" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RaToB; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."_RaToB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RaToB2; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."_RaToB2" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$relationNames; Owner: -
--

CREATE TABLE "schema-generator$relationNames"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: A A_pkey; Type: CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."A"
    ADD CONSTRAINT "A_pkey" PRIMARY KEY (id);


--
-- Name: B B_pkey; Type: CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."B"
    ADD CONSTRAINT "B_pkey" PRIMARY KEY (id);


--
-- Name: C C_pkey; Type: CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."C"
    ADD CONSTRAINT "C_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _AToC_A; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_AToC_A" ON "schema-generator$relationNames"."_AToC" USING btree ("A");


--
-- Name: _AToC_AB_unique; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE UNIQUE INDEX "_AToC_AB_unique" ON "schema-generator$relationNames"."_AToC" USING btree ("A", "B");


--
-- Name: _AToC_B; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_AToC_B" ON "schema-generator$relationNames"."_AToC" USING btree ("B");


--
-- Name: _BToC_A; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_BToC_A" ON "schema-generator$relationNames"."_BToC" USING btree ("A");


--
-- Name: _BToC_AB_unique; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE UNIQUE INDEX "_BToC_AB_unique" ON "schema-generator$relationNames"."_BToC" USING btree ("A", "B");


--
-- Name: _BToC_B; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_BToC_B" ON "schema-generator$relationNames"."_BToC" USING btree ("B");


--
-- Name: _RaToB2_A; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_RaToB2_A" ON "schema-generator$relationNames"."_RaToB2" USING btree ("A");


--
-- Name: _RaToB2_AB_unique; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE UNIQUE INDEX "_RaToB2_AB_unique" ON "schema-generator$relationNames"."_RaToB2" USING btree ("A", "B");


--
-- Name: _RaToB2_B; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_RaToB2_B" ON "schema-generator$relationNames"."_RaToB2" USING btree ("B");


--
-- Name: _RaToB_A; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_RaToB_A" ON "schema-generator$relationNames"."_RaToB" USING btree ("A");


--
-- Name: _RaToB_AB_unique; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE UNIQUE INDEX "_RaToB_AB_unique" ON "schema-generator$relationNames"."_RaToB" USING btree ("A", "B");


--
-- Name: _RaToB_B; Type: INDEX; Schema: schema-generator$relationNames; Owner: -
--

CREATE INDEX "_RaToB_B" ON "schema-generator$relationNames"."_RaToB" USING btree ("B");


--
-- Name: _AToC _AToC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_AToC"
    ADD CONSTRAINT "_AToC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relationNames"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToC _AToC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_AToC"
    ADD CONSTRAINT "_AToC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relationNames"."C"(id) ON DELETE CASCADE;


--
-- Name: _BToC _BToC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_BToC"
    ADD CONSTRAINT "_BToC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relationNames"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToC _BToC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_BToC"
    ADD CONSTRAINT "_BToC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relationNames"."C"(id) ON DELETE CASCADE;


--
-- Name: _RaToB2 _RaToB2_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_RaToB2"
    ADD CONSTRAINT "_RaToB2_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relationNames"."A"(id) ON DELETE CASCADE;


--
-- Name: _RaToB2 _RaToB2_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_RaToB2"
    ADD CONSTRAINT "_RaToB2_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relationNames"."B"(id) ON DELETE CASCADE;


--
-- Name: _RaToB _RaToB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_RaToB"
    ADD CONSTRAINT "_RaToB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relationNames"."A"(id) ON DELETE CASCADE;


--
-- Name: _RaToB _RaToB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relationNames; Owner: -
--

ALTER TABLE ONLY "schema-generator$relationNames"."_RaToB"
    ADD CONSTRAINT "_RaToB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relationNames"."B"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

