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
-- Name: schema-generator$enum; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$enum";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: A; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."A" (
    id character varying(25) NOT NULL,
    "fieldA" text,
    "fieldB" text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithId; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."AWithId" (
    id character varying(25) NOT NULL,
    "fieldA" text,
    "fieldB" text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: AWithId_fieldC; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."AWithId_fieldC" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: A_fieldC; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."A_fieldC" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: B; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."B" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: C; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."C" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: C_field; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."C_field" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value integer NOT NULL
);


--
-- Name: D; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."D" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: D_field; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."D_field" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value timestamp(3) without time zone NOT NULL
);


--
-- Name: E; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."E" (
    id character varying(25) NOT NULL,
    field text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _AToB; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."_AToB" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AToE; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."_AToE" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithIdToC; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."_AWithIdToC" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AWithIdToD; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."_AWithIdToD" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$enum; Owner: -
--

CREATE TABLE "schema-generator$enum"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: AWithId_fieldC AWithId_fieldC_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."AWithId_fieldC"
    ADD CONSTRAINT "AWithId_fieldC_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: AWithId AWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."AWithId"
    ADD CONSTRAINT "AWithId_pkey" PRIMARY KEY (id);


--
-- Name: A_fieldC A_fieldC_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."A_fieldC"
    ADD CONSTRAINT "A_fieldC_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: A A_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."A"
    ADD CONSTRAINT "A_pkey" PRIMARY KEY (id);


--
-- Name: B B_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."B"
    ADD CONSTRAINT "B_pkey" PRIMARY KEY (id);


--
-- Name: C_field C_field_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."C_field"
    ADD CONSTRAINT "C_field_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: C C_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."C"
    ADD CONSTRAINT "C_pkey" PRIMARY KEY (id);


--
-- Name: D_field D_field_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."D_field"
    ADD CONSTRAINT "D_field_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: D D_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."D"
    ADD CONSTRAINT "D_pkey" PRIMARY KEY (id);


--
-- Name: E E_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."E"
    ADD CONSTRAINT "E_pkey" PRIMARY KEY (id);


--
-- Name: _AToB _AToB_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AToB"
    ADD CONSTRAINT "_AToB_pkey" PRIMARY KEY (id);


--
-- Name: _AToE _AToE_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AToE"
    ADD CONSTRAINT "_AToE_pkey" PRIMARY KEY (id);


--
-- Name: _AWithIdToC _AWithIdToC_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AWithIdToC"
    ADD CONSTRAINT "_AWithIdToC_pkey" PRIMARY KEY (id);


--
-- Name: _AWithIdToD _AWithIdToD_pkey; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AWithIdToD"
    ADD CONSTRAINT "_AWithIdToD_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _AToB_A; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AToB_A" ON "schema-generator$enum"."_AToB" USING btree ("A");


--
-- Name: _AToB_AB_unique; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE UNIQUE INDEX "_AToB_AB_unique" ON "schema-generator$enum"."_AToB" USING btree ("A", "B");


--
-- Name: _AToB_B; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AToB_B" ON "schema-generator$enum"."_AToB" USING btree ("B");


--
-- Name: _AToE_A; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AToE_A" ON "schema-generator$enum"."_AToE" USING btree ("A");


--
-- Name: _AToE_AB_unique; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE UNIQUE INDEX "_AToE_AB_unique" ON "schema-generator$enum"."_AToE" USING btree ("A", "B");


--
-- Name: _AToE_B; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AToE_B" ON "schema-generator$enum"."_AToE" USING btree ("B");


--
-- Name: _AWithIdToC_A; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AWithIdToC_A" ON "schema-generator$enum"."_AWithIdToC" USING btree ("A");


--
-- Name: _AWithIdToC_AB_unique; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE UNIQUE INDEX "_AWithIdToC_AB_unique" ON "schema-generator$enum"."_AWithIdToC" USING btree ("A", "B");


--
-- Name: _AWithIdToC_B; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AWithIdToC_B" ON "schema-generator$enum"."_AWithIdToC" USING btree ("B");


--
-- Name: _AWithIdToD_A; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AWithIdToD_A" ON "schema-generator$enum"."_AWithIdToD" USING btree ("A");


--
-- Name: _AWithIdToD_AB_unique; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE UNIQUE INDEX "_AWithIdToD_AB_unique" ON "schema-generator$enum"."_AWithIdToD" USING btree ("A", "B");


--
-- Name: _AWithIdToD_B; Type: INDEX; Schema: schema-generator$enum; Owner: -
--

CREATE INDEX "_AWithIdToD_B" ON "schema-generator$enum"."_AWithIdToD" USING btree ("B");


--
-- Name: AWithId_fieldC AWithId_fieldC_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."AWithId_fieldC"
    ADD CONSTRAINT "AWithId_fieldC_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$enum"."AWithId"(id);


--
-- Name: A_fieldC A_fieldC_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."A_fieldC"
    ADD CONSTRAINT "A_fieldC_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$enum"."A"(id);


--
-- Name: C_field C_field_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."C_field"
    ADD CONSTRAINT "C_field_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$enum"."C"(id);


--
-- Name: D_field D_field_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."D_field"
    ADD CONSTRAINT "D_field_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$enum"."D"(id);


--
-- Name: _AToB _AToB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AToB"
    ADD CONSTRAINT "_AToB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$enum"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToB _AToB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AToB"
    ADD CONSTRAINT "_AToB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$enum"."B"(id) ON DELETE CASCADE;


--
-- Name: _AToE _AToE_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AToE"
    ADD CONSTRAINT "_AToE_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$enum"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToE _AToE_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AToE"
    ADD CONSTRAINT "_AToE_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$enum"."E"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdToC _AWithIdToC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AWithIdToC"
    ADD CONSTRAINT "_AWithIdToC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$enum"."AWithId"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdToC _AWithIdToC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AWithIdToC"
    ADD CONSTRAINT "_AWithIdToC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$enum"."C"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdToD _AWithIdToD_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AWithIdToD"
    ADD CONSTRAINT "_AWithIdToD_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$enum"."AWithId"(id) ON DELETE CASCADE;


--
-- Name: _AWithIdToD _AWithIdToD_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$enum; Owner: -
--

ALTER TABLE ONLY "schema-generator$enum"."_AWithIdToD"
    ADD CONSTRAINT "_AWithIdToD_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$enum"."D"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

