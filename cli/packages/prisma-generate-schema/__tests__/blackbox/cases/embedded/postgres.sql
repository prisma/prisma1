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
-- Name: schema-generator$embedded; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$embedded";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Child; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."Child" (
    id character varying(25) NOT NULL,
    c text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Friend; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."Friend" (
    id character varying(25) NOT NULL,
    f text,
    test text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Joint; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."Joint" (
    id character varying(25) NOT NULL,
    j text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Parent; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."Parent" (
    id character varying(25) NOT NULL,
    p text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _ChildToFriend; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."_ChildToFriend" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _ChildToJoint; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."_ChildToJoint" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _ChildToParent; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."_ChildToParent" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _JointToParent; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."_JointToParent" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$embedded; Owner: -
--

CREATE TABLE "schema-generator$embedded"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: Child Child_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."Child"
    ADD CONSTRAINT "Child_pkey" PRIMARY KEY (id);


--
-- Name: Friend Friend_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."Friend"
    ADD CONSTRAINT "Friend_pkey" PRIMARY KEY (id);


--
-- Name: Joint Joint_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."Joint"
    ADD CONSTRAINT "Joint_pkey" PRIMARY KEY (id);


--
-- Name: Parent Parent_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."Parent"
    ADD CONSTRAINT "Parent_pkey" PRIMARY KEY (id);


--
-- Name: _ChildToFriend _ChildToFriend_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToFriend"
    ADD CONSTRAINT "_ChildToFriend_pkey" PRIMARY KEY (id);


--
-- Name: _ChildToJoint _ChildToJoint_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToJoint"
    ADD CONSTRAINT "_ChildToJoint_pkey" PRIMARY KEY (id);


--
-- Name: _ChildToParent _ChildToParent_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToParent"
    ADD CONSTRAINT "_ChildToParent_pkey" PRIMARY KEY (id);


--
-- Name: _JointToParent _JointToParent_pkey; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_JointToParent"
    ADD CONSTRAINT "_JointToParent_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _ChildToFriend_A; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_ChildToFriend_A" ON "schema-generator$embedded"."_ChildToFriend" USING btree ("A");


--
-- Name: _ChildToFriend_AB_unique; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE UNIQUE INDEX "_ChildToFriend_AB_unique" ON "schema-generator$embedded"."_ChildToFriend" USING btree ("A", "B");


--
-- Name: _ChildToFriend_B; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_ChildToFriend_B" ON "schema-generator$embedded"."_ChildToFriend" USING btree ("B");


--
-- Name: _ChildToJoint_A; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_ChildToJoint_A" ON "schema-generator$embedded"."_ChildToJoint" USING btree ("A");


--
-- Name: _ChildToJoint_AB_unique; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE UNIQUE INDEX "_ChildToJoint_AB_unique" ON "schema-generator$embedded"."_ChildToJoint" USING btree ("A", "B");


--
-- Name: _ChildToJoint_B; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_ChildToJoint_B" ON "schema-generator$embedded"."_ChildToJoint" USING btree ("B");


--
-- Name: _ChildToParent_A; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_ChildToParent_A" ON "schema-generator$embedded"."_ChildToParent" USING btree ("A");


--
-- Name: _ChildToParent_AB_unique; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE UNIQUE INDEX "_ChildToParent_AB_unique" ON "schema-generator$embedded"."_ChildToParent" USING btree ("A", "B");


--
-- Name: _ChildToParent_B; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_ChildToParent_B" ON "schema-generator$embedded"."_ChildToParent" USING btree ("B");


--
-- Name: _JointToParent_A; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_JointToParent_A" ON "schema-generator$embedded"."_JointToParent" USING btree ("A");


--
-- Name: _JointToParent_AB_unique; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE UNIQUE INDEX "_JointToParent_AB_unique" ON "schema-generator$embedded"."_JointToParent" USING btree ("A", "B");


--
-- Name: _JointToParent_B; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE INDEX "_JointToParent_B" ON "schema-generator$embedded"."_JointToParent" USING btree ("B");


--
-- Name: schema-generator$embedded.Child.c._UNIQUE; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$embedded.Child.c._UNIQUE" ON "schema-generator$embedded"."Child" USING btree (c);


--
-- Name: schema-generator$embedded.Friend.f._UNIQUE; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$embedded.Friend.f._UNIQUE" ON "schema-generator$embedded"."Friend" USING btree (f);


--
-- Name: schema-generator$embedded.Parent.p._UNIQUE; Type: INDEX; Schema: schema-generator$embedded; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$embedded.Parent.p._UNIQUE" ON "schema-generator$embedded"."Parent" USING btree (p);


--
-- Name: _ChildToFriend _ChildToFriend_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToFriend"
    ADD CONSTRAINT "_ChildToFriend_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$embedded"."Child"(id) ON DELETE CASCADE;


--
-- Name: _ChildToFriend _ChildToFriend_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToFriend"
    ADD CONSTRAINT "_ChildToFriend_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$embedded"."Friend"(id) ON DELETE CASCADE;


--
-- Name: _ChildToJoint _ChildToJoint_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToJoint"
    ADD CONSTRAINT "_ChildToJoint_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$embedded"."Child"(id) ON DELETE CASCADE;


--
-- Name: _ChildToJoint _ChildToJoint_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToJoint"
    ADD CONSTRAINT "_ChildToJoint_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$embedded"."Joint"(id) ON DELETE CASCADE;


--
-- Name: _ChildToParent _ChildToParent_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToParent"
    ADD CONSTRAINT "_ChildToParent_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$embedded"."Child"(id) ON DELETE CASCADE;


--
-- Name: _ChildToParent _ChildToParent_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_ChildToParent"
    ADD CONSTRAINT "_ChildToParent_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$embedded"."Parent"(id) ON DELETE CASCADE;


--
-- Name: _JointToParent _JointToParent_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_JointToParent"
    ADD CONSTRAINT "_JointToParent_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$embedded"."Joint"(id) ON DELETE CASCADE;


--
-- Name: _JointToParent _JointToParent_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$embedded; Owner: -
--

ALTER TABLE ONLY "schema-generator$embedded"."_JointToParent"
    ADD CONSTRAINT "_JointToParent_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$embedded"."Parent"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

