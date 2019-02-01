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
-- Name: schema-generator$emptyTypes; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$emptyTypes";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: OnlyDate; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."OnlyDate" (
    id character varying(25) NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OnlyId; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."OnlyId" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OnlyIdAndARelation; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."OnlyIdAndARelation" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OnlyIdAndARelation2; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."OnlyIdAndARelation2" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OnlyRelation; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."OnlyRelation" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OnlyRelationA; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."OnlyRelationA" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OnlyRelationB; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."OnlyRelationB" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _OnlyDateToOnlyRelation; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."_OnlyDateToOnlyRelation" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _OnlyIdToOnlyIdAndARelation; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _OnlyIdToOnlyIdAndARelation2; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation2" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _OnlyRelationAToOnlyRelationA; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."_OnlyRelationAToOnlyRelationA" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _OnlyRelationBToOnlyRelationB; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."_OnlyRelationBToOnlyRelationB" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE TABLE "schema-generator$emptyTypes"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: OnlyDate OnlyDate_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."OnlyDate"
    ADD CONSTRAINT "OnlyDate_pkey" PRIMARY KEY (id);


--
-- Name: OnlyIdAndARelation2 OnlyIdAndARelation2_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."OnlyIdAndARelation2"
    ADD CONSTRAINT "OnlyIdAndARelation2_pkey" PRIMARY KEY (id);


--
-- Name: OnlyIdAndARelation OnlyIdAndARelation_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."OnlyIdAndARelation"
    ADD CONSTRAINT "OnlyIdAndARelation_pkey" PRIMARY KEY (id);


--
-- Name: OnlyId OnlyId_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."OnlyId"
    ADD CONSTRAINT "OnlyId_pkey" PRIMARY KEY (id);


--
-- Name: OnlyRelationA OnlyRelationA_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."OnlyRelationA"
    ADD CONSTRAINT "OnlyRelationA_pkey" PRIMARY KEY (id);


--
-- Name: OnlyRelationB OnlyRelationB_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."OnlyRelationB"
    ADD CONSTRAINT "OnlyRelationB_pkey" PRIMARY KEY (id);


--
-- Name: OnlyRelation OnlyRelation_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."OnlyRelation"
    ADD CONSTRAINT "OnlyRelation_pkey" PRIMARY KEY (id);


--
-- Name: _OnlyDateToOnlyRelation _OnlyDateToOnlyRelation_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyDateToOnlyRelation"
    ADD CONSTRAINT "_OnlyDateToOnlyRelation_pkey" PRIMARY KEY (id);


--
-- Name: _OnlyIdToOnlyIdAndARelation2 _OnlyIdToOnlyIdAndARelation2_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation2"
    ADD CONSTRAINT "_OnlyIdToOnlyIdAndARelation2_pkey" PRIMARY KEY (id);


--
-- Name: _OnlyIdToOnlyIdAndARelation _OnlyIdToOnlyIdAndARelation_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation"
    ADD CONSTRAINT "_OnlyIdToOnlyIdAndARelation_pkey" PRIMARY KEY (id);


--
-- Name: _OnlyRelationAToOnlyRelationA _OnlyRelationAToOnlyRelationA_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyRelationAToOnlyRelationA"
    ADD CONSTRAINT "_OnlyRelationAToOnlyRelationA_pkey" PRIMARY KEY (id);


--
-- Name: _OnlyRelationBToOnlyRelationB _OnlyRelationBToOnlyRelationB_pkey; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyRelationBToOnlyRelationB"
    ADD CONSTRAINT "_OnlyRelationBToOnlyRelationB_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _OnlyDateToOnlyRelation_A; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyDateToOnlyRelation_A" ON "schema-generator$emptyTypes"."_OnlyDateToOnlyRelation" USING btree ("A");


--
-- Name: _OnlyDateToOnlyRelation_AB_unique; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE UNIQUE INDEX "_OnlyDateToOnlyRelation_AB_unique" ON "schema-generator$emptyTypes"."_OnlyDateToOnlyRelation" USING btree ("A", "B");


--
-- Name: _OnlyDateToOnlyRelation_B; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyDateToOnlyRelation_B" ON "schema-generator$emptyTypes"."_OnlyDateToOnlyRelation" USING btree ("B");


--
-- Name: _OnlyIdToOnlyIdAndARelation2_A; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyIdToOnlyIdAndARelation2_A" ON "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation2" USING btree ("A");


--
-- Name: _OnlyIdToOnlyIdAndARelation2_AB_unique; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE UNIQUE INDEX "_OnlyIdToOnlyIdAndARelation2_AB_unique" ON "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation2" USING btree ("A", "B");


--
-- Name: _OnlyIdToOnlyIdAndARelation2_B; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyIdToOnlyIdAndARelation2_B" ON "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation2" USING btree ("B");


--
-- Name: _OnlyIdToOnlyIdAndARelation_A; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyIdToOnlyIdAndARelation_A" ON "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation" USING btree ("A");


