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
-- Name: schema-generator$relations; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$relations";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: BillingInfo; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."BillingInfo" (
    id character varying(25) NOT NULL,
    account text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BillingInfoWithoutConnection; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."BillingInfoWithoutConnection" (
    id character varying(25) NOT NULL,
    account text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OptionalDetails; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."OptionalDetails" (
    id character varying(25) NOT NULL,
    text text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: OptionalDetailsWithoutConnection; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."OptionalDetailsWithoutConnection" (
    id character varying(25) NOT NULL,
    text text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Post; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."Post" (
    id character varying(25) NOT NULL,
    text text NOT NULL,
    count integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: PostWithoutConnection; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."PostWithoutConnection" (
    id character varying(25) NOT NULL,
    text text NOT NULL,
    count integer NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: User; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."User" (
    id character varying(25) NOT NULL,
    name text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _BillingInfoToUser; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_BillingInfoToUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _BillingInfoWithoutConnectionToUser; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_BillingInfoWithoutConnectionToUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _LikesByUser; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_LikesByUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _OptionalDetailsToUser; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_OptionalDetailsToUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _OptionalDetailsWithoutConnectionToUser; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_OptionalDetailsWithoutConnectionToUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _PostWithoutConnectionToUser; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_PostWithoutConnectionToUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _PostsByUser; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_PostsByUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$relations; Owner: -
--

CREATE TABLE "schema-generator$relations"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: BillingInfoWithoutConnection BillingInfoWithoutConnection_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."BillingInfoWithoutConnection"
    ADD CONSTRAINT "BillingInfoWithoutConnection_pkey" PRIMARY KEY (id);


--
-- Name: BillingInfo BillingInfo_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."BillingInfo"
    ADD CONSTRAINT "BillingInfo_pkey" PRIMARY KEY (id);


--
-- Name: OptionalDetailsWithoutConnection OptionalDetailsWithoutConnection_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."OptionalDetailsWithoutConnection"
    ADD CONSTRAINT "OptionalDetailsWithoutConnection_pkey" PRIMARY KEY (id);


--
-- Name: OptionalDetails OptionalDetails_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."OptionalDetails"
    ADD CONSTRAINT "OptionalDetails_pkey" PRIMARY KEY (id);


--
-- Name: PostWithoutConnection PostWithoutConnection_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."PostWithoutConnection"
    ADD CONSTRAINT "PostWithoutConnection_pkey" PRIMARY KEY (id);


--
-- Name: Post Post_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."Post"
    ADD CONSTRAINT "Post_pkey" PRIMARY KEY (id);


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: _BillingInfoToUser _BillingInfoToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_BillingInfoToUser"
    ADD CONSTRAINT "_BillingInfoToUser_pkey" PRIMARY KEY (id);


--
-- Name: _BillingInfoWithoutConnectionToUser _BillingInfoWithoutConnectionToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_BillingInfoWithoutConnectionToUser"
    ADD CONSTRAINT "_BillingInfoWithoutConnectionToUser_pkey" PRIMARY KEY (id);


--
-- Name: _LikesByUser _LikesByUser_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_LikesByUser"
    ADD CONSTRAINT "_LikesByUser_pkey" PRIMARY KEY (id);


--
-- Name: _OptionalDetailsToUser _OptionalDetailsToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_OptionalDetailsToUser"
    ADD CONSTRAINT "_OptionalDetailsToUser_pkey" PRIMARY KEY (id);


--
-- Name: _OptionalDetailsWithoutConnectionToUser _OptionalDetailsWithoutConnectionToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_OptionalDetailsWithoutConnectionToUser"
    ADD CONSTRAINT "_OptionalDetailsWithoutConnectionToUser_pkey" PRIMARY KEY (id);


--
-- Name: _PostWithoutConnectionToUser _PostWithoutConnectionToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_PostWithoutConnectionToUser"
    ADD CONSTRAINT "_PostWithoutConnectionToUser_pkey" PRIMARY KEY (id);


--
-- Name: _PostsByUser _PostsByUser_pkey; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_PostsByUser"
    ADD CONSTRAINT "_PostsByUser_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _BillingInfoToUser_A; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_BillingInfoToUser_A" ON "schema-generator$relations"."_BillingInfoToUser" USING btree ("A");


--
-- Name: _BillingInfoToUser_AB_unique; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE UNIQUE INDEX "_BillingInfoToUser_AB_unique" ON "schema-generator$relations"."_BillingInfoToUser" USING btree ("A", "B");


--
-- Name: _BillingInfoToUser_B; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_BillingInfoToUser_B" ON "schema-generator$relations"."_BillingInfoToUser" USING btree ("B");


--
-- Name: _BillingInfoWithoutConnectionToUser_A; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_BillingInfoWithoutConnectionToUser_A" ON "schema-generator$relations"."_BillingInfoWithoutConnectionToUser" USING btree ("A");


--
-- Name: _BillingInfoWithoutConnectionToUser_AB_unique; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE UNIQUE INDEX "_BillingInfoWithoutConnectionToUser_AB_unique" ON "schema-generator$relations"."_BillingInfoWithoutConnectionToUser" USING btree ("A", "B");


--
-- Name: _BillingInfoWithoutConnectionToUser_B; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_BillingInfoWithoutConnectionToUser_B" ON "schema-generator$relations"."_BillingInfoWithoutConnectionToUser" USING btree ("B");


--
-- Name: _LikesByUser_A; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_LikesByUser_A" ON "schema-generator$relations"."_LikesByUser" USING btree ("A");


--
-- Name: _LikesByUser_AB_unique; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE UNIQUE INDEX "_LikesByUser_AB_unique" ON "schema-generator$relations"."_LikesByUser" USING btree ("A", "B");


--
-- Name: _LikesByUser_B; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_LikesByUser_B" ON "schema-generator$relations"."_LikesByUser" USING btree ("B");


--
-- Name: _OptionalDetailsToUser_A; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_OptionalDetailsToUser_A" ON "schema-generator$relations"."_OptionalDetailsToUser" USING btree ("A");


--
-- Name: _OptionalDetailsToUser_AB_unique; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE UNIQUE INDEX "_OptionalDetailsToUser_AB_unique" ON "schema-generator$relations"."_OptionalDetailsToUser" USING btree ("A", "B");


--
-- Name: _OptionalDetailsToUser_B; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_OptionalDetailsToUser_B" ON "schema-generator$relations"."_OptionalDetailsToUser" USING btree ("B");


--
-- Name: _OptionalDetailsWithoutConnectionToUser_A; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_OptionalDetailsWithoutConnectionToUser_A" ON "schema-generator$relations"."_OptionalDetailsWithoutConnectionToUser" USING btree ("A");


--
-- Name: _OptionalDetailsWithoutConnectionToUser_AB_unique; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE UNIQUE INDEX "_OptionalDetailsWithoutConnectionToUser_AB_unique" ON "schema-generator$relations"."_OptionalDetailsWithoutConnectionToUser" USING btree ("A", "B");


--
-- Name: _OptionalDetailsWithoutConnectionToUser_B; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_OptionalDetailsWithoutConnectionToUser_B" ON "schema-generator$relations"."_OptionalDetailsWithoutConnectionToUser" USING btree ("B");


--
-- Name: _PostWithoutConnectionToUser_A; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_PostWithoutConnectionToUser_A" ON "schema-generator$relations"."_PostWithoutConnectionToUser" USING btree ("A");


--
-- Name: _PostWithoutConnectionToUser_AB_unique; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE UNIQUE INDEX "_PostWithoutConnectionToUser_AB_unique" ON "schema-generator$relations"."_PostWithoutConnectionToUser" USING btree ("A", "B");


--
-- Name: _PostWithoutConnectionToUser_B; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_PostWithoutConnectionToUser_B" ON "schema-generator$relations"."_PostWithoutConnectionToUser" USING btree ("B");


--
-- Name: _PostsByUser_A; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_PostsByUser_A" ON "schema-generator$relations"."_PostsByUser" USING btree ("A");


--
-- Name: _PostsByUser_AB_unique; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE UNIQUE INDEX "_PostsByUser_AB_unique" ON "schema-generator$relations"."_PostsByUser" USING btree ("A", "B");


--
-- Name: _PostsByUser_B; Type: INDEX; Schema: schema-generator$relations; Owner: -
--

CREATE INDEX "_PostsByUser_B" ON "schema-generator$relations"."_PostsByUser" USING btree ("B");


--
-- Name: _BillingInfoToUser _BillingInfoToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_BillingInfoToUser"
    ADD CONSTRAINT "_BillingInfoToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relations"."BillingInfo"(id) ON DELETE CASCADE;


--
-- Name: _BillingInfoToUser _BillingInfoToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_BillingInfoToUser"
    ADD CONSTRAINT "_BillingInfoToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relations"."User"(id) ON DELETE CASCADE;


--
-- Name: _BillingInfoWithoutConnectionToUser _BillingInfoWithoutConnectionToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_BillingInfoWithoutConnectionToUser"
    ADD CONSTRAINT "_BillingInfoWithoutConnectionToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relations"."BillingInfoWithoutConnection"(id) ON DELETE CASCADE;


--
-- Name: _BillingInfoWithoutConnectionToUser _BillingInfoWithoutConnectionToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_BillingInfoWithoutConnectionToUser"
    ADD CONSTRAINT "_BillingInfoWithoutConnectionToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relations"."User"(id) ON DELETE CASCADE;


--
-- Name: _LikesByUser _LikesByUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_LikesByUser"
    ADD CONSTRAINT "_LikesByUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relations"."Post"(id) ON DELETE CASCADE;


--
-- Name: _LikesByUser _LikesByUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_LikesByUser"
    ADD CONSTRAINT "_LikesByUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relations"."User"(id) ON DELETE CASCADE;


--
-- Name: _OptionalDetailsToUser _OptionalDetailsToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_OptionalDetailsToUser"
    ADD CONSTRAINT "_OptionalDetailsToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relations"."OptionalDetails"(id) ON DELETE CASCADE;


--
-- Name: _OptionalDetailsToUser _OptionalDetailsToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_OptionalDetailsToUser"
    ADD CONSTRAINT "_OptionalDetailsToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relations"."User"(id) ON DELETE CASCADE;


--
-- Name: _OptionalDetailsWithoutConnectionToUser _OptionalDetailsWithoutConnectionToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_OptionalDetailsWithoutConnectionToUser"
    ADD CONSTRAINT "_OptionalDetailsWithoutConnectionToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relations"."OptionalDetailsWithoutConnection"(id) ON DELETE CASCADE;


--
-- Name: _OptionalDetailsWithoutConnectionToUser _OptionalDetailsWithoutConnectionToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_OptionalDetailsWithoutConnectionToUser"
    ADD CONSTRAINT "_OptionalDetailsWithoutConnectionToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relations"."User"(id) ON DELETE CASCADE;


--
-- Name: _PostWithoutConnectionToUser _PostWithoutConnectionToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_PostWithoutConnectionToUser"
    ADD CONSTRAINT "_PostWithoutConnectionToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relations"."PostWithoutConnection"(id) ON DELETE CASCADE;


--
-- Name: _PostWithoutConnectionToUser _PostWithoutConnectionToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_PostWithoutConnectionToUser"
    ADD CONSTRAINT "_PostWithoutConnectionToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relations"."User"(id) ON DELETE CASCADE;


--
-- Name: _PostsByUser _PostsByUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_PostsByUser"
    ADD CONSTRAINT "_PostsByUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$relations"."Post"(id) ON DELETE CASCADE;


--
-- Name: _PostsByUser _PostsByUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations"."_PostsByUser"
    ADD CONSTRAINT "_PostsByUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$relations"."User"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

