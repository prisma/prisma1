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
-- Name: schema-generator$todo; Type: SCHEMA; Schema: -; Owner: prisma
--

CREATE SCHEMA "schema-generator$todo";


ALTER SCHEMA "schema-generator$todo" OWNER TO prisma;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Todo; Type: TABLE; Schema: schema-generator$todo; Owner: prisma
--

CREATE TABLE "schema-generator$todo"."Todo" (
    id character varying(25) NOT NULL,
    text text NOT NULL,
    done boolean NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$todo"."Todo" OWNER TO prisma;

--
-- Name: User; Type: TABLE; Schema: schema-generator$todo; Owner: prisma
--

CREATE TABLE "schema-generator$todo"."User" (
    id character varying(25) NOT NULL,
    name text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$todo"."User" OWNER TO prisma;

--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$todo; Owner: prisma
--

CREATE TABLE "schema-generator$todo"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$todo"."_RelayId" OWNER TO prisma;

--
-- Name: _TodoToUser; Type: TABLE; Schema: schema-generator$todo; Owner: prisma
--

CREATE TABLE "schema-generator$todo"."_TodoToUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$todo"."_TodoToUser" OWNER TO prisma;

--
-- Data for Name: Todo; Type: TABLE DATA; Schema: schema-generator$todo; Owner: prisma
--

COPY "schema-generator$todo"."Todo" (id, text, done, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: User; Type: TABLE DATA; Schema: schema-generator$todo; Owner: prisma
--

COPY "schema-generator$todo"."User" (id, name, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: _RelayId; Type: TABLE DATA; Schema: schema-generator$todo; Owner: prisma
--

COPY "schema-generator$todo"."_RelayId" (id, "stableModelIdentifier") FROM stdin;
\.


--
-- Data for Name: _TodoToUser; Type: TABLE DATA; Schema: schema-generator$todo; Owner: prisma
--

COPY "schema-generator$todo"."_TodoToUser" (id, "A", "B") FROM stdin;
\.


--
-- Name: Todo Todo_pkey; Type: CONSTRAINT; Schema: schema-generator$todo; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$todo"."Todo"
    ADD CONSTRAINT "Todo_pkey" PRIMARY KEY (id);


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: schema-generator$todo; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$todo"."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: _TodoToUser _TodoToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$todo; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$todo"."_TodoToUser"
    ADD CONSTRAINT "_TodoToUser_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$todo; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$todo"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _TodoToUser_A; Type: INDEX; Schema: schema-generator$todo; Owner: prisma
--

CREATE INDEX "_TodoToUser_A" ON "schema-generator$todo"."_TodoToUser" USING btree ("A");


--
-- Name: _TodoToUser_AB_unique; Type: INDEX; Schema: schema-generator$todo; Owner: prisma
--

CREATE UNIQUE INDEX "_TodoToUser_AB_unique" ON "schema-generator$todo"."_TodoToUser" USING btree ("A", "B");


--
-- Name: _TodoToUser_B; Type: INDEX; Schema: schema-generator$todo; Owner: prisma
--

CREATE INDEX "_TodoToUser_B" ON "schema-generator$todo"."_TodoToUser" USING btree ("B");


--
-- Name: _TodoToUser _TodoToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$todo; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$todo"."_TodoToUser"
    ADD CONSTRAINT "_TodoToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$todo"."Todo"(id) ON DELETE CASCADE;


--
-- Name: _TodoToUser _TodoToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$todo; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$todo"."_TodoToUser"
    ADD CONSTRAINT "_TodoToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$todo"."User"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

