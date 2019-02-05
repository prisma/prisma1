import testSchema from "../common"

describe('Introspector', () => {
  // Schema from https://github.com/prismagraphql/prisma/issues/2504
  test('test schema - 2504', async () => {
    await testSchema(sql, 'databaseintrospector')
  })
})

const sql = `
--
-- PostgreSQL database dump
--

-- Dumped from database version 10.4
-- Dumped by pg_dump version 10.4

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
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: intarray; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS intarray WITH SCHEMA DatabaseIntrospector;


--
-- Name: EXTENSION intarray; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION intarray IS 'functions, operators, and index support for 1-D arrays of integers';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: direct_messages; Type: TABLE; Schema: DatabaseIntrospector; Owner: postgres
--

CREATE TABLE DatabaseIntrospector.direct_messages (
    id integer NOT NULL,
    text VARCHAR(25) NOT NULL,
    receiver_id integer,
    sender_id integer
);


ALTER TABLE DatabaseIntrospector.direct_messages OWNER TO postgres;

--
-- Name: direct_messages_id_seq; Type: SEQUENCE; Schema: DatabaseIntrospector; Owner: postgres
--

CREATE SEQUENCE DatabaseIntrospector.direct_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE DatabaseIntrospector.direct_messages_id_seq OWNER TO postgres;

--
-- Name: direct_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER SEQUENCE DatabaseIntrospector.direct_messages_id_seq OWNED BY DatabaseIntrospector.direct_messages.id;

--
-- Name: users; Type: TABLE; Schema: DatabaseIntrospector; Owner: postgres
--

CREATE TABLE DatabaseIntrospector.users (
    id integer NOT NULL,
    name VARCHAR(25) NOT NULL
);


ALTER TABLE DatabaseIntrospector.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: DatabaseIntrospector; Owner: postgres
--

CREATE SEQUENCE DatabaseIntrospector.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE DatabaseIntrospector.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER SEQUENCE DatabaseIntrospector.users_id_seq OWNED BY DatabaseIntrospector.users.id;


--
-- Name: direct_messages id; Type: DEFAULT; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER TABLE ONLY DatabaseIntrospector.direct_messages ALTER COLUMN id SET DEFAULT nextval('DatabaseIntrospector.direct_messages_id_seq'::regclass);

--
-- Name: users id; Type: DEFAULT; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER TABLE ONLY DatabaseIntrospector.users ALTER COLUMN id SET DEFAULT nextval('DatabaseIntrospector.users_id_seq'::regclass);

--
-- Name: direct_messages direct_messages_pkey; Type: CONSTRAINT; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER TABLE ONLY DatabaseIntrospector.direct_messages
    ADD CONSTRAINT direct_messages_pkey PRIMARY KEY (id);

--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER TABLE ONLY DatabaseIntrospector.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

--
-- Name: direct_messages direct_messages_receiver_id_fkey; Type: FK CONSTRAINT; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER TABLE ONLY DatabaseIntrospector.direct_messages
    ADD CONSTRAINT direct_messages_receiver_id_fkey FOREIGN KEY (receiver_id) REFERENCES DatabaseIntrospector.users(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: direct_messages direct_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: DatabaseIntrospector; Owner: postgres
--

ALTER TABLE ONLY DatabaseIntrospector.direct_messages
    ADD CONSTRAINT direct_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES DatabaseIntrospector.users(id) ON UPDATE CASCADE ON DELETE SET NULL;

--
-- PostgreSQL database dump complete
--
`
