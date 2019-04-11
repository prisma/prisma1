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
-- Name: schema-generator$financial; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$financial";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Campus; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."Campus" (
    id character varying(25) NOT NULL,
    description text,
    "isActive" boolean,
    name text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: FinancialAccount; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."FinancialAccount" (
    id character varying(25) NOT NULL,
    key text,
    description text NOT NULL,
    "isActive" boolean NOT NULL,
    name text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: FinancialPaymentDetail; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."FinancialPaymentDetail" (
    id character varying(25) NOT NULL,
    "accountNumberMasked" text NOT NULL,
    "creditCardType" text,
    "achType" text,
    "currencyType" text,
    "expirationDate" timestamp(3) without time zone NOT NULL,
    "nameOnCard" text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: FinancialScheduledTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."FinancialScheduledTransaction" (
    id character varying(25) NOT NULL,
    "endDate" timestamp(3) without time zone,
    "isActive" boolean NOT NULL,
    "startDate" timestamp(3) without time zone,
    frequency text,
    amount numeric(65,30) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: FinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."FinancialTransaction" (
    id character varying(25) NOT NULL,
    "processedDate" timestamp(3) without time zone,
    status text NOT NULL,
    "transactionDate" timestamp(3) without time zone,
    amount numeric(65,30) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Group; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."Group" (
    id character varying(25) NOT NULL,
    key text,
    description text,
    "isActive" boolean NOT NULL,
    name text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: GroupInvite; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."GroupInvite" (
    id character varying(25) NOT NULL,
    email text NOT NULL,
    status text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: GroupMember; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."GroupMember" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: GroupRole; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."GroupRole" (
    id character varying(25) NOT NULL,
    "canEdit" boolean NOT NULL,
    "canView" boolean NOT NULL,
    description text NOT NULL,
    "isLeader" boolean,
    name text NOT NULL,
    type text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: GroupType; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."GroupType" (
    id character varying(25) NOT NULL,
    description text,
    name text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Location; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."Location" (
    id character varying(25) NOT NULL,
    city text,
    "locationType" text,
    "postalCode" text,
    state text,
    street1 text,
    street2 text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Person; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."Person" (
    id character varying(25) NOT NULL,
    email text,
    "firstName" text,
    "lastName" text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: PhoneNumber; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."PhoneNumber" (
    id character varying(25) NOT NULL,
    number text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: User; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."User" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: _CampusToFinancialAccount; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_CampusToFinancialAccount" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CampusToGroup; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_CampusToGroup" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CampusToLocation; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_CampusToLocation" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _CampusToPhoneNumber; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_CampusToPhoneNumber" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialAccountToFinancialScheduledTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialAccountToFinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialAccountToFinancialTransaction" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialPaymentDetailToFinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialPaymentDetailToLocation; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialPaymentDetailToLocation" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialScheduledTransactionToPerson; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialScheduledTransactionToPerson" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialTransactionToGroup; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialTransactionToGroup" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _FinancialTransactionToPerson; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_FinancialTransactionToPerson" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupInviteToGroupRole; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupInviteToGroupRole" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupMemberToGroupRole; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupMemberToGroupRole" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupMemberToPerson; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupMemberToPerson" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupRoleToGroupType; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupRoleToGroupType" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupToGroup; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupToGroup" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupToGroupInvite; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupToGroupInvite" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupToGroupMember; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupToGroupMember" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _GroupToGroupType; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_GroupToGroupType" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _PersonToPhoneNumber; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_PersonToPhoneNumber" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _PersonToUser; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_PersonToUser" (
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$financial; Owner: -
--

CREATE TABLE "schema-generator$financial"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


--
-- Name: Campus Campus_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."Campus"
    ADD CONSTRAINT "Campus_pkey" PRIMARY KEY (id);


--
-- Name: FinancialAccount FinancialAccount_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialAccount"
    ADD CONSTRAINT "FinancialAccount_pkey" PRIMARY KEY (id);


--
-- Name: FinancialPaymentDetail FinancialPaymentDetail_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialPaymentDetail"
    ADD CONSTRAINT "FinancialPaymentDetail_pkey" PRIMARY KEY (id);


--
-- Name: FinancialScheduledTransaction FinancialScheduledTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialScheduledTransaction"
    ADD CONSTRAINT "FinancialScheduledTransaction_pkey" PRIMARY KEY (id);


--
-- Name: FinancialTransaction FinancialTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialTransaction"
    ADD CONSTRAINT "FinancialTransaction_pkey" PRIMARY KEY (id);


--
-- Name: GroupInvite GroupInvite_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."GroupInvite"
    ADD CONSTRAINT "GroupInvite_pkey" PRIMARY KEY (id);


--
-- Name: GroupMember GroupMember_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."GroupMember"
    ADD CONSTRAINT "GroupMember_pkey" PRIMARY KEY (id);


--
-- Name: GroupRole GroupRole_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."GroupRole"
    ADD CONSTRAINT "GroupRole_pkey" PRIMARY KEY (id);


--
-- Name: GroupType GroupType_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."GroupType"
    ADD CONSTRAINT "GroupType_pkey" PRIMARY KEY (id);


--
-- Name: Group Group_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."Group"
    ADD CONSTRAINT "Group_pkey" PRIMARY KEY (id);


--
-- Name: Location Location_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."Location"
    ADD CONSTRAINT "Location_pkey" PRIMARY KEY (id);


--
-- Name: Person Person_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."Person"
    ADD CONSTRAINT "Person_pkey" PRIMARY KEY (id);


--
-- Name: PhoneNumber PhoneNumber_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."PhoneNumber"
    ADD CONSTRAINT "PhoneNumber_pkey" PRIMARY KEY (id);


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _CampusToFinancialAccount_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToFinancialAccount_A" ON "schema-generator$financial"."_CampusToFinancialAccount" USING btree ("A");


--
-- Name: _CampusToFinancialAccount_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_CampusToFinancialAccount_AB_unique" ON "schema-generator$financial"."_CampusToFinancialAccount" USING btree ("A", "B");


--
-- Name: _CampusToFinancialAccount_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToFinancialAccount_B" ON "schema-generator$financial"."_CampusToFinancialAccount" USING btree ("B");


--
-- Name: _CampusToGroup_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToGroup_A" ON "schema-generator$financial"."_CampusToGroup" USING btree ("A");


--
-- Name: _CampusToGroup_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_CampusToGroup_AB_unique" ON "schema-generator$financial"."_CampusToGroup" USING btree ("A", "B");


--
-- Name: _CampusToGroup_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToGroup_B" ON "schema-generator$financial"."_CampusToGroup" USING btree ("B");


--
-- Name: _CampusToLocation_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToLocation_A" ON "schema-generator$financial"."_CampusToLocation" USING btree ("A");


--
-- Name: _CampusToLocation_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_CampusToLocation_AB_unique" ON "schema-generator$financial"."_CampusToLocation" USING btree ("A", "B");


--
-- Name: _CampusToLocation_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToLocation_B" ON "schema-generator$financial"."_CampusToLocation" USING btree ("B");


--
-- Name: _CampusToPhoneNumber_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToPhoneNumber_A" ON "schema-generator$financial"."_CampusToPhoneNumber" USING btree ("A");


--
-- Name: _CampusToPhoneNumber_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_CampusToPhoneNumber_AB_unique" ON "schema-generator$financial"."_CampusToPhoneNumber" USING btree ("A", "B");


--
-- Name: _CampusToPhoneNumber_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_CampusToPhoneNumber_B" ON "schema-generator$financial"."_CampusToPhoneNumber" USING btree ("B");


--
-- Name: _FinancialAccountToFinancialScheduledTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialAccountToFinancialScheduledTransaction_A" ON "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" USING btree ("A");


--
-- Name: _FinancialAccountToFinancialScheduledTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialAccountToFinancialScheduledTransaction_AB_unique" ON "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" USING btree ("A", "B");


--
-- Name: _FinancialAccountToFinancialScheduledTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialAccountToFinancialScheduledTransaction_B" ON "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" USING btree ("B");


--
-- Name: _FinancialAccountToFinancialTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialAccountToFinancialTransaction_A" ON "schema-generator$financial"."_FinancialAccountToFinancialTransaction" USING btree ("A");


--
-- Name: _FinancialAccountToFinancialTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialAccountToFinancialTransaction_AB_unique" ON "schema-generator$financial"."_FinancialAccountToFinancialTransaction" USING btree ("A", "B");


--
-- Name: _FinancialAccountToFinancialTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialAccountToFinancialTransaction_B" ON "schema-generator$financial"."_FinancialAccountToFinancialTransaction" USING btree ("B");


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialPaymentDetailToFinancialScheduledTransaction_A" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" USING btree ("A");


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction_AB_uniqu; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialPaymentDetailToFinancialScheduledTransaction_AB_uniqu" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" USING btree ("A", "B");


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialPaymentDetailToFinancialScheduledTransaction_B" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" USING btree ("B");


--
-- Name: _FinancialPaymentDetailToFinancialTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialPaymentDetailToFinancialTransaction_A" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" USING btree ("A");


--
-- Name: _FinancialPaymentDetailToFinancialTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialPaymentDetailToFinancialTransaction_AB_unique" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" USING btree ("A", "B");


--
-- Name: _FinancialPaymentDetailToFinancialTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialPaymentDetailToFinancialTransaction_B" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" USING btree ("B");


--
-- Name: _FinancialPaymentDetailToLocation_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialPaymentDetailToLocation_A" ON "schema-generator$financial"."_FinancialPaymentDetailToLocation" USING btree ("A");


--
-- Name: _FinancialPaymentDetailToLocation_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialPaymentDetailToLocation_AB_unique" ON "schema-generator$financial"."_FinancialPaymentDetailToLocation" USING btree ("A", "B");


--
-- Name: _FinancialPaymentDetailToLocation_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialPaymentDetailToLocation_B" ON "schema-generator$financial"."_FinancialPaymentDetailToLocation" USING btree ("B");


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialScheduledTransactionToFinancialTransaction_A" ON "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" USING btree ("A");


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialScheduledTransactionToFinancialTransaction_AB_unique" ON "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" USING btree ("A", "B");


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialScheduledTransactionToFinancialTransaction_B" ON "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" USING btree ("B");


--
-- Name: _FinancialScheduledTransactionToPerson_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialScheduledTransactionToPerson_A" ON "schema-generator$financial"."_FinancialScheduledTransactionToPerson" USING btree ("A");


--
-- Name: _FinancialScheduledTransactionToPerson_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialScheduledTransactionToPerson_AB_unique" ON "schema-generator$financial"."_FinancialScheduledTransactionToPerson" USING btree ("A", "B");


--
-- Name: _FinancialScheduledTransactionToPerson_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialScheduledTransactionToPerson_B" ON "schema-generator$financial"."_FinancialScheduledTransactionToPerson" USING btree ("B");


--
-- Name: _FinancialTransactionToGroup_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialTransactionToGroup_A" ON "schema-generator$financial"."_FinancialTransactionToGroup" USING btree ("A");


--
-- Name: _FinancialTransactionToGroup_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialTransactionToGroup_AB_unique" ON "schema-generator$financial"."_FinancialTransactionToGroup" USING btree ("A", "B");


--
-- Name: _FinancialTransactionToGroup_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialTransactionToGroup_B" ON "schema-generator$financial"."_FinancialTransactionToGroup" USING btree ("B");


--
-- Name: _FinancialTransactionToPerson_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialTransactionToPerson_A" ON "schema-generator$financial"."_FinancialTransactionToPerson" USING btree ("A");


--
-- Name: _FinancialTransactionToPerson_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_FinancialTransactionToPerson_AB_unique" ON "schema-generator$financial"."_FinancialTransactionToPerson" USING btree ("A", "B");


--
-- Name: _FinancialTransactionToPerson_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_FinancialTransactionToPerson_B" ON "schema-generator$financial"."_FinancialTransactionToPerson" USING btree ("B");


--
-- Name: _GroupInviteToGroupRole_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupInviteToGroupRole_A" ON "schema-generator$financial"."_GroupInviteToGroupRole" USING btree ("A");


--
-- Name: _GroupInviteToGroupRole_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupInviteToGroupRole_AB_unique" ON "schema-generator$financial"."_GroupInviteToGroupRole" USING btree ("A", "B");


--
-- Name: _GroupInviteToGroupRole_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupInviteToGroupRole_B" ON "schema-generator$financial"."_GroupInviteToGroupRole" USING btree ("B");


--
-- Name: _GroupMemberToGroupRole_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupMemberToGroupRole_A" ON "schema-generator$financial"."_GroupMemberToGroupRole" USING btree ("A");


--
-- Name: _GroupMemberToGroupRole_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupMemberToGroupRole_AB_unique" ON "schema-generator$financial"."_GroupMemberToGroupRole" USING btree ("A", "B");


--
-- Name: _GroupMemberToGroupRole_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupMemberToGroupRole_B" ON "schema-generator$financial"."_GroupMemberToGroupRole" USING btree ("B");


--
-- Name: _GroupMemberToPerson_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupMemberToPerson_A" ON "schema-generator$financial"."_GroupMemberToPerson" USING btree ("A");


--
-- Name: _GroupMemberToPerson_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupMemberToPerson_AB_unique" ON "schema-generator$financial"."_GroupMemberToPerson" USING btree ("A", "B");


--
-- Name: _GroupMemberToPerson_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupMemberToPerson_B" ON "schema-generator$financial"."_GroupMemberToPerson" USING btree ("B");


--
-- Name: _GroupRoleToGroupType_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupRoleToGroupType_A" ON "schema-generator$financial"."_GroupRoleToGroupType" USING btree ("A");


--
-- Name: _GroupRoleToGroupType_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupRoleToGroupType_AB_unique" ON "schema-generator$financial"."_GroupRoleToGroupType" USING btree ("A", "B");


--
-- Name: _GroupRoleToGroupType_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupRoleToGroupType_B" ON "schema-generator$financial"."_GroupRoleToGroupType" USING btree ("B");


--
-- Name: _GroupToGroupInvite_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroupInvite_A" ON "schema-generator$financial"."_GroupToGroupInvite" USING btree ("A");


--
-- Name: _GroupToGroupInvite_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupToGroupInvite_AB_unique" ON "schema-generator$financial"."_GroupToGroupInvite" USING btree ("A", "B");


--
-- Name: _GroupToGroupInvite_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroupInvite_B" ON "schema-generator$financial"."_GroupToGroupInvite" USING btree ("B");


--
-- Name: _GroupToGroupMember_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroupMember_A" ON "schema-generator$financial"."_GroupToGroupMember" USING btree ("A");


--
-- Name: _GroupToGroupMember_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupToGroupMember_AB_unique" ON "schema-generator$financial"."_GroupToGroupMember" USING btree ("A", "B");


--
-- Name: _GroupToGroupMember_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroupMember_B" ON "schema-generator$financial"."_GroupToGroupMember" USING btree ("B");


--
-- Name: _GroupToGroupType_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroupType_A" ON "schema-generator$financial"."_GroupToGroupType" USING btree ("A");


--
-- Name: _GroupToGroupType_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupToGroupType_AB_unique" ON "schema-generator$financial"."_GroupToGroupType" USING btree ("A", "B");


--
-- Name: _GroupToGroupType_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroupType_B" ON "schema-generator$financial"."_GroupToGroupType" USING btree ("B");


--
-- Name: _GroupToGroup_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroup_A" ON "schema-generator$financial"."_GroupToGroup" USING btree ("A");


--
-- Name: _GroupToGroup_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_GroupToGroup_AB_unique" ON "schema-generator$financial"."_GroupToGroup" USING btree ("A", "B");


--
-- Name: _GroupToGroup_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_GroupToGroup_B" ON "schema-generator$financial"."_GroupToGroup" USING btree ("B");


--
-- Name: _PersonToPhoneNumber_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_PersonToPhoneNumber_A" ON "schema-generator$financial"."_PersonToPhoneNumber" USING btree ("A");


--
-- Name: _PersonToPhoneNumber_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_PersonToPhoneNumber_AB_unique" ON "schema-generator$financial"."_PersonToPhoneNumber" USING btree ("A", "B");


--
-- Name: _PersonToPhoneNumber_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_PersonToPhoneNumber_B" ON "schema-generator$financial"."_PersonToPhoneNumber" USING btree ("B");


--
-- Name: _PersonToUser_A; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_PersonToUser_A" ON "schema-generator$financial"."_PersonToUser" USING btree ("A");


--
-- Name: _PersonToUser_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "_PersonToUser_AB_unique" ON "schema-generator$financial"."_PersonToUser" USING btree ("A", "B");


--
-- Name: _PersonToUser_B; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE INDEX "_PersonToUser_B" ON "schema-generator$financial"."_PersonToUser" USING btree ("B");


--
-- Name: schema-generator$financial.FinancialAccount.key._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$financial.FinancialAccount.key._UNIQUE" ON "schema-generator$financial"."FinancialAccount" USING btree (key);


--
-- Name: schema-generator$financial.Group.key._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$financial.Group.key._UNIQUE" ON "schema-generator$financial"."Group" USING btree (key);


--
-- Name: schema-generator$financial.GroupRole.name._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$financial.GroupRole.name._UNIQUE" ON "schema-generator$financial"."GroupRole" USING btree (name);


--
-- Name: schema-generator$financial.GroupType.name._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$financial.GroupType.name._UNIQUE" ON "schema-generator$financial"."GroupType" USING btree (name);


--
-- Name: _CampusToFinancialAccount _CampusToFinancialAccount_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToFinancialAccount"
    ADD CONSTRAINT "_CampusToFinancialAccount_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToFinancialAccount _CampusToFinancialAccount_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToFinancialAccount"
    ADD CONSTRAINT "_CampusToFinancialAccount_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialAccount"(id) ON DELETE CASCADE;


--
-- Name: _CampusToGroup _CampusToGroup_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToGroup"
    ADD CONSTRAINT "_CampusToGroup_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToGroup _CampusToGroup_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToGroup"
    ADD CONSTRAINT "_CampusToGroup_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _CampusToLocation _CampusToLocation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToLocation"
    ADD CONSTRAINT "_CampusToLocation_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToLocation _CampusToLocation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToLocation"
    ADD CONSTRAINT "_CampusToLocation_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Location"(id) ON DELETE CASCADE;


--
-- Name: _CampusToPhoneNumber _CampusToPhoneNumber_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToPhoneNumber"
    ADD CONSTRAINT "_CampusToPhoneNumber_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToPhoneNumber _CampusToPhoneNumber_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToPhoneNumber"
    ADD CONSTRAINT "_CampusToPhoneNumber_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."PhoneNumber"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialScheduledTransaction _FinancialAccountToFinancialScheduledTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialScheduledTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialAccount"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialScheduledTransaction _FinancialAccountToFinancialScheduledTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialScheduledTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialTransaction _FinancialAccountToFinancialTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialAccount"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialTransaction _FinancialAccountToFinancialTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction _FinancialPaymentDetailToFinancialScheduledTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialScheduledTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialPaymentDetail"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction _FinancialPaymentDetailToFinancialScheduledTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialScheduledTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialTransaction _FinancialPaymentDetailToFinancialTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialPaymentDetail"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialTransaction _FinancialPaymentDetailToFinancialTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToLocation _FinancialPaymentDetailToLocation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToLocation"
    ADD CONSTRAINT "_FinancialPaymentDetailToLocation_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialPaymentDetail"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToLocation _FinancialPaymentDetailToLocation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToLocation"
    ADD CONSTRAINT "_FinancialPaymentDetailToLocation_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Location"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction _FinancialScheduledTransactionToFinancialTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction"
    ADD CONSTRAINT "_FinancialScheduledTransactionToFinancialTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction _FinancialScheduledTransactionToFinancialTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction"
    ADD CONSTRAINT "_FinancialScheduledTransactionToFinancialTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToPerson _FinancialScheduledTransactionToPerson_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToPerson"
    ADD CONSTRAINT "_FinancialScheduledTransactionToPerson_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToPerson _FinancialScheduledTransactionToPerson_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToPerson"
    ADD CONSTRAINT "_FinancialScheduledTransactionToPerson_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToGroup _FinancialTransactionToGroup_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToGroup"
    ADD CONSTRAINT "_FinancialTransactionToGroup_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToGroup _FinancialTransactionToGroup_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToGroup"
    ADD CONSTRAINT "_FinancialTransactionToGroup_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToPerson _FinancialTransactionToPerson_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToPerson"
    ADD CONSTRAINT "_FinancialTransactionToPerson_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToPerson _FinancialTransactionToPerson_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToPerson"
    ADD CONSTRAINT "_FinancialTransactionToPerson_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _GroupInviteToGroupRole _GroupInviteToGroupRole_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupInviteToGroupRole"
    ADD CONSTRAINT "_GroupInviteToGroupRole_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupInvite"(id) ON DELETE CASCADE;


--
-- Name: _GroupInviteToGroupRole _GroupInviteToGroupRole_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupInviteToGroupRole"
    ADD CONSTRAINT "_GroupInviteToGroupRole_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupRole"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToGroupRole _GroupMemberToGroupRole_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToGroupRole"
    ADD CONSTRAINT "_GroupMemberToGroupRole_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupMember"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToGroupRole _GroupMemberToGroupRole_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToGroupRole"
    ADD CONSTRAINT "_GroupMemberToGroupRole_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupRole"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToPerson _GroupMemberToPerson_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToPerson"
    ADD CONSTRAINT "_GroupMemberToPerson_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupMember"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToPerson _GroupMemberToPerson_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToPerson"
    ADD CONSTRAINT "_GroupMemberToPerson_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _GroupRoleToGroupType _GroupRoleToGroupType_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupRoleToGroupType"
    ADD CONSTRAINT "_GroupRoleToGroupType_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupRole"(id) ON DELETE CASCADE;


--
-- Name: _GroupRoleToGroupType _GroupRoleToGroupType_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupRoleToGroupType"
    ADD CONSTRAINT "_GroupRoleToGroupType_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupType"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupInvite _GroupToGroupInvite_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupInvite"
    ADD CONSTRAINT "_GroupToGroupInvite_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupInvite _GroupToGroupInvite_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupInvite"
    ADD CONSTRAINT "_GroupToGroupInvite_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupInvite"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupMember _GroupToGroupMember_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupMember"
    ADD CONSTRAINT "_GroupToGroupMember_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupMember _GroupToGroupMember_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupMember"
    ADD CONSTRAINT "_GroupToGroupMember_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupMember"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupType _GroupToGroupType_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupType"
    ADD CONSTRAINT "_GroupToGroupType_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupType _GroupToGroupType_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupType"
    ADD CONSTRAINT "_GroupToGroupType_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupType"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroup _GroupToGroup_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroup"
    ADD CONSTRAINT "_GroupToGroup_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroup _GroupToGroup_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroup"
    ADD CONSTRAINT "_GroupToGroup_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _PersonToPhoneNumber _PersonToPhoneNumber_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToPhoneNumber"
    ADD CONSTRAINT "_PersonToPhoneNumber_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _PersonToPhoneNumber _PersonToPhoneNumber_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToPhoneNumber"
    ADD CONSTRAINT "_PersonToPhoneNumber_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."PhoneNumber"(id) ON DELETE CASCADE;


--
-- Name: _PersonToUser _PersonToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToUser"
    ADD CONSTRAINT "_PersonToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _PersonToUser _PersonToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: -
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToUser"
    ADD CONSTRAINT "_PersonToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."User"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

