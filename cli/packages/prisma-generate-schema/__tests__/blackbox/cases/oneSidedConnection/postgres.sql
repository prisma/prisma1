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
-- Name: schema-generator$oneSidedConnection; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$oneSidedConnection";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: A; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."A" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: B; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."B" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: C; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."C" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: D; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."D" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: E; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."E" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: F; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."F" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: TypeWithId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."TypeWithId" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: TypeWithoutId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."TypeWithoutId" (
    id character varying(25) NOT NULL,
    field text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _AToTypeWithId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_AToTypeWithId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AToTypeWithoutId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_AToTypeWithoutId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BToTypeWithId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_BToTypeWithId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BToTypeWithoutId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_BToTypeWithoutId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CToTypeWithId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_CToTypeWithId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CToTypeWithoutId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_CToTypeWithoutId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _DToTypeWithId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_DToTypeWithId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _DToTypeWithoutId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_DToTypeWithoutId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _EToTypeWithId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_EToTypeWithId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _EToTypeWithoutId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_EToTypeWithoutId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FToTypeWithId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_FToTypeWithId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FToTypeWithoutId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_FToTypeWithoutId" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE TABLE "schema-generator$oneSidedConnection"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: A A_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."A"
    ADD CONSTRAINT "A_pkey" PRIMARY KEY (id);


--
-- Name: B B_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."B"
    ADD CONSTRAINT "B_pkey" PRIMARY KEY (id);


--
-- Name: C C_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."C"
    ADD CONSTRAINT "C_pkey" PRIMARY KEY (id);


--
-- Name: D D_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."D"
    ADD CONSTRAINT "D_pkey" PRIMARY KEY (id);


--
-- Name: E E_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."E"
    ADD CONSTRAINT "E_pkey" PRIMARY KEY (id);


--
-- Name: F F_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."F"
    ADD CONSTRAINT "F_pkey" PRIMARY KEY (id);


--
-- Name: TypeWithId TypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."TypeWithId"
    ADD CONSTRAINT "TypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: TypeWithoutId TypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."TypeWithoutId"
    ADD CONSTRAINT "TypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _AToTypeWithId _AToTypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_AToTypeWithId"
    ADD CONSTRAINT "_AToTypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: _AToTypeWithoutId _AToTypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_AToTypeWithoutId"
    ADD CONSTRAINT "_AToTypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _BToTypeWithId _BToTypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_BToTypeWithId"
    ADD CONSTRAINT "_BToTypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: _BToTypeWithoutId _BToTypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_BToTypeWithoutId"
    ADD CONSTRAINT "_BToTypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _CToTypeWithId _CToTypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_CToTypeWithId"
    ADD CONSTRAINT "_CToTypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: _CToTypeWithoutId _CToTypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_CToTypeWithoutId"
    ADD CONSTRAINT "_CToTypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _DToTypeWithId _DToTypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_DToTypeWithId"
    ADD CONSTRAINT "_DToTypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: _DToTypeWithoutId _DToTypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_DToTypeWithoutId"
    ADD CONSTRAINT "_DToTypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _EToTypeWithId _EToTypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_EToTypeWithId"
    ADD CONSTRAINT "_EToTypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: _EToTypeWithoutId _EToTypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_EToTypeWithoutId"
    ADD CONSTRAINT "_EToTypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _FToTypeWithId _FToTypeWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_FToTypeWithId"
    ADD CONSTRAINT "_FToTypeWithId_pkey" PRIMARY KEY (id);


--
-- Name: _FToTypeWithoutId _FToTypeWithoutId_pkey; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_FToTypeWithoutId"
    ADD CONSTRAINT "_FToTypeWithoutId_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _AToTypeWithId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_AToTypeWithId_A" ON "schema-generator$oneSidedConnection"."_AToTypeWithId" USING btree ("A");


--
-- Name: _AToTypeWithId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AToTypeWithId_AB_unique" ON "schema-generator$oneSidedConnection"."_AToTypeWithId" USING btree ("A", "B");


--
-- Name: _AToTypeWithId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_AToTypeWithId_B" ON "schema-generator$oneSidedConnection"."_AToTypeWithId" USING btree ("B");


--
-- Name: _AToTypeWithoutId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_AToTypeWithoutId_A" ON "schema-generator$oneSidedConnection"."_AToTypeWithoutId" USING btree ("A");


--
-- Name: _AToTypeWithoutId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_AToTypeWithoutId_AB_unique" ON "schema-generator$oneSidedConnection"."_AToTypeWithoutId" USING btree ("A", "B");


--
-- Name: _AToTypeWithoutId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_AToTypeWithoutId_B" ON "schema-generator$oneSidedConnection"."_AToTypeWithoutId" USING btree ("B");


--
-- Name: _BToTypeWithId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_BToTypeWithId_A" ON "schema-generator$oneSidedConnection"."_BToTypeWithId" USING btree ("A");


--
-- Name: _BToTypeWithId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BToTypeWithId_AB_unique" ON "schema-generator$oneSidedConnection"."_BToTypeWithId" USING btree ("A", "B");


