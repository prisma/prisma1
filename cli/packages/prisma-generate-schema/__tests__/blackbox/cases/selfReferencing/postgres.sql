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
-- Name: schema-generator$selfReferencing; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$selfReferencing";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: MultiSelfReferencingB; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."MultiSelfReferencingB" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: MultiSelfReferencingBWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."MultiSelfReferencingBWithId" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: MultiSelfReferencingC; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."MultiSelfReferencingC" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: MultiSelfReferencingCWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."MultiSelfReferencingCWithId" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: SelfReferencingA; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."SelfReferencingA" (
    id character varying(25) NOT NULL,
    field integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: SelfReferencingAWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."SelfReferencingAWithId" (
    id character varying(25) NOT NULL,
    field integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: SelfReferencingB; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."SelfReferencingB" (
    id character varying(25) NOT NULL,
    field integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: SelfReferencingBWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."SelfReferencingBWithId" (
    id character varying(25) NOT NULL,
    field integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: SelfReferencingC; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."SelfReferencingC" (
    id character varying(25) NOT NULL,
    field integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: SelfReferencingCWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."SelfReferencingCWithId" (
    id character varying(25) NOT NULL,
    field integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _AToA; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_AToA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _AToA2; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_AToA2" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: _SelfReferencingAToSelfReferencingAWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_SelfReferencingAToSelfReferencingAWithId" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _SelfReferencingAWithIdToSelfReferencingAWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_SelfReferencingAWithIdToSelfReferencingAWithId" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _SelfReferencingBToSelfReferencingB; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_SelfReferencingBToSelfReferencingB" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _SelfReferencingBWithIdToSelfReferencingBWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingBWithId" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _SelfReferencingBWithIdToSelfReferencingCWithId; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingCWithId" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _SelfReferencingCToSelfReferencingC; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_SelfReferencingCToSelfReferencingC" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _WithIdAToA; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_WithIdAToA" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _WithIdAToA2; Type: TABLE; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE TABLE "schema-generator$selfReferencing"."_WithIdAToA2" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: MultiSelfReferencingBWithId MultiSelfReferencingBWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."MultiSelfReferencingBWithId"
    ADD CONSTRAINT "MultiSelfReferencingBWithId_pkey" PRIMARY KEY (id);


--
-- Name: MultiSelfReferencingB MultiSelfReferencingB_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."MultiSelfReferencingB"
    ADD CONSTRAINT "MultiSelfReferencingB_pkey" PRIMARY KEY (id);


--
-- Name: MultiSelfReferencingCWithId MultiSelfReferencingCWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."MultiSelfReferencingCWithId"
    ADD CONSTRAINT "MultiSelfReferencingCWithId_pkey" PRIMARY KEY (id);


--
-- Name: MultiSelfReferencingC MultiSelfReferencingC_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."MultiSelfReferencingC"
    ADD CONSTRAINT "MultiSelfReferencingC_pkey" PRIMARY KEY (id);


--
-- Name: SelfReferencingAWithId SelfReferencingAWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."SelfReferencingAWithId"
    ADD CONSTRAINT "SelfReferencingAWithId_pkey" PRIMARY KEY (id);


--
-- Name: SelfReferencingA SelfReferencingA_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."SelfReferencingA"
    ADD CONSTRAINT "SelfReferencingA_pkey" PRIMARY KEY (id);


--
-- Name: SelfReferencingBWithId SelfReferencingBWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."SelfReferencingBWithId"
    ADD CONSTRAINT "SelfReferencingBWithId_pkey" PRIMARY KEY (id);


--
-- Name: SelfReferencingB SelfReferencingB_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."SelfReferencingB"
    ADD CONSTRAINT "SelfReferencingB_pkey" PRIMARY KEY (id);


--
-- Name: SelfReferencingCWithId SelfReferencingCWithId_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."SelfReferencingCWithId"
    ADD CONSTRAINT "SelfReferencingCWithId_pkey" PRIMARY KEY (id);


--
-- Name: SelfReferencingC SelfReferencingC_pkey; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."SelfReferencingC"
    ADD CONSTRAINT "SelfReferencingC_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _AToA2_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_AToA2_A" ON "schema-generator$selfReferencing"."_AToA2" USING btree ("A");


--
-- Name: _AToA2_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_AToA2_AB_unique" ON "schema-generator$selfReferencing"."_AToA2" USING btree ("A", "B");


--
-- Name: _AToA2_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_AToA2_B" ON "schema-generator$selfReferencing"."_AToA2" USING btree ("B");


--
-- Name: _AToA_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_AToA_A" ON "schema-generator$selfReferencing"."_AToA" USING btree ("A");


--
-- Name: _AToA_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_AToA_AB_unique" ON "schema-generator$selfReferencing"."_AToA" USING btree ("A", "B");


--
-- Name: _AToA_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_AToA_B" ON "schema-generator$selfReferencing"."_AToA" USING btree ("B");


--
-- Name: _SelfReferencingAToSelfReferencingAWithId_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingAToSelfReferencingAWithId_A" ON "schema-generator$selfReferencing"."_SelfReferencingAToSelfReferencingAWithId" USING btree ("A");


--
-- Name: _SelfReferencingAToSelfReferencingAWithId_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_SelfReferencingAToSelfReferencingAWithId_AB_unique" ON "schema-generator$selfReferencing"."_SelfReferencingAToSelfReferencingAWithId" USING btree ("A", "B");


--
-- Name: _SelfReferencingAToSelfReferencingAWithId_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingAToSelfReferencingAWithId_B" ON "schema-generator$selfReferencing"."_SelfReferencingAToSelfReferencingAWithId" USING btree ("B");


--
-- Name: _SelfReferencingAWithIdToSelfReferencingAWithId_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingAWithIdToSelfReferencingAWithId_A" ON "schema-generator$selfReferencing"."_SelfReferencingAWithIdToSelfReferencingAWithId" USING btree ("A");


--
-- Name: _SelfReferencingAWithIdToSelfReferencingAWithId_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_SelfReferencingAWithIdToSelfReferencingAWithId_AB_unique" ON "schema-generator$selfReferencing"."_SelfReferencingAWithIdToSelfReferencingAWithId" USING btree ("A", "B");


--
-- Name: _SelfReferencingAWithIdToSelfReferencingAWithId_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingAWithIdToSelfReferencingAWithId_B" ON "schema-generator$selfReferencing"."_SelfReferencingAWithIdToSelfReferencingAWithId" USING btree ("B");


--
-- Name: _SelfReferencingBToSelfReferencingB_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingBToSelfReferencingB_A" ON "schema-generator$selfReferencing"."_SelfReferencingBToSelfReferencingB" USING btree ("A");


--
-- Name: _SelfReferencingBToSelfReferencingB_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_SelfReferencingBToSelfReferencingB_AB_unique" ON "schema-generator$selfReferencing"."_SelfReferencingBToSelfReferencingB" USING btree ("A", "B");


--
-- Name: _SelfReferencingBToSelfReferencingB_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingBToSelfReferencingB_B" ON "schema-generator$selfReferencing"."_SelfReferencingBToSelfReferencingB" USING btree ("B");


--
-- Name: _SelfReferencingBWithIdToSelfReferencingBWithId_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingBWithIdToSelfReferencingBWithId_A" ON "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingBWithId" USING btree ("A");


--
-- Name: _SelfReferencingBWithIdToSelfReferencingBWithId_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_SelfReferencingBWithIdToSelfReferencingBWithId_AB_unique" ON "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingBWithId" USING btree ("A", "B");


--
-- Name: _SelfReferencingBWithIdToSelfReferencingBWithId_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingBWithIdToSelfReferencingBWithId_B" ON "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingBWithId" USING btree ("B");


--
-- Name: _SelfReferencingBWithIdToSelfReferencingCWithId_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingBWithIdToSelfReferencingCWithId_A" ON "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingCWithId" USING btree ("A");


--
-- Name: _SelfReferencingBWithIdToSelfReferencingCWithId_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_SelfReferencingBWithIdToSelfReferencingCWithId_AB_unique" ON "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingCWithId" USING btree ("A", "B");


--
-- Name: _SelfReferencingBWithIdToSelfReferencingCWithId_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingBWithIdToSelfReferencingCWithId_B" ON "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingCWithId" USING btree ("B");


--
-- Name: _SelfReferencingCToSelfReferencingC_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingCToSelfReferencingC_A" ON "schema-generator$selfReferencing"."_SelfReferencingCToSelfReferencingC" USING btree ("A");


--
-- Name: _SelfReferencingCToSelfReferencingC_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_SelfReferencingCToSelfReferencingC_AB_unique" ON "schema-generator$selfReferencing"."_SelfReferencingCToSelfReferencingC" USING btree ("A", "B");


--
-- Name: _SelfReferencingCToSelfReferencingC_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_SelfReferencingCToSelfReferencingC_B" ON "schema-generator$selfReferencing"."_SelfReferencingCToSelfReferencingC" USING btree ("B");


--
-- Name: _WithIdAToA2_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_WithIdAToA2_A" ON "schema-generator$selfReferencing"."_WithIdAToA2" USING btree ("A");


--
-- Name: _WithIdAToA2_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_WithIdAToA2_AB_unique" ON "schema-generator$selfReferencing"."_WithIdAToA2" USING btree ("A", "B");


--
-- Name: _WithIdAToA2_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_WithIdAToA2_B" ON "schema-generator$selfReferencing"."_WithIdAToA2" USING btree ("B");


--
-- Name: _WithIdAToA_A; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_WithIdAToA_A" ON "schema-generator$selfReferencing"."_WithIdAToA" USING btree ("A");


--
-- Name: _WithIdAToA_AB_unique; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE UNIQUE INDEX "_WithIdAToA_AB_unique" ON "schema-generator$selfReferencing"."_WithIdAToA" USING btree ("A", "B");


--
-- Name: _WithIdAToA_B; Type: INDEX; Schema: schema-generator$selfReferencing; Owner: -
--

CREATE INDEX "_WithIdAToA_B" ON "schema-generator$selfReferencing"."_WithIdAToA" USING btree ("B");


--
-- Name: _AToA2 _AToA2_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_AToA2"
    ADD CONSTRAINT "_AToA2_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingB"(id) ON DELETE CASCADE;


--
-- Name: _AToA2 _AToA2_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_AToA2"
    ADD CONSTRAINT "_AToA2_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingB"(id) ON DELETE CASCADE;


--
-- Name: _AToA _AToA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_AToA"
    ADD CONSTRAINT "_AToA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingC"(id) ON DELETE CASCADE;


--
-- Name: _AToA _AToA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_AToA"
    ADD CONSTRAINT "_AToA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingC"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingAToSelfReferencingAWithId _SelfReferencingAToSelfReferencingAWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingAToSelfReferencingAWithId"
    ADD CONSTRAINT "_SelfReferencingAToSelfReferencingAWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."SelfReferencingA"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingAToSelfReferencingAWithId _SelfReferencingAToSelfReferencingAWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingAToSelfReferencingAWithId"
    ADD CONSTRAINT "_SelfReferencingAToSelfReferencingAWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."SelfReferencingAWithId"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingAWithIdToSelfReferencingAWithId _SelfReferencingAWithIdToSelfReferencingAWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingAWithIdToSelfReferencingAWithId"
    ADD CONSTRAINT "_SelfReferencingAWithIdToSelfReferencingAWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."SelfReferencingAWithId"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingAWithIdToSelfReferencingAWithId _SelfReferencingAWithIdToSelfReferencingAWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingAWithIdToSelfReferencingAWithId"
    ADD CONSTRAINT "_SelfReferencingAWithIdToSelfReferencingAWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."SelfReferencingAWithId"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingBToSelfReferencingB _SelfReferencingBToSelfReferencingB_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingBToSelfReferencingB"
    ADD CONSTRAINT "_SelfReferencingBToSelfReferencingB_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."SelfReferencingB"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingBToSelfReferencingB _SelfReferencingBToSelfReferencingB_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingBToSelfReferencingB"
    ADD CONSTRAINT "_SelfReferencingBToSelfReferencingB_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."SelfReferencingB"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingBWithIdToSelfReferencingBWithId _SelfReferencingBWithIdToSelfReferencingBWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingBWithId"
    ADD CONSTRAINT "_SelfReferencingBWithIdToSelfReferencingBWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."SelfReferencingBWithId"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingBWithIdToSelfReferencingBWithId _SelfReferencingBWithIdToSelfReferencingBWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingBWithId"
    ADD CONSTRAINT "_SelfReferencingBWithIdToSelfReferencingBWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."SelfReferencingBWithId"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingBWithIdToSelfReferencingCWithId _SelfReferencingBWithIdToSelfReferencingCWithId_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingCWithId"
    ADD CONSTRAINT "_SelfReferencingBWithIdToSelfReferencingCWithId_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."SelfReferencingBWithId"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingBWithIdToSelfReferencingCWithId _SelfReferencingBWithIdToSelfReferencingCWithId_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingBWithIdToSelfReferencingCWithId"
    ADD CONSTRAINT "_SelfReferencingBWithIdToSelfReferencingCWithId_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."SelfReferencingCWithId"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingCToSelfReferencingC _SelfReferencingCToSelfReferencingC_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingCToSelfReferencingC"
    ADD CONSTRAINT "_SelfReferencingCToSelfReferencingC_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."SelfReferencingC"(id) ON DELETE CASCADE;


--
-- Name: _SelfReferencingCToSelfReferencingC _SelfReferencingCToSelfReferencingC_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_SelfReferencingCToSelfReferencingC"
    ADD CONSTRAINT "_SelfReferencingCToSelfReferencingC_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."SelfReferencingC"(id) ON DELETE CASCADE;


--
-- Name: _WithIdAToA2 _WithIdAToA2_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_WithIdAToA2"
    ADD CONSTRAINT "_WithIdAToA2_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingBWithId"(id) ON DELETE CASCADE;


--
-- Name: _WithIdAToA2 _WithIdAToA2_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_WithIdAToA2"
    ADD CONSTRAINT "_WithIdAToA2_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingBWithId"(id) ON DELETE CASCADE;


--
-- Name: _WithIdAToA _WithIdAToA_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_WithIdAToA"
    ADD CONSTRAINT "_WithIdAToA_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingCWithId"(id) ON DELETE CASCADE;


--
-- Name: _WithIdAToA _WithIdAToA_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$selfReferencing; Owner: -
--

ALTER TABLE ONLY "schema-generator$selfReferencing"."_WithIdAToA"
    ADD CONSTRAINT "_WithIdAToA_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$selfReferencing"."MultiSelfReferencingCWithId"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

