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
-- Name: schema-generator$meshRelation; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$meshRelation";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: A; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."A" (
    id character varying(25) NOT NULL,
    field integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: B; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."B" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: B_field; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."B_field" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: C; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."C" (
    id character varying(25) NOT NULL,
    "expirationDate" timestamp(3) without time zone,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: D; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."D" (
    id character varying(25) NOT NULL,
    field text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: E; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."E" (
    id character varying(25) NOT NULL,
    field text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _AToA; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_AToA" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AToB; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_AToB" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AToC; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_AToC" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AToD; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_AToD" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AToE; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_AToE" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BToB; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_BToB" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BToC; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_BToC" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BToC2; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_BToC2" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BToD; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_BToD" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CToC; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_CToC" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CToD; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_CToD" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _DToD; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_DToD" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$meshRelation; Owner: -
--

CREATE TABLE "schema-generator$meshRelation"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: A A_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."A"
    ADD CONSTRAINT "A_pkey" PRIMARY KEY (id);


--
-- Name: B_field B_field_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."B_field"
    ADD CONSTRAINT "B_field_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: B B_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."B"
    ADD CONSTRAINT "B_pkey" PRIMARY KEY (id);


--
-- Name: C C_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."C"
    ADD CONSTRAINT "C_pkey" PRIMARY KEY (id);


--
-- Name: D D_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."D"
    ADD CONSTRAINT "D_pkey" PRIMARY KEY (id);


--
-- Name: E E_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."E"
    ADD CONSTRAINT "E_pkey" PRIMARY KEY (id);


--
-- Name: _AToA _AToA_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToA"
    ADD CONSTRAINT "_AToA_pkey" PRIMARY KEY (id);


--
-- Name: _AToB _AToB_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToB"
    ADD CONSTRAINT "_AToB_pkey" PRIMARY KEY (id);


--
-- Name: _AToC _AToC_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToC"
    ADD CONSTRAINT "_AToC_pkey" PRIMARY KEY (id);


--
-- Name: _AToD _AToD_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToD"
    ADD CONSTRAINT "_AToD_pkey" PRIMARY KEY (id);


--
-- Name: _AToE _AToE_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToE"
    ADD CONSTRAINT "_AToE_pkey" PRIMARY KEY (id);


--
-- Name: _BToB _BToB_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToB"
    ADD CONSTRAINT "_BToB_pkey" PRIMARY KEY (id);


--
-- Name: _BToC2 _BToC2_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToC2"
    ADD CONSTRAINT "_BToC2_pkey" PRIMARY KEY (id);


--
-- Name: _BToC _BToC_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToC"
    ADD CONSTRAINT "_BToC_pkey" PRIMARY KEY (id);


--
-- Name: _BToD _BToD_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToD"
    ADD CONSTRAINT "_BToD_pkey" PRIMARY KEY (id);


--
-- Name: _CToC _CToC_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_CToC"
    ADD CONSTRAINT "_CToC_pkey" PRIMARY KEY (id);


--
-- Name: _CToD _CToD_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_CToD"
    ADD CONSTRAINT "_CToD_pkey" PRIMARY KEY (id);


--
-- Name: _DToD _DToD_pkey; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_DToD"
    ADD CONSTRAINT "_DToD_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _AToA_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToA_A" ON "schema-generator$meshRelation"."_AToA" USING btree ("A");


--
-- Name: _AToA_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_AToA_AB_unique" ON "schema-generator$meshRelation"."_AToA" USING btree ("A", "B");


--
-- Name: _AToA_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToA_B" ON "schema-generator$meshRelation"."_AToA" USING btree ("B");


--
-- Name: _AToB_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToB_A" ON "schema-generator$meshRelation"."_AToB" USING btree ("A");


--
-- Name: _AToB_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_AToB_AB_unique" ON "schema-generator$meshRelation"."_AToB" USING btree ("A", "B");


--
-- Name: _AToB_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToB_B" ON "schema-generator$meshRelation"."_AToB" USING btree ("B");


--
-- Name: _AToC_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToC_A" ON "schema-generator$meshRelation"."_AToC" USING btree ("A");


--
-- Name: _AToC_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_AToC_AB_unique" ON "schema-generator$meshRelation"."_AToC" USING btree ("A", "B");


--
-- Name: _AToC_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToC_B" ON "schema-generator$meshRelation"."_AToC" USING btree ("B");


--
-- Name: _AToD_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToD_A" ON "schema-generator$meshRelation"."_AToD" USING btree ("A");


--
-- Name: _AToD_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_AToD_AB_unique" ON "schema-generator$meshRelation"."_AToD" USING btree ("A", "B");


--
-- Name: _AToD_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToD_B" ON "schema-generator$meshRelation"."_AToD" USING btree ("B");


--
-- Name: _AToE_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToE_A" ON "schema-generator$meshRelation"."_AToE" USING btree ("A");


--
-- Name: _AToE_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_AToE_AB_unique" ON "schema-generator$meshRelation"."_AToE" USING btree ("A", "B");


--
-- Name: _AToE_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_AToE_B" ON "schema-generator$meshRelation"."_AToE" USING btree ("B");


--
-- Name: _BToB_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToB_A" ON "schema-generator$meshRelation"."_BToB" USING btree ("A");


--
-- Name: _BToB_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_BToB_AB_unique" ON "schema-generator$meshRelation"."_BToB" USING btree ("A", "B");


--
-- Name: _BToB_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToB_B" ON "schema-generator$meshRelation"."_BToB" USING btree ("B");


--
-- Name: _BToC2_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToC2_A" ON "schema-generator$meshRelation"."_BToC2" USING btree ("A");


--
-- Name: _BToC2_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_BToC2_AB_unique" ON "schema-generator$meshRelation"."_BToC2" USING btree ("A", "B");


--
-- Name: _BToC2_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToC2_B" ON "schema-generator$meshRelation"."_BToC2" USING btree ("B");


--
-- Name: _BToC_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToC_A" ON "schema-generator$meshRelation"."_BToC" USING btree ("A");


--
-- Name: _BToC_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_BToC_AB_unique" ON "schema-generator$meshRelation"."_BToC" USING btree ("A", "B");


--
-- Name: _BToC_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToC_B" ON "schema-generator$meshRelation"."_BToC" USING btree ("B");


--
-- Name: _BToD_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToD_A" ON "schema-generator$meshRelation"."_BToD" USING btree ("A");


--
-- Name: _BToD_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_BToD_AB_unique" ON "schema-generator$meshRelation"."_BToD" USING btree ("A", "B");


--
-- Name: _BToD_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_BToD_B" ON "schema-generator$meshRelation"."_BToD" USING btree ("B");


--
-- Name: _CToC_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_CToC_A" ON "schema-generator$meshRelation"."_CToC" USING btree ("A");


--
-- Name: _CToC_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_CToC_AB_unique" ON "schema-generator$meshRelation"."_CToC" USING btree ("A", "B");


--
-- Name: _CToC_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_CToC_B" ON "schema-generator$meshRelation"."_CToC" USING btree ("B");


--
-- Name: _CToD_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_CToD_A" ON "schema-generator$meshRelation"."_CToD" USING btree ("A");


--
-- Name: _CToD_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_CToD_AB_unique" ON "schema-generator$meshRelation"."_CToD" USING btree ("A", "B");


--
-- Name: _CToD_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_CToD_B" ON "schema-generator$meshRelation"."_CToD" USING btree ("B");


--
-- Name: _DToD_A; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_DToD_A" ON "schema-generator$meshRelation"."_DToD" USING btree ("A");


--
-- Name: _DToD_AB_unique; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE UNIQUE INDEX "_DToD_AB_unique" ON "schema-generator$meshRelation"."_DToD" USING btree ("A", "B");


--
-- Name: _DToD_B; Type: INDEX; Schema: schema-generator$meshRelation; Owner: -
--

CREATE INDEX "_DToD_B" ON "schema-generator$meshRelation"."_DToD" USING btree ("B");


--
-- Name: B_field B_field_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."B_field"
    ADD CONSTRAINT "B_field_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$meshRelation"."B"(id);


--
-- Name: _AToA _AToA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToA"
    ADD CONSTRAINT "_AToA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToA _AToA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToA"
    ADD CONSTRAINT "_AToA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToB _AToB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToB"
    ADD CONSTRAINT "_AToB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToB _AToB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToB"
    ADD CONSTRAINT "_AToB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."B"(id) ON DELETE CASCADE;


--
-- Name: _AToC _AToC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToC"
    ADD CONSTRAINT "_AToC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToC _AToC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToC"
    ADD CONSTRAINT "_AToC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."C"(id) ON DELETE CASCADE;


--
-- Name: _AToD _AToD_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToD"
    ADD CONSTRAINT "_AToD_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToD _AToD_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToD"
    ADD CONSTRAINT "_AToD_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."D"(id) ON DELETE CASCADE;


--
-- Name: _AToE _AToE_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToE"
    ADD CONSTRAINT "_AToE_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."A"(id) ON DELETE CASCADE;


--
-- Name: _AToE _AToE_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_AToE"
    ADD CONSTRAINT "_AToE_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."E"(id) ON DELETE CASCADE;


--
-- Name: _BToB _BToB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToB"
    ADD CONSTRAINT "_BToB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToB _BToB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToB"
    ADD CONSTRAINT "_BToB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToC2 _BToC2_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToC2"
    ADD CONSTRAINT "_BToC2_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToC2 _BToC2_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToC2"
    ADD CONSTRAINT "_BToC2_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."C"(id) ON DELETE CASCADE;


--
-- Name: _BToC _BToC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToC"
    ADD CONSTRAINT "_BToC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToC _BToC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToC"
    ADD CONSTRAINT "_BToC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."C"(id) ON DELETE CASCADE;


--
-- Name: _BToD _BToD_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToD"
    ADD CONSTRAINT "_BToD_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."B"(id) ON DELETE CASCADE;


--
-- Name: _BToD _BToD_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_BToD"
    ADD CONSTRAINT "_BToD_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."D"(id) ON DELETE CASCADE;


--
-- Name: _CToC _CToC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_CToC"
    ADD CONSTRAINT "_CToC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."C"(id) ON DELETE CASCADE;


--
-- Name: _CToC _CToC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_CToC"
    ADD CONSTRAINT "_CToC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."C"(id) ON DELETE CASCADE;


--
-- Name: _CToD _CToD_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_CToD"
    ADD CONSTRAINT "_CToD_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."C"(id) ON DELETE CASCADE;


--
-- Name: _CToD _CToD_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_CToD"
    ADD CONSTRAINT "_CToD_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."D"(id) ON DELETE CASCADE;


--
-- Name: _DToD _DToD_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_DToD"
    ADD CONSTRAINT "_DToD_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$meshRelation"."D"(id) ON DELETE CASCADE;


--
-- Name: _DToD _DToD_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$meshRelation; Owner: -
--

ALTER TABLE ONLY "schema-generator$meshRelation"."_DToD"
    ADD CONSTRAINT "_DToD_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$meshRelation"."D"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