--
-- Name: _BToTypeWithId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_BToTypeWithId_B" ON "schema-generator$oneSidedConnection"."_BToTypeWithId" USING btree ("B");


--
-- Name: _BToTypeWithoutId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_BToTypeWithoutId_A" ON "schema-generator$oneSidedConnection"."_BToTypeWithoutId" USING btree ("A");


--
-- Name: _BToTypeWithoutId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_BToTypeWithoutId_AB_unique" ON "schema-generator$oneSidedConnection"."_BToTypeWithoutId" USING btree ("A", "B");


--
-- Name: _BToTypeWithoutId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_BToTypeWithoutId_B" ON "schema-generator$oneSidedConnection"."_BToTypeWithoutId" USING btree ("B");


--
-- Name: _CToTypeWithId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_CToTypeWithId_A" ON "schema-generator$oneSidedConnection"."_CToTypeWithId" USING btree ("A");


--
-- Name: _CToTypeWithId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_CToTypeWithId_AB_unique" ON "schema-generator$oneSidedConnection"."_CToTypeWithId" USING btree ("A", "B");


--
-- Name: _CToTypeWithId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_CToTypeWithId_B" ON "schema-generator$oneSidedConnection"."_CToTypeWithId" USING btree ("B");


--
-- Name: _CToTypeWithoutId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_CToTypeWithoutId_A" ON "schema-generator$oneSidedConnection"."_CToTypeWithoutId" USING btree ("A");


--
-- Name: _CToTypeWithoutId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_CToTypeWithoutId_AB_unique" ON "schema-generator$oneSidedConnection"."_CToTypeWithoutId" USING btree ("A", "B");


--
-- Name: _CToTypeWithoutId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_CToTypeWithoutId_B" ON "schema-generator$oneSidedConnection"."_CToTypeWithoutId" USING btree ("B");


--
-- Name: _DToTypeWithId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_DToTypeWithId_A" ON "schema-generator$oneSidedConnection"."_DToTypeWithId" USING btree ("A");


--
-- Name: _DToTypeWithId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_DToTypeWithId_AB_unique" ON "schema-generator$oneSidedConnection"."_DToTypeWithId" USING btree ("A", "B");


--
-- Name: _DToTypeWithId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_DToTypeWithId_B" ON "schema-generator$oneSidedConnection"."_DToTypeWithId" USING btree ("B");


--
-- Name: _DToTypeWithoutId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_DToTypeWithoutId_A" ON "schema-generator$oneSidedConnection"."_DToTypeWithoutId" USING btree ("A");


--
-- Name: _DToTypeWithoutId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_DToTypeWithoutId_AB_unique" ON "schema-generator$oneSidedConnection"."_DToTypeWithoutId" USING btree ("A", "B");


--
-- Name: _DToTypeWithoutId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_DToTypeWithoutId_B" ON "schema-generator$oneSidedConnection"."_DToTypeWithoutId" USING btree ("B");


--
-- Name: _EToTypeWithId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_EToTypeWithId_A" ON "schema-generator$oneSidedConnection"."_EToTypeWithId" USING btree ("A");


--
-- Name: _EToTypeWithId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_EToTypeWithId_AB_unique" ON "schema-generator$oneSidedConnection"."_EToTypeWithId" USING btree ("A", "B");


--
-- Name: _EToTypeWithId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_EToTypeWithId_B" ON "schema-generator$oneSidedConnection"."_EToTypeWithId" USING btree ("B");


--
-- Name: _EToTypeWithoutId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_EToTypeWithoutId_A" ON "schema-generator$oneSidedConnection"."_EToTypeWithoutId" USING btree ("A");


--
-- Name: _EToTypeWithoutId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_EToTypeWithoutId_AB_unique" ON "schema-generator$oneSidedConnection"."_EToTypeWithoutId" USING btree ("A", "B");


--
-- Name: _EToTypeWithoutId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_EToTypeWithoutId_B" ON "schema-generator$oneSidedConnection"."_EToTypeWithoutId" USING btree ("B");


--
-- Name: _FToTypeWithId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_FToTypeWithId_A" ON "schema-generator$oneSidedConnection"."_FToTypeWithId" USING btree ("A");


--
-- Name: _FToTypeWithId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_FToTypeWithId_AB_unique" ON "schema-generator$oneSidedConnection"."_FToTypeWithId" USING btree ("A", "B");


--
-- Name: _FToTypeWithId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_FToTypeWithId_B" ON "schema-generator$oneSidedConnection"."_FToTypeWithId" USING btree ("B");


--
-- Name: _FToTypeWithoutId_A; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_FToTypeWithoutId_A" ON "schema-generator$oneSidedConnection"."_FToTypeWithoutId" USING btree ("A");


--
-- Name: _FToTypeWithoutId_AB_unique; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE UNIQUE INDEX "_FToTypeWithoutId_AB_unique" ON "schema-generator$oneSidedConnection"."_FToTypeWithoutId" USING btree ("A", "B");


