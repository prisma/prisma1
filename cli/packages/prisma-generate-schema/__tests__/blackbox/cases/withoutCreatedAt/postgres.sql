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
-- Name: schema-generator$withoutCreatedAt; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$withoutCreatedAt";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: MultiRefToUsers; Type: TABLE; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE TABLE "schema-generator$withoutCreatedAt"."MultiRefToUsers" (
    id character varying(25) NOT NULL,
    name text
);


--
-- Name: Post; Type: TABLE; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE TABLE "schema-generator$withoutCreatedAt"."Post" (
    id character varying(25) NOT NULL,
    published boolean NOT NULL,
    title text NOT NULL,
    content text
);


--
-- Name: User; Type: TABLE; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE TABLE "schema-generator$withoutCreatedAt"."User" (
    id character varying(25) NOT NULL,
    email text NOT NULL,
    name text,
    "signUpDate" timestamp(3) without time zone NOT NULL
);


--
-- Name: _MultiRefToUsersToUser; Type: TABLE; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE TABLE "schema-generator$withoutCreatedAt"."_MultiRefToUsersToUser" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _PostToUser; Type: TABLE; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE TABLE "schema-generator$withoutCreatedAt"."_PostToUser" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: MultiRefToUsers MultiRefToUsers_pkey; Type: CONSTRAINT; Schema: schema-generator$withoutCreatedAt; Owner: -
--

ALTER TABLE ONLY "schema-generator$withoutCreatedAt"."MultiRefToUsers"
    ADD CONSTRAINT "MultiRefToUsers_pkey" PRIMARY KEY (id);


--
-- Name: Post Post_pkey; Type: CONSTRAINT; Schema: schema-generator$withoutCreatedAt; Owner: -
--

ALTER TABLE ONLY "schema-generator$withoutCreatedAt"."Post"
    ADD CONSTRAINT "Post_pkey" PRIMARY KEY (id);


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: schema-generator$withoutCreatedAt; Owner: -
--

ALTER TABLE ONLY "schema-generator$withoutCreatedAt"."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: _MultiRefToUsersToUser_AB_unique; Type: INDEX; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE UNIQUE INDEX "_MultiRefToUsersToUser_AB_unique" ON "schema-generator$withoutCreatedAt"."_MultiRefToUsersToUser" USING btree ("A", "B");


--
-- Name: _MultiRefToUsersToUser_B; Type: INDEX; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE INDEX "_MultiRefToUsersToUser_B" ON "schema-generator$withoutCreatedAt"."_MultiRefToUsersToUser" USING btree ("B");


--
-- Name: _PostToUser_AB_unique; Type: INDEX; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE UNIQUE INDEX "_PostToUser_AB_unique" ON "schema-generator$withoutCreatedAt"."_PostToUser" USING btree ("A", "B");


--
-- Name: _PostToUser_B; Type: INDEX; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE INDEX "_PostToUser_B" ON "schema-generator$withoutCreatedAt"."_PostToUser" USING btree ("B");


--
-- Name: schema-generator$withoutCreatedAt.User.email._UNIQUE; Type: INDEX; Schema: schema-generator$withoutCreatedAt; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$withoutCreatedAt.User.email._UNIQUE" ON "schema-generator$withoutCreatedAt"."User" USING btree (email);


--
-- Name: _MultiRefToUsersToUser _MultiRefToUsersToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$withoutCreatedAt; Owner: -
--

ALTER TABLE ONLY "schema-generator$withoutCreatedAt"."_MultiRefToUsersToUser"
    ADD CONSTRAINT "_MultiRefToUsersToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$withoutCreatedAt"."MultiRefToUsers"(id) ON DELETE CASCADE;


--
-- Name: _MultiRefToUsersToUser _MultiRefToUsersToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$withoutCreatedAt; Owner: -
--

ALTER TABLE ONLY "schema-generator$withoutCreatedAt"."_MultiRefToUsersToUser"
    ADD CONSTRAINT "_MultiRefToUsersToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$withoutCreatedAt"."User"(id) ON DELETE CASCADE;


--
-- Name: _PostToUser _PostToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$withoutCreatedAt; Owner: -
--

ALTER TABLE ONLY "schema-generator$withoutCreatedAt"."_PostToUser"
    ADD CONSTRAINT "_PostToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$withoutCreatedAt"."Post"(id) ON DELETE CASCADE;


--
-- Name: _PostToUser _PostToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$withoutCreatedAt; Owner: -
--

ALTER TABLE ONLY "schema-generator$withoutCreatedAt"."_PostToUser"
    ADD CONSTRAINT "_PostToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$withoutCreatedAt"."User"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

