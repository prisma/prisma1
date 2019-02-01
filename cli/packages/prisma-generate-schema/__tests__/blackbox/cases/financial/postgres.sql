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
-- Name: schema-generator$financial; Type: SCHEMA; Schema: -; Owner: prisma
--

CREATE SCHEMA "schema-generator$financial";


ALTER SCHEMA "schema-generator$financial" OWNER TO prisma;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Campus; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."Campus" (
    id character varying(25) NOT NULL,
    description text,
    "isActive" boolean,
    name text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$financial"."Campus" OWNER TO prisma;

--
-- Name: FinancialAccount; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
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


ALTER TABLE "schema-generator$financial"."FinancialAccount" OWNER TO prisma;

--
-- Name: FinancialPaymentDetail; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
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


ALTER TABLE "schema-generator$financial"."FinancialPaymentDetail" OWNER TO prisma;

--
-- Name: FinancialScheduledTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
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


ALTER TABLE "schema-generator$financial"."FinancialScheduledTransaction" OWNER TO prisma;

--
-- Name: FinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
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


ALTER TABLE "schema-generator$financial"."FinancialTransaction" OWNER TO prisma;

--
-- Name: Group; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
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


ALTER TABLE "schema-generator$financial"."Group" OWNER TO prisma;

--
-- Name: GroupInvite; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."GroupInvite" (
    id character varying(25) NOT NULL,
    email text NOT NULL,
    status text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$financial"."GroupInvite" OWNER TO prisma;

--
-- Name: GroupMember; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."GroupMember" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$financial"."GroupMember" OWNER TO prisma;

--
-- Name: GroupRole; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
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


ALTER TABLE "schema-generator$financial"."GroupRole" OWNER TO prisma;

--
-- Name: GroupType; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."GroupType" (
    id character varying(25) NOT NULL,
    description text,
    name text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$financial"."GroupType" OWNER TO prisma;

--
-- Name: Location; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
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


ALTER TABLE "schema-generator$financial"."Location" OWNER TO prisma;

--
-- Name: Person; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."Person" (
    id character varying(25) NOT NULL,
    email text,
    "firstName" text,
    "lastName" text,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$financial"."Person" OWNER TO prisma;

--
-- Name: PhoneNumber; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."PhoneNumber" (
    id character varying(25) NOT NULL,
    number text NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$financial"."PhoneNumber" OWNER TO prisma;

--
-- Name: User; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."User" (
    id character varying(25) NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL
);


ALTER TABLE "schema-generator$financial"."User" OWNER TO prisma;

--
-- Name: _CampusToFinancialAccount; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_CampusToFinancialAccount" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_CampusToFinancialAccount" OWNER TO prisma;

--
-- Name: _CampusToGroup; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_CampusToGroup" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_CampusToGroup" OWNER TO prisma;

--
-- Name: _CampusToLocation; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_CampusToLocation" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_CampusToLocation" OWNER TO prisma;

--
-- Name: _CampusToPhoneNumber; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_CampusToPhoneNumber" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_CampusToPhoneNumber" OWNER TO prisma;

--
-- Name: _FinancialAccountToFinancialScheduledTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" OWNER TO prisma;

--
-- Name: _FinancialAccountToFinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialAccountToFinancialTransaction" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialAccountToFinancialTransaction" OWNER TO prisma;

--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" OWNER TO prisma;

--
-- Name: _FinancialPaymentDetailToFinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" OWNER TO prisma;

--
-- Name: _FinancialPaymentDetailToLocation; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialPaymentDetailToLocation" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialPaymentDetailToLocation" OWNER TO prisma;

--
-- Name: _FinancialScheduledTransactionToFinancialTransaction; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" OWNER TO prisma;

--
-- Name: _FinancialScheduledTransactionToPerson; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialScheduledTransactionToPerson" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialScheduledTransactionToPerson" OWNER TO prisma;

--
-- Name: _FinancialTransactionToGroup; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialTransactionToGroup" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialTransactionToGroup" OWNER TO prisma;

--
-- Name: _FinancialTransactionToPerson; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_FinancialTransactionToPerson" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_FinancialTransactionToPerson" OWNER TO prisma;

--
-- Name: _GroupInviteToGroupRole; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupInviteToGroupRole" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupInviteToGroupRole" OWNER TO prisma;

--
-- Name: _GroupMemberToGroupRole; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupMemberToGroupRole" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupMemberToGroupRole" OWNER TO prisma;

--
-- Name: _GroupMemberToPerson; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupMemberToPerson" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupMemberToPerson" OWNER TO prisma;

--
-- Name: _GroupRoleToGroupType; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupRoleToGroupType" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupRoleToGroupType" OWNER TO prisma;

--
-- Name: _GroupToGroup; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupToGroup" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupToGroup" OWNER TO prisma;

--
-- Name: _GroupToGroupInvite; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupToGroupInvite" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupToGroupInvite" OWNER TO prisma;

--
-- Name: _GroupToGroupMember; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupToGroupMember" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupToGroupMember" OWNER TO prisma;

--
-- Name: _GroupToGroupType; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_GroupToGroupType" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_GroupToGroupType" OWNER TO prisma;

--
-- Name: _PersonToPhoneNumber; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_PersonToPhoneNumber" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_PersonToPhoneNumber" OWNER TO prisma;

--
-- Name: _PersonToUser; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_PersonToUser" (
    id character(25) NOT NULL,
    "A" character varying(25) NOT NULL,
    "B" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_PersonToUser" OWNER TO prisma;

--
-- Name: _RelayId; Type: TABLE; Schema: schema-generator$financial; Owner: prisma
--

CREATE TABLE "schema-generator$financial"."_RelayId" (
    id character varying(36) NOT NULL,
    "stableModelIdentifier" character varying(25) NOT NULL
);


ALTER TABLE "schema-generator$financial"."_RelayId" OWNER TO prisma;

--
-- Data for Name: Campus; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."Campus" (id, description, "isActive", name, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: FinancialAccount; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."FinancialAccount" (id, key, description, "isActive", name, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: FinancialPaymentDetail; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."FinancialPaymentDetail" (id, "accountNumberMasked", "creditCardType", "achType", "currencyType", "expirationDate", "nameOnCard", "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: FinancialScheduledTransaction; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."FinancialScheduledTransaction" (id, "endDate", "isActive", "startDate", frequency, amount, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: FinancialTransaction; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."FinancialTransaction" (id, "processedDate", status, "transactionDate", amount, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: Group; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."Group" (id, key, description, "isActive", name, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: GroupInvite; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."GroupInvite" (id, email, status, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: GroupMember; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."GroupMember" (id, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: GroupRole; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."GroupRole" (id, "canEdit", "canView", description, "isLeader", name, type, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: GroupType; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."GroupType" (id, description, name, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: Location; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."Location" (id, city, "locationType", "postalCode", state, street1, street2, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: Person; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."Person" (id, email, "firstName", "lastName", "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: PhoneNumber; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."PhoneNumber" (id, number, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: User; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."User" (id, "updatedAt", "createdAt") FROM stdin;
\.


--
-- Data for Name: _CampusToFinancialAccount; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_CampusToFinancialAccount" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _CampusToGroup; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_CampusToGroup" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _CampusToLocation; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_CampusToLocation" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _CampusToPhoneNumber; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_CampusToPhoneNumber" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialAccountToFinancialScheduledTransaction; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialAccountToFinancialTransaction; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialAccountToFinancialTransaction" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialPaymentDetailToFinancialScheduledTransaction; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialPaymentDetailToFinancialTransaction; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialPaymentDetailToLocation; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialPaymentDetailToLocation" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialScheduledTransactionToFinancialTransaction; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialScheduledTransactionToPerson; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialScheduledTransactionToPerson" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialTransactionToGroup; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialTransactionToGroup" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _FinancialTransactionToPerson; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_FinancialTransactionToPerson" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupInviteToGroupRole; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupInviteToGroupRole" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupMemberToGroupRole; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupMemberToGroupRole" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupMemberToPerson; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupMemberToPerson" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupRoleToGroupType; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupRoleToGroupType" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupToGroup; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupToGroup" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupToGroupInvite; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupToGroupInvite" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupToGroupMember; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupToGroupMember" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _GroupToGroupType; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_GroupToGroupType" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _PersonToPhoneNumber; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_PersonToPhoneNumber" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _PersonToUser; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_PersonToUser" (id, "A", "B") FROM stdin;
\.


--
-- Data for Name: _RelayId; Type: TABLE DATA; Schema: schema-generator$financial; Owner: prisma
--

COPY "schema-generator$financial"."_RelayId" (id, "stableModelIdentifier") FROM stdin;
\.


--
-- Name: Campus Campus_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."Campus"
    ADD CONSTRAINT "Campus_pkey" PRIMARY KEY (id);


--
-- Name: FinancialAccount FinancialAccount_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialAccount"
    ADD CONSTRAINT "FinancialAccount_pkey" PRIMARY KEY (id);


--
-- Name: FinancialPaymentDetail FinancialPaymentDetail_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialPaymentDetail"
    ADD CONSTRAINT "FinancialPaymentDetail_pkey" PRIMARY KEY (id);


--
-- Name: FinancialScheduledTransaction FinancialScheduledTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialScheduledTransaction"
    ADD CONSTRAINT "FinancialScheduledTransaction_pkey" PRIMARY KEY (id);


--
-- Name: FinancialTransaction FinancialTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."FinancialTransaction"
    ADD CONSTRAINT "FinancialTransaction_pkey" PRIMARY KEY (id);


--
-- Name: GroupInvite GroupInvite_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."GroupInvite"
    ADD CONSTRAINT "GroupInvite_pkey" PRIMARY KEY (id);


--
-- Name: GroupMember GroupMember_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."GroupMember"
    ADD CONSTRAINT "GroupMember_pkey" PRIMARY KEY (id);


--
-- Name: GroupRole GroupRole_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."GroupRole"
    ADD CONSTRAINT "GroupRole_pkey" PRIMARY KEY (id);


--
-- Name: GroupType GroupType_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."GroupType"
    ADD CONSTRAINT "GroupType_pkey" PRIMARY KEY (id);


--
-- Name: Group Group_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."Group"
    ADD CONSTRAINT "Group_pkey" PRIMARY KEY (id);


--
-- Name: Location Location_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."Location"
    ADD CONSTRAINT "Location_pkey" PRIMARY KEY (id);


--
-- Name: Person Person_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."Person"
    ADD CONSTRAINT "Person_pkey" PRIMARY KEY (id);


--
-- Name: PhoneNumber PhoneNumber_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."PhoneNumber"
    ADD CONSTRAINT "PhoneNumber_pkey" PRIMARY KEY (id);


--
-- Name: User User_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."User"
    ADD CONSTRAINT "User_pkey" PRIMARY KEY (id);


--
-- Name: _CampusToFinancialAccount _CampusToFinancialAccount_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToFinancialAccount"
    ADD CONSTRAINT "_CampusToFinancialAccount_pkey" PRIMARY KEY (id);


--
-- Name: _CampusToGroup _CampusToGroup_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToGroup"
    ADD CONSTRAINT "_CampusToGroup_pkey" PRIMARY KEY (id);


--
-- Name: _CampusToLocation _CampusToLocation_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToLocation"
    ADD CONSTRAINT "_CampusToLocation_pkey" PRIMARY KEY (id);


--
-- Name: _CampusToPhoneNumber _CampusToPhoneNumber_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToPhoneNumber"
    ADD CONSTRAINT "_CampusToPhoneNumber_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialAccountToFinancialScheduledTransaction _FinancialAccountToFinancialScheduledTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialScheduledTransaction_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialAccountToFinancialTransaction _FinancialAccountToFinancialTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialTransaction_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction _FinancialPaymentDetailToFinancialScheduledTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialScheduledTransaction_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialPaymentDetailToFinancialTransaction _FinancialPaymentDetailToFinancialTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialTransaction_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialPaymentDetailToLocation _FinancialPaymentDetailToLocation_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToLocation"
    ADD CONSTRAINT "_FinancialPaymentDetailToLocation_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction _FinancialScheduledTransactionToFinancialTransaction_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction"
    ADD CONSTRAINT "_FinancialScheduledTransactionToFinancialTransaction_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialScheduledTransactionToPerson _FinancialScheduledTransactionToPerson_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToPerson"
    ADD CONSTRAINT "_FinancialScheduledTransactionToPerson_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialTransactionToGroup _FinancialTransactionToGroup_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToGroup"
    ADD CONSTRAINT "_FinancialTransactionToGroup_pkey" PRIMARY KEY (id);


--
-- Name: _FinancialTransactionToPerson _FinancialTransactionToPerson_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToPerson"
    ADD CONSTRAINT "_FinancialTransactionToPerson_pkey" PRIMARY KEY (id);


--
-- Name: _GroupInviteToGroupRole _GroupInviteToGroupRole_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupInviteToGroupRole"
    ADD CONSTRAINT "_GroupInviteToGroupRole_pkey" PRIMARY KEY (id);


--
-- Name: _GroupMemberToGroupRole _GroupMemberToGroupRole_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToGroupRole"
    ADD CONSTRAINT "_GroupMemberToGroupRole_pkey" PRIMARY KEY (id);


--
-- Name: _GroupMemberToPerson _GroupMemberToPerson_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToPerson"
    ADD CONSTRAINT "_GroupMemberToPerson_pkey" PRIMARY KEY (id);


--
-- Name: _GroupRoleToGroupType _GroupRoleToGroupType_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupRoleToGroupType"
    ADD CONSTRAINT "_GroupRoleToGroupType_pkey" PRIMARY KEY (id);


--
-- Name: _GroupToGroupInvite _GroupToGroupInvite_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupInvite"
    ADD CONSTRAINT "_GroupToGroupInvite_pkey" PRIMARY KEY (id);


--
-- Name: _GroupToGroupMember _GroupToGroupMember_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupMember"
    ADD CONSTRAINT "_GroupToGroupMember_pkey" PRIMARY KEY (id);


--
-- Name: _GroupToGroupType _GroupToGroupType_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupType"
    ADD CONSTRAINT "_GroupToGroupType_pkey" PRIMARY KEY (id);


--
-- Name: _GroupToGroup _GroupToGroup_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroup"
    ADD CONSTRAINT "_GroupToGroup_pkey" PRIMARY KEY (id);


--
-- Name: _PersonToPhoneNumber _PersonToPhoneNumber_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToPhoneNumber"
    ADD CONSTRAINT "_PersonToPhoneNumber_pkey" PRIMARY KEY (id);


--
-- Name: _PersonToUser _PersonToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToUser"
    ADD CONSTRAINT "_PersonToUser_pkey" PRIMARY KEY (id);


--
-- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_RelayId"
    ADD CONSTRAINT "pk_RelayId" PRIMARY KEY (id);


--
-- Name: _CampusToFinancialAccount_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToFinancialAccount_A" ON "schema-generator$financial"."_CampusToFinancialAccount" USING btree ("A");


--
-- Name: _CampusToFinancialAccount_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_CampusToFinancialAccount_AB_unique" ON "schema-generator$financial"."_CampusToFinancialAccount" USING btree ("A", "B");


--
-- Name: _CampusToFinancialAccount_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToFinancialAccount_B" ON "schema-generator$financial"."_CampusToFinancialAccount" USING btree ("B");


--
-- Name: _CampusToGroup_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToGroup_A" ON "schema-generator$financial"."_CampusToGroup" USING btree ("A");


--
-- Name: _CampusToGroup_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_CampusToGroup_AB_unique" ON "schema-generator$financial"."_CampusToGroup" USING btree ("A", "B");


--
-- Name: _CampusToGroup_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToGroup_B" ON "schema-generator$financial"."_CampusToGroup" USING btree ("B");


--
-- Name: _CampusToLocation_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToLocation_A" ON "schema-generator$financial"."_CampusToLocation" USING btree ("A");


--
-- Name: _CampusToLocation_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_CampusToLocation_AB_unique" ON "schema-generator$financial"."_CampusToLocation" USING btree ("A", "B");


--
-- Name: _CampusToLocation_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToLocation_B" ON "schema-generator$financial"."_CampusToLocation" USING btree ("B");


--
-- Name: _CampusToPhoneNumber_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToPhoneNumber_A" ON "schema-generator$financial"."_CampusToPhoneNumber" USING btree ("A");


--
-- Name: _CampusToPhoneNumber_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_CampusToPhoneNumber_AB_unique" ON "schema-generator$financial"."_CampusToPhoneNumber" USING btree ("A", "B");


--
-- Name: _CampusToPhoneNumber_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_CampusToPhoneNumber_B" ON "schema-generator$financial"."_CampusToPhoneNumber" USING btree ("B");


--
-- Name: _FinancialAccountToFinancialScheduledTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialAccountToFinancialScheduledTransaction_A" ON "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" USING btree ("A");


--
-- Name: _FinancialAccountToFinancialScheduledTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialAccountToFinancialScheduledTransaction_AB_unique" ON "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" USING btree ("A", "B");


--
-- Name: _FinancialAccountToFinancialScheduledTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialAccountToFinancialScheduledTransaction_B" ON "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction" USING btree ("B");


--
-- Name: _FinancialAccountToFinancialTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialAccountToFinancialTransaction_A" ON "schema-generator$financial"."_FinancialAccountToFinancialTransaction" USING btree ("A");


--
-- Name: _FinancialAccountToFinancialTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialAccountToFinancialTransaction_AB_unique" ON "schema-generator$financial"."_FinancialAccountToFinancialTransaction" USING btree ("A", "B");


--
-- Name: _FinancialAccountToFinancialTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialAccountToFinancialTransaction_B" ON "schema-generator$financial"."_FinancialAccountToFinancialTransaction" USING btree ("B");


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialPaymentDetailToFinancialScheduledTransaction_A" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" USING btree ("A");


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction_AB_uniqu; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialPaymentDetailToFinancialScheduledTransaction_AB_uniqu" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" USING btree ("A", "B");


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialPaymentDetailToFinancialScheduledTransaction_B" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction" USING btree ("B");


--
-- Name: _FinancialPaymentDetailToFinancialTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialPaymentDetailToFinancialTransaction_A" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" USING btree ("A");


--
-- Name: _FinancialPaymentDetailToFinancialTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialPaymentDetailToFinancialTransaction_AB_unique" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" USING btree ("A", "B");


--
-- Name: _FinancialPaymentDetailToFinancialTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialPaymentDetailToFinancialTransaction_B" ON "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction" USING btree ("B");


--
-- Name: _FinancialPaymentDetailToLocation_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialPaymentDetailToLocation_A" ON "schema-generator$financial"."_FinancialPaymentDetailToLocation" USING btree ("A");


--
-- Name: _FinancialPaymentDetailToLocation_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialPaymentDetailToLocation_AB_unique" ON "schema-generator$financial"."_FinancialPaymentDetailToLocation" USING btree ("A", "B");


--
-- Name: _FinancialPaymentDetailToLocation_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialPaymentDetailToLocation_B" ON "schema-generator$financial"."_FinancialPaymentDetailToLocation" USING btree ("B");


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialScheduledTransactionToFinancialTransaction_A" ON "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" USING btree ("A");


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialScheduledTransactionToFinancialTransaction_AB_unique" ON "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" USING btree ("A", "B");


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialScheduledTransactionToFinancialTransaction_B" ON "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction" USING btree ("B");


--
-- Name: _FinancialScheduledTransactionToPerson_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialScheduledTransactionToPerson_A" ON "schema-generator$financial"."_FinancialScheduledTransactionToPerson" USING btree ("A");


--
-- Name: _FinancialScheduledTransactionToPerson_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialScheduledTransactionToPerson_AB_unique" ON "schema-generator$financial"."_FinancialScheduledTransactionToPerson" USING btree ("A", "B");


--
-- Name: _FinancialScheduledTransactionToPerson_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialScheduledTransactionToPerson_B" ON "schema-generator$financial"."_FinancialScheduledTransactionToPerson" USING btree ("B");


--
-- Name: _FinancialTransactionToGroup_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialTransactionToGroup_A" ON "schema-generator$financial"."_FinancialTransactionToGroup" USING btree ("A");


--
-- Name: _FinancialTransactionToGroup_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialTransactionToGroup_AB_unique" ON "schema-generator$financial"."_FinancialTransactionToGroup" USING btree ("A", "B");


--
-- Name: _FinancialTransactionToGroup_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialTransactionToGroup_B" ON "schema-generator$financial"."_FinancialTransactionToGroup" USING btree ("B");


--
-- Name: _FinancialTransactionToPerson_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialTransactionToPerson_A" ON "schema-generator$financial"."_FinancialTransactionToPerson" USING btree ("A");


--
-- Name: _FinancialTransactionToPerson_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_FinancialTransactionToPerson_AB_unique" ON "schema-generator$financial"."_FinancialTransactionToPerson" USING btree ("A", "B");


--
-- Name: _FinancialTransactionToPerson_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_FinancialTransactionToPerson_B" ON "schema-generator$financial"."_FinancialTransactionToPerson" USING btree ("B");


--
-- Name: _GroupInviteToGroupRole_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupInviteToGroupRole_A" ON "schema-generator$financial"."_GroupInviteToGroupRole" USING btree ("A");


--
-- Name: _GroupInviteToGroupRole_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupInviteToGroupRole_AB_unique" ON "schema-generator$financial"."_GroupInviteToGroupRole" USING btree ("A", "B");


--
-- Name: _GroupInviteToGroupRole_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupInviteToGroupRole_B" ON "schema-generator$financial"."_GroupInviteToGroupRole" USING btree ("B");


--
-- Name: _GroupMemberToGroupRole_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupMemberToGroupRole_A" ON "schema-generator$financial"."_GroupMemberToGroupRole" USING btree ("A");


--
-- Name: _GroupMemberToGroupRole_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupMemberToGroupRole_AB_unique" ON "schema-generator$financial"."_GroupMemberToGroupRole" USING btree ("A", "B");


--
-- Name: _GroupMemberToGroupRole_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupMemberToGroupRole_B" ON "schema-generator$financial"."_GroupMemberToGroupRole" USING btree ("B");


--
-- Name: _GroupMemberToPerson_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupMemberToPerson_A" ON "schema-generator$financial"."_GroupMemberToPerson" USING btree ("A");


--
-- Name: _GroupMemberToPerson_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupMemberToPerson_AB_unique" ON "schema-generator$financial"."_GroupMemberToPerson" USING btree ("A", "B");


--
-- Name: _GroupMemberToPerson_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupMemberToPerson_B" ON "schema-generator$financial"."_GroupMemberToPerson" USING btree ("B");


--
-- Name: _GroupRoleToGroupType_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupRoleToGroupType_A" ON "schema-generator$financial"."_GroupRoleToGroupType" USING btree ("A");


--
-- Name: _GroupRoleToGroupType_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupRoleToGroupType_AB_unique" ON "schema-generator$financial"."_GroupRoleToGroupType" USING btree ("A", "B");


--
-- Name: _GroupRoleToGroupType_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupRoleToGroupType_B" ON "schema-generator$financial"."_GroupRoleToGroupType" USING btree ("B");


--
-- Name: _GroupToGroupInvite_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroupInvite_A" ON "schema-generator$financial"."_GroupToGroupInvite" USING btree ("A");


--
-- Name: _GroupToGroupInvite_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupToGroupInvite_AB_unique" ON "schema-generator$financial"."_GroupToGroupInvite" USING btree ("A", "B");


--
-- Name: _GroupToGroupInvite_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroupInvite_B" ON "schema-generator$financial"."_GroupToGroupInvite" USING btree ("B");


--
-- Name: _GroupToGroupMember_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroupMember_A" ON "schema-generator$financial"."_GroupToGroupMember" USING btree ("A");


--
-- Name: _GroupToGroupMember_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupToGroupMember_AB_unique" ON "schema-generator$financial"."_GroupToGroupMember" USING btree ("A", "B");


--
-- Name: _GroupToGroupMember_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroupMember_B" ON "schema-generator$financial"."_GroupToGroupMember" USING btree ("B");


--
-- Name: _GroupToGroupType_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroupType_A" ON "schema-generator$financial"."_GroupToGroupType" USING btree ("A");


--
-- Name: _GroupToGroupType_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupToGroupType_AB_unique" ON "schema-generator$financial"."_GroupToGroupType" USING btree ("A", "B");


--
-- Name: _GroupToGroupType_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroupType_B" ON "schema-generator$financial"."_GroupToGroupType" USING btree ("B");


--
-- Name: _GroupToGroup_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroup_A" ON "schema-generator$financial"."_GroupToGroup" USING btree ("A");


--
-- Name: _GroupToGroup_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_GroupToGroup_AB_unique" ON "schema-generator$financial"."_GroupToGroup" USING btree ("A", "B");


--
-- Name: _GroupToGroup_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_GroupToGroup_B" ON "schema-generator$financial"."_GroupToGroup" USING btree ("B");


--
-- Name: _PersonToPhoneNumber_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_PersonToPhoneNumber_A" ON "schema-generator$financial"."_PersonToPhoneNumber" USING btree ("A");


--
-- Name: _PersonToPhoneNumber_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_PersonToPhoneNumber_AB_unique" ON "schema-generator$financial"."_PersonToPhoneNumber" USING btree ("A", "B");


--
-- Name: _PersonToPhoneNumber_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_PersonToPhoneNumber_B" ON "schema-generator$financial"."_PersonToPhoneNumber" USING btree ("B");


--
-- Name: _PersonToUser_A; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_PersonToUser_A" ON "schema-generator$financial"."_PersonToUser" USING btree ("A");


--
-- Name: _PersonToUser_AB_unique; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "_PersonToUser_AB_unique" ON "schema-generator$financial"."_PersonToUser" USING btree ("A", "B");


--
-- Name: _PersonToUser_B; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE INDEX "_PersonToUser_B" ON "schema-generator$financial"."_PersonToUser" USING btree ("B");


--
-- Name: schema-generator$financial.FinancialAccount.key._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "schema-generator$financial.FinancialAccount.key._UNIQUE" ON "schema-generator$financial"."FinancialAccount" USING btree (key);


--
-- Name: schema-generator$financial.Group.key._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "schema-generator$financial.Group.key._UNIQUE" ON "schema-generator$financial"."Group" USING btree (key);


--
-- Name: schema-generator$financial.GroupRole.name._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "schema-generator$financial.GroupRole.name._UNIQUE" ON "schema-generator$financial"."GroupRole" USING btree (name);


--
-- Name: schema-generator$financial.GroupType.name._UNIQUE; Type: INDEX; Schema: schema-generator$financial; Owner: prisma
--

CREATE UNIQUE INDEX "schema-generator$financial.GroupType.name._UNIQUE" ON "schema-generator$financial"."GroupType" USING btree (name);


--
-- Name: _CampusToFinancialAccount _CampusToFinancialAccount_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToFinancialAccount"
    ADD CONSTRAINT "_CampusToFinancialAccount_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToFinancialAccount _CampusToFinancialAccount_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToFinancialAccount"
    ADD CONSTRAINT "_CampusToFinancialAccount_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialAccount"(id) ON DELETE CASCADE;


--
-- Name: _CampusToGroup _CampusToGroup_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToGroup"
    ADD CONSTRAINT "_CampusToGroup_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToGroup _CampusToGroup_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToGroup"
    ADD CONSTRAINT "_CampusToGroup_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _CampusToLocation _CampusToLocation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToLocation"
    ADD CONSTRAINT "_CampusToLocation_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToLocation _CampusToLocation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToLocation"
    ADD CONSTRAINT "_CampusToLocation_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Location"(id) ON DELETE CASCADE;


--
-- Name: _CampusToPhoneNumber _CampusToPhoneNumber_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToPhoneNumber"
    ADD CONSTRAINT "_CampusToPhoneNumber_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Campus"(id) ON DELETE CASCADE;


--
-- Name: _CampusToPhoneNumber _CampusToPhoneNumber_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_CampusToPhoneNumber"
    ADD CONSTRAINT "_CampusToPhoneNumber_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."PhoneNumber"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialScheduledTransaction _FinancialAccountToFinancialScheduledTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialScheduledTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialAccount"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialScheduledTransaction _FinancialAccountToFinancialScheduledTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialScheduledTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialTransaction _FinancialAccountToFinancialTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialAccount"(id) ON DELETE CASCADE;


--
-- Name: _FinancialAccountToFinancialTransaction _FinancialAccountToFinancialTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialAccountToFinancialTransaction"
    ADD CONSTRAINT "_FinancialAccountToFinancialTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction _FinancialPaymentDetailToFinancialScheduledTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialScheduledTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialPaymentDetail"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialScheduledTransaction _FinancialPaymentDetailToFinancialScheduledTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialScheduledTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialScheduledTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialTransaction _FinancialPaymentDetailToFinancialTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialPaymentDetail"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToFinancialTransaction _FinancialPaymentDetailToFinancialTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToFinancialTransaction"
    ADD CONSTRAINT "_FinancialPaymentDetailToFinancialTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToLocation _FinancialPaymentDetailToLocation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToLocation"
    ADD CONSTRAINT "_FinancialPaymentDetailToLocation_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialPaymentDetail"(id) ON DELETE CASCADE;


--
-- Name: _FinancialPaymentDetailToLocation _FinancialPaymentDetailToLocation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialPaymentDetailToLocation"
    ADD CONSTRAINT "_FinancialPaymentDetailToLocation_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Location"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction _FinancialScheduledTransactionToFinancialTransaction_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction"
    ADD CONSTRAINT "_FinancialScheduledTransactionToFinancialTransaction_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToFinancialTransaction _FinancialScheduledTransactionToFinancialTransaction_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToFinancialTransaction"
    ADD CONSTRAINT "_FinancialScheduledTransactionToFinancialTransaction_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToPerson _FinancialScheduledTransactionToPerson_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToPerson"
    ADD CONSTRAINT "_FinancialScheduledTransactionToPerson_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialScheduledTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialScheduledTransactionToPerson _FinancialScheduledTransactionToPerson_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialScheduledTransactionToPerson"
    ADD CONSTRAINT "_FinancialScheduledTransactionToPerson_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToGroup _FinancialTransactionToGroup_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToGroup"
    ADD CONSTRAINT "_FinancialTransactionToGroup_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToGroup _FinancialTransactionToGroup_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToGroup"
    ADD CONSTRAINT "_FinancialTransactionToGroup_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToPerson _FinancialTransactionToPerson_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToPerson"
    ADD CONSTRAINT "_FinancialTransactionToPerson_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."FinancialTransaction"(id) ON DELETE CASCADE;


--
-- Name: _FinancialTransactionToPerson _FinancialTransactionToPerson_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_FinancialTransactionToPerson"
    ADD CONSTRAINT "_FinancialTransactionToPerson_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _GroupInviteToGroupRole _GroupInviteToGroupRole_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupInviteToGroupRole"
    ADD CONSTRAINT "_GroupInviteToGroupRole_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupInvite"(id) ON DELETE CASCADE;


--
-- Name: _GroupInviteToGroupRole _GroupInviteToGroupRole_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupInviteToGroupRole"
    ADD CONSTRAINT "_GroupInviteToGroupRole_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupRole"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToGroupRole _GroupMemberToGroupRole_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToGroupRole"
    ADD CONSTRAINT "_GroupMemberToGroupRole_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupMember"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToGroupRole _GroupMemberToGroupRole_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToGroupRole"
    ADD CONSTRAINT "_GroupMemberToGroupRole_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupRole"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToPerson _GroupMemberToPerson_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToPerson"
    ADD CONSTRAINT "_GroupMemberToPerson_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupMember"(id) ON DELETE CASCADE;


--
-- Name: _GroupMemberToPerson _GroupMemberToPerson_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupMemberToPerson"
    ADD CONSTRAINT "_GroupMemberToPerson_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _GroupRoleToGroupType _GroupRoleToGroupType_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupRoleToGroupType"
    ADD CONSTRAINT "_GroupRoleToGroupType_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."GroupRole"(id) ON DELETE CASCADE;


--
-- Name: _GroupRoleToGroupType _GroupRoleToGroupType_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupRoleToGroupType"
    ADD CONSTRAINT "_GroupRoleToGroupType_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupType"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupInvite _GroupToGroupInvite_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupInvite"
    ADD CONSTRAINT "_GroupToGroupInvite_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupInvite _GroupToGroupInvite_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupInvite"
    ADD CONSTRAINT "_GroupToGroupInvite_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupInvite"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupMember _GroupToGroupMember_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupMember"
    ADD CONSTRAINT "_GroupToGroupMember_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupMember _GroupToGroupMember_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupMember"
    ADD CONSTRAINT "_GroupToGroupMember_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupMember"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupType _GroupToGroupType_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupType"
    ADD CONSTRAINT "_GroupToGroupType_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroupType _GroupToGroupType_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroupType"
    ADD CONSTRAINT "_GroupToGroupType_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."GroupType"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroup _GroupToGroup_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroup"
    ADD CONSTRAINT "_GroupToGroup_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _GroupToGroup _GroupToGroup_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_GroupToGroup"
    ADD CONSTRAINT "_GroupToGroup_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."Group"(id) ON DELETE CASCADE;


--
-- Name: _PersonToPhoneNumber _PersonToPhoneNumber_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToPhoneNumber"
    ADD CONSTRAINT "_PersonToPhoneNumber_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _PersonToPhoneNumber _PersonToPhoneNumber_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToPhoneNumber"
    ADD CONSTRAINT "_PersonToPhoneNumber_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."PhoneNumber"(id) ON DELETE CASCADE;


--
-- Name: _PersonToUser _PersonToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToUser"
    ADD CONSTRAINT "_PersonToUser_A_fkey" FOREIGN KEY ("A") REFERENCES "schema-generator$financial"."Person"(id) ON DELETE CASCADE;


--
-- Name: _PersonToUser _PersonToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$financial; Owner: prisma
--

ALTER TABLE ONLY "schema-generator$financial"."_PersonToUser"
    ADD CONSTRAINT "_PersonToUser_B_fkey" FOREIGN KEY ("B") REFERENCES "schema-generator$financial"."User"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