--
-- Name: _FToTypeWithoutId_B; Type: INDEX; Schema: schema-generator$oneSidedConnection; Owner: -
--

CREATE INDEX "_FToTypeWithoutId_B" ON "schema-generator$oneSidedConnection"."_FToTypeWithoutId" USING btree ("B");


--
-- Name: _AToTypeWithId _AToTypeWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_AToTypeWithId"
    ADD CONSTRAINT "_AToTypeWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToTypeWithId _AToTypeWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_AToTypeWithId"
    ADD CONSTRAINT "_AToTypeWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithId"(id) ON DELETE CASCADE;


--
-- Name: _AToTypeWithoutId _AToTypeWithoutId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_AToTypeWithoutId"
    ADD CONSTRAINT "_AToTypeWithoutId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToTypeWithoutId _AToTypeWithoutId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_AToTypeWithoutId"
    ADD CONSTRAINT "_AToTypeWithoutId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithoutId"(id) ON DELETE CASCADE;


--
-- Name: _BToTypeWithId _BToTypeWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_BToTypeWithId"
    ADD CONSTRAINT "_BToTypeWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToTypeWithId _BToTypeWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_BToTypeWithId"
    ADD CONSTRAINT "_BToTypeWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithId"(id) ON DELETE CASCADE;


--
-- Name: _BToTypeWithoutId _BToTypeWithoutId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_BToTypeWithoutId"
    ADD CONSTRAINT "_BToTypeWithoutId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToTypeWithoutId _BToTypeWithoutId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_BToTypeWithoutId"
    ADD CONSTRAINT "_BToTypeWithoutId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithoutId"(id) ON DELETE CASCADE;


--
-- Name: _CToTypeWithId _CToTypeWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_CToTypeWithId"
    ADD CONSTRAINT "_CToTypeWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."C"(id) ON DELETE CASCADE;


--
-- Name: _CToTypeWithId _CToTypeWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_CToTypeWithId"
    ADD CONSTRAINT "_CToTypeWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithId"(id) ON DELETE CASCADE;


--
-- Name: _CToTypeWithoutId _CToTypeWithoutId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_CToTypeWithoutId"
    ADD CONSTRAINT "_CToTypeWithoutId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."C"(id) ON DELETE CASCADE;


--
-- Name: _CToTypeWithoutId _CToTypeWithoutId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_CToTypeWithoutId"
    ADD CONSTRAINT "_CToTypeWithoutId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithoutId"(id) ON DELETE CASCADE;


--
-- Name: _DToTypeWithId _DToTypeWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_DToTypeWithId"
    ADD CONSTRAINT "_DToTypeWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."D"(id) ON DELETE CASCADE;


--
-- Name: _DToTypeWithId _DToTypeWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_DToTypeWithId"
    ADD CONSTRAINT "_DToTypeWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithId"(id) ON DELETE CASCADE;


--
-- Name: _DToTypeWithoutId _DToTypeWithoutId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_DToTypeWithoutId"
    ADD CONSTRAINT "_DToTypeWithoutId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."D"(id) ON DELETE CASCADE;


--
-- Name: _DToTypeWithoutId _DToTypeWithoutId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_DToTypeWithoutId"
    ADD CONSTRAINT "_DToTypeWithoutId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithoutId"(id) ON DELETE CASCADE;


--
-- Name: _EToTypeWithId _EToTypeWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_EToTypeWithId"
    ADD CONSTRAINT "_EToTypeWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."E"(id) ON DELETE CASCADE;


--
-- Name: _EToTypeWithId _EToTypeWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_EToTypeWithId"
    ADD CONSTRAINT "_EToTypeWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithId"(id) ON DELETE CASCADE;


--
-- Name: _EToTypeWithoutId _EToTypeWithoutId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_EToTypeWithoutId"
    ADD CONSTRAINT "_EToTypeWithoutId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."E"(id) ON DELETE CASCADE;


--
-- Name: _EToTypeWithoutId _EToTypeWithoutId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_EToTypeWithoutId"
    ADD CONSTRAINT "_EToTypeWithoutId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithoutId"(id) ON DELETE CASCADE;


--
-- Name: _FToTypeWithId _FToTypeWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_FToTypeWithId"
    ADD CONSTRAINT "_FToTypeWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."F"(id) ON DELETE CASCADE;


--
-- Name: _FToTypeWithId _FToTypeWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_FToTypeWithId"
    ADD CONSTRAINT "_FToTypeWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithId"(id) ON DELETE CASCADE;


--
-- Name: _FToTypeWithoutId _FToTypeWithoutId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_FToTypeWithoutId"
    ADD CONSTRAINT "_FToTypeWithoutId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$oneSidedConnection"."F"(id) ON DELETE CASCADE;


--
-- Name: _FToTypeWithoutId _FToTypeWithoutId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$oneSidedConnection; Owner: -
--

ALTER TABLE ONLY "schema-generator$oneSidedConnection"."_FToTypeWithoutId"
    ADD CONSTRAINT "_FToTypeWithoutId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$oneSidedConnection"."TypeWithoutId"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

