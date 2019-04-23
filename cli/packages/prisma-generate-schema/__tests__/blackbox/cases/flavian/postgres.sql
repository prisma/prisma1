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
-- Name: schema-generator$flavian; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$flavian";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Post; Type: TABLE; Schema: schema-generator$flavian; Owner: -
--

CREATE TABLE "schema-generator$flavian"."Post" (
    id character varying(25) NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    published boolean NOT NULL,
    title text NOT NULL,
    content text
);


--
-- Name: User; Type: TABLE; Schema: schema-generator$flavian; Owner: -
--

CREATE TABLE "schema-generator$flavian"."User" (
    id character varying(25) NOT NULL,
    email text NOT NULL,
    name text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _PostToUser; Type: TABLE; Schema: schema-generator$flavian; Owner: -
--

CREATE TABLE "schema-generator$flavian"."_PostToUser" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$flavian; Owner: -
--

CREATE TABLE "schema-generator$flavian"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: Post Post_pkey; Type: CONSTRAINT; Schema: schema-generator$flavian; Owner: -
--

ALTER TABLE ONLY "schema-generator$flavian"."Post"
    ADD CONSTRAINT "Post_pkey" PRIMARY KEY (id);


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: schema-generator$flavian; Owner: -
--

ALTER TABLE ONLY "schema-generator$flavian"."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$flavian; Owner: -
--

ALTER TABLE ONLY "schema-generator$flavian"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _PostToUser_A; Type: INDEX; Schema: schema-generator$flavian; Owner: -
--

CREATE INDEX "_PostToUser_A" ON "schema-generator$flavian"."_PostToUser" USING btree ("A");


--
-- Name: _PostToUser_AB_unique; Type: INDEX; Schema: schema-generator$flavian; Owner: -
--

CREATE UNIQUE INDEX "_PostToUser_AB_unique" ON "schema-generator$flavian"."_PostToUser" USING btree ("A", "B");


--
-- Name: _PostToUser_B; Type: INDEX; Schema: schema-generator$flavian; Owner: -
--

CREATE INDEX "_PostToUser_B" ON "schema-generator$flavian"."_PostToUser" USING btree ("B");


--
-- Name: schema-generator$flavian.User.email._UNIQUE; Type: INDEX; Schema: schema-generator$flavian; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$flavian.User.email._UNIQUE" ON "schema-generator$flavian"."User" USING btree (email);


--
-- Name: _PostToUser _PostToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$flavian; Owner: -
--

ALTER TABLE ONLY "schema-generator$flavian"."_PostToUser"
    ADD CONSTRAINT "_PostToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$flavian"."Post"(id) ON DELETE CASCADE;


--
-- Name: _PostToUser _PostToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$flavian; Owner: -
--

ALTER TABLE ONLY "schema-generator$flavian"."_PostToUser"
    ADD CONSTRAINT "_PostToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$flavian"."User"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