--
-- Name: _OnlyIdToOnlyIdAndARelation_AB_unique; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE UNIQUE INDEX "_OnlyIdToOnlyIdAndARelation_AB_unique" ON "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation" USING btree ("A", "B");


--
-- Name: _OnlyIdToOnlyIdAndARelation_B; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyIdToOnlyIdAndARelation_B" ON "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation" USING btree ("B");


--
-- Name: _OnlyRelationAToOnlyRelationA_A; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyRelationAToOnlyRelationA_A" ON "schema-generator$emptyTypes"."_OnlyRelationAToOnlyRelationA" USING btree ("A");


--
-- Name: _OnlyRelationAToOnlyRelationA_AB_unique; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE UNIQUE INDEX "_OnlyRelationAToOnlyRelationA_AB_unique" ON "schema-generator$emptyTypes"."_OnlyRelationAToOnlyRelationA" USING btree ("A", "B");


--
-- Name: _OnlyRelationAToOnlyRelationA_B; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyRelationAToOnlyRelationA_B" ON "schema-generator$emptyTypes"."_OnlyRelationAToOnlyRelationA" USING btree ("B");


--
-- Name: _OnlyRelationBToOnlyRelationB_A; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyRelationBToOnlyRelationB_A" ON "schema-generator$emptyTypes"."_OnlyRelationBToOnlyRelationB" USING btree ("A");


--
-- Name: _OnlyRelationBToOnlyRelationB_AB_unique; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE UNIQUE INDEX "_OnlyRelationBToOnlyRelationB_AB_unique" ON "schema-generator$emptyTypes"."_OnlyRelationBToOnlyRelationB" USING btree ("A", "B");


--
-- Name: _OnlyRelationBToOnlyRelationB_B; Type: INDEX; Schema: schema-generator$emptyTypes; Owner: -
--

CREATE INDEX "_OnlyRelationBToOnlyRelationB_B" ON "schema-generator$emptyTypes"."_OnlyRelationBToOnlyRelationB" USING btree ("B");


--
-- Name: _OnlyDateToOnlyRelation _OnlyDateToOnlyRelation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyDateToOnlyRelation"
    ADD CONSTRAINT "_OnlyDateToOnlyRelation_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$emptyTypes"."OnlyDate"(id) ON DELETE CASCADE;


--
-- Name: _OnlyDateToOnlyRelation _OnlyDateToOnlyRelation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyDateToOnlyRelation"
    ADD CONSTRAINT "_OnlyDateToOnlyRelation_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$emptyTypes"."OnlyRelation"(id) ON DELETE CASCADE;


--
-- Name: _OnlyIdToOnlyIdAndARelation2 _OnlyIdToOnlyIdAndARelation2_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation2"
    ADD CONSTRAINT "_OnlyIdToOnlyIdAndARelation2_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$emptyTypes"."OnlyId"(id) ON DELETE CASCADE;


--
-- Name: _OnlyIdToOnlyIdAndARelation2 _OnlyIdToOnlyIdAndARelation2_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation2"
    ADD CONSTRAINT "_OnlyIdToOnlyIdAndARelation2_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$emptyTypes"."OnlyIdAndARelation2"(id) ON DELETE CASCADE;


--
-- Name: _OnlyIdToOnlyIdAndARelation _OnlyIdToOnlyIdAndARelation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation"
    ADD CONSTRAINT "_OnlyIdToOnlyIdAndARelation_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$emptyTypes"."OnlyId"(id) ON DELETE CASCADE;


--
-- Name: _OnlyIdToOnlyIdAndARelation _OnlyIdToOnlyIdAndARelation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyIdToOnlyIdAndARelation"
    ADD CONSTRAINT "_OnlyIdToOnlyIdAndARelation_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$emptyTypes"."OnlyIdAndARelation"(id) ON DELETE CASCADE;


--
-- Name: _OnlyRelationAToOnlyRelationA _OnlyRelationAToOnlyRelationA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyRelationAToOnlyRelationA"
    ADD CONSTRAINT "_OnlyRelationAToOnlyRelationA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$emptyTypes"."OnlyRelationA"(id) ON DELETE CASCADE;


--
-- Name: _OnlyRelationAToOnlyRelationA _OnlyRelationAToOnlyRelationA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyRelationAToOnlyRelationA"
    ADD CONSTRAINT "_OnlyRelationAToOnlyRelationA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$emptyTypes"."OnlyRelationA"(id) ON DELETE CASCADE;


--
-- Name: _OnlyRelationBToOnlyRelationB _OnlyRelationBToOnlyRelationB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyRelationBToOnlyRelationB"
    ADD CONSTRAINT "_OnlyRelationBToOnlyRelationB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$emptyTypes"."OnlyRelationB"(id) ON DELETE CASCADE;


--
-- Name: _OnlyRelationBToOnlyRelationB _OnlyRelationBToOnlyRelationB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$emptyTypes; Owner: -
--

ALTER TABLE ONLY "schema-generator$emptyTypes"."_OnlyRelationBToOnlyRelationB"
    ADD CONSTRAINT "_OnlyRelationBToOnlyRelationB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$emptyTypes"."OnlyRelationB"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

