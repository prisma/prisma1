import testSchema from '../common'

describe('Introspector', () => {
  // Schema from https://github.com/prismagraphql/prisma/issues/2504
  test('Issue - 4208', async () => {
    await testSchema(sql, 'management4208')
  })
})

const sql = `
--
-- PostgreSQL database dump
--

--
-- Name: jwt_token; Type: TYPE; Schema: public; Owner: ryan
--

CREATE TYPE jwt_token AS (
	user_id integer
);

--
-- Name: questions; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE questions (
    question_id integer NOT NULL,
    content text NOT NULL,
    time_entered timestamp with time zone DEFAULT now() NOT NULL,
    status text NOT NULL,
    time_addressed timestamp with time zone,
    session_id integer NOT NULL,
    asker_id integer NOT NULL,
    answerer_id integer,
    location text
);

CREATE TABLE tags (
    tag_id integer NOT NULL,
    name text NOT NULL,
    course_id integer NOT NULL,
    level integer NOT NULL,
    activated boolean DEFAULT true NOT NULL
);

--
-- Name: session_series; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE session_series (
    session_series_id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    building text NOT NULL,
    room text NOT NULL,
    course_id integer NOT NULL,
    title text
);

CREATE TABLE sessions (
    session_id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    building text NOT NULL,
    room text NOT NULL,
    session_series_id integer,
    course_id integer NOT NULL,
    title text
);

--
-- Name: users; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE users (
    user_id integer NOT NULL,
    email text NOT NULL,
    google_id text NOT NULL,
    first_name text,
    last_name text,
    created_at timestamp with time zone DEFAULT now(),
    last_activity_at timestamp with time zone DEFAULT now(),
    photo_url text,
    display_name text
);

--
-- Name: course_users; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE course_users (
    course_id integer NOT NULL,
    user_id integer NOT NULL,
    role text NOT NULL
);

--
-- Name: CloudSecret; Type: TABLE; Schema: management; Owner: ryan
--

CREATE TABLE "CloudSecret" (
    secret character varying(255) NOT NULL
);

--
-- Name: InternalMigration; Type: TABLE; Schema: management; Owner: ryan
--

CREATE TABLE "InternalMigration" (
    id character varying(255) NOT NULL,
    "appliedAt" timestamp without time zone NOT NULL
);

--
-- Name: Migration; Type: TABLE; Schema: management; Owner: ryan
--

CREATE TABLE "Migration" (
    "projectId" character varying(200) DEFAULT ''::character varying NOT NULL,
    revision integer DEFAULT 1 NOT NULL,
    schema text,
    functions text,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    applied integer DEFAULT 0 NOT NULL,
    "rolledBack" integer DEFAULT 0 NOT NULL,
    steps text,
    errors text,
    "startedAt" timestamp without time zone,
    "finishedAt" timestamp without time zone,
    datamodel text,
    CONSTRAINT "Migration_status_check" CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'SUCCESS'::character varying, 'ROLLING_BACK'::character varying, 'ROLLBACK_SUCCESS'::character varying, 'ROLLBACK_FAILURE'::character varying])::text[])))
);

--
-- Name: Project; Type: TABLE; Schema: management; Owner: ryan
--

CREATE TABLE "Project" (
    id character varying(200) DEFAULT ''::character varying NOT NULL,
    secrets text,
    "allowQueries" boolean DEFAULT true NOT NULL,
    "allowMutations" boolean DEFAULT true NOT NULL,
    functions text
);

--
-- Name: TelemetryInfo; Type: TABLE; Schema: management; Owner: ryan
--

CREATE TABLE "TelemetryInfo" (
    id character varying(255) NOT NULL,
    "lastPinged" timestamp without time zone
);

--
-- Name: courses; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE courses (
    course_id integer NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    semester text NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    queue_open_interval interval DEFAULT '00:30:00'::interval NOT NULL,
    char_limit integer DEFAULT 100 NOT NULL
);

--
-- Name: courses_course_id_seq; Type: SEQUENCE; Schema: public; Owner: ryan
--

CREATE SEQUENCE courses_course_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE courses_course_id_seq OWNED BY courses.course_id;

--
-- Name: question_tags; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE question_tags (
    question_id integer NOT NULL,
    tag_id integer NOT NULL
);

--
-- Name: questions_question_id_seq; Type: SEQUENCE; Schema: public; Owner: ryan
--

CREATE SEQUENCE questions_question_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE questions_question_id_seq OWNED BY questions.question_id;

--
-- Name: session_series_session_series_id_seq; Type: SEQUENCE; Schema: public; Owner: ryan
--

CREATE SEQUENCE session_series_session_series_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE session_series_session_series_id_seq OWNED BY session_series.session_series_id;


--
-- Name: session_series_tas; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE session_series_tas (
    session_series_id integer NOT NULL,
    user_id integer NOT NULL
);


CREATE TABLE session_tas (
    session_id integer NOT NULL,
    user_id integer NOT NULL
);

--
-- Name: sessions_session_id_seq; Type: SEQUENCE; Schema: public; Owner: ryan
--

CREATE SEQUENCE sessions_session_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE sessions_session_id_seq OWNED BY sessions.session_id;


--
-- Name: tag_relations; Type: TABLE; Schema: public; Owner: ryan
--

CREATE TABLE tag_relations (
    parent_id integer NOT NULL,
    child_id integer NOT NULL
);

--
-- Name: tags_tag_id_seq; Type: SEQUENCE; Schema: public; Owner: ryan
--

CREATE SEQUENCE tags_tag_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tags_tag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ryan
--

ALTER SEQUENCE tags_tag_id_seq OWNED BY tags.tag_id;


--
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: ryan
--

CREATE SEQUENCE users_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ryan
--

ALTER SEQUENCE users_user_id_seq OWNED BY users.user_id;


--
-- Name: courses course_id; Type: DEFAULT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY courses ALTER COLUMN course_id SET DEFAULT nextval('courses_course_id_seq'::regclass);


--
-- Name: questions question_id; Type: DEFAULT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY questions ALTER COLUMN question_id SET DEFAULT nextval('questions_question_id_seq'::regclass);


--
-- Name: session_series session_series_id; Type: DEFAULT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_series ALTER COLUMN session_series_id SET DEFAULT nextval('session_series_session_series_id_seq'::regclass);


--
-- Name: sessions session_id; Type: DEFAULT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY sessions ALTER COLUMN session_id SET DEFAULT nextval('sessions_session_id_seq'::regclass);


--
-- Name: tags tag_id; Type: DEFAULT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY tags ALTER COLUMN tag_id SET DEFAULT nextval('tags_tag_id_seq'::regclass);


--
-- Name: users user_id; Type: DEFAULT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY users ALTER COLUMN user_id SET DEFAULT nextval('users_user_id_seq'::regclass);

--
-- Name: CloudSecret CloudSecret_pkey; Type: CONSTRAINT; Schema: management; Owner: ryan
--

ALTER TABLE ONLY "CloudSecret"
    ADD CONSTRAINT "CloudSecret_pkey" PRIMARY KEY (secret);


--
-- Name: InternalMigration InternalMigration_pkey; Type: CONSTRAINT; Schema: management; Owner: ryan
--

ALTER TABLE ONLY "InternalMigration"
    ADD CONSTRAINT "InternalMigration_pkey" PRIMARY KEY (id);


--
-- Name: Migration Migration_pkey; Type: CONSTRAINT; Schema: management; Owner: ryan
--

ALTER TABLE ONLY "Migration"
    ADD CONSTRAINT "Migration_pkey" PRIMARY KEY ("projectId", revision);


--
-- Name: Project Project_pkey; Type: CONSTRAINT; Schema: management; Owner: ryan
--

ALTER TABLE ONLY "Project"
    ADD CONSTRAINT "Project_pkey" PRIMARY KEY (id);


--
-- Name: TelemetryInfo TelemetryInfo_pkey; Type: CONSTRAINT; Schema: management; Owner: ryan
--

ALTER TABLE ONLY "TelemetryInfo"
    ADD CONSTRAINT "TelemetryInfo_pkey" PRIMARY KEY (id);

--
-- Name: course_users course_users_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY course_users
    ADD CONSTRAINT course_users_pk PRIMARY KEY (course_id, user_id);


--
-- Name: courses courses_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY courses
    ADD CONSTRAINT courses_pk PRIMARY KEY (course_id);


--
-- Name: question_tags question_tags_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY question_tags
    ADD CONSTRAINT question_tags_pk PRIMARY KEY (question_id, tag_id);


--
-- Name: questions questions_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY questions
    ADD CONSTRAINT questions_pk PRIMARY KEY (question_id);


--
-- Name: session_series session_series_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_series
    ADD CONSTRAINT session_series_pk PRIMARY KEY (session_series_id);


--
-- Name: session_series_tas session_series_tas_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_series_tas
    ADD CONSTRAINT session_series_tas_pk PRIMARY KEY (session_series_id, user_id);


--
-- Name: session_tas session_tas_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_tas
    ADD CONSTRAINT session_tas_pk PRIMARY KEY (session_id, user_id);


--
-- Name: sessions sessions_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_pk PRIMARY KEY (session_id);


--
-- Name: tag_relations tag_relations_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY tag_relations
    ADD CONSTRAINT tag_relations_pk PRIMARY KEY (parent_id, child_id);


--
-- Name: tags tags_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY tags
    ADD CONSTRAINT tags_pk PRIMARY KEY (tag_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_googleid_key; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_googleid_key UNIQUE (google_id);


--
-- Name: users users_pk; Type: CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pk PRIMARY KEY (user_id);

--
-- Name: Migration migrations_projectid_foreign; Type: FK CONSTRAINT; Schema: management; Owner: ryan
--

ALTER TABLE ONLY "Migration"
    ADD CONSTRAINT migrations_projectid_foreign FOREIGN KEY ("projectId") REFERENCES "Project"(id) ON UPDATE CASCADE ON DELETE CASCADE;

--
-- Name: course_users course_users_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY course_users
    ADD CONSTRAINT course_users_fk0 FOREIGN KEY (course_id) REFERENCES courses(course_id);


--
-- Name: course_users course_users_fk1; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY course_users
    ADD CONSTRAINT course_users_fk1 FOREIGN KEY (user_id) REFERENCES users(user_id);


--
-- Name: question_tags question_tags_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY question_tags
    ADD CONSTRAINT question_tags_fk0 FOREIGN KEY (question_id) REFERENCES questions(question_id);


--
-- Name: question_tags question_tags_fk1; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY question_tags
    ADD CONSTRAINT question_tags_fk1 FOREIGN KEY (tag_id) REFERENCES tags(tag_id);


--
-- Name: questions questions_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY questions
    ADD CONSTRAINT questions_fk0 FOREIGN KEY (session_id) REFERENCES sessions(session_id);


--
-- Name: questions questions_fk1; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY questions
    ADD CONSTRAINT questions_fk1 FOREIGN KEY (asker_id) REFERENCES users(user_id);


--
-- Name: questions questions_fk2; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY questions
    ADD CONSTRAINT questions_fk2 FOREIGN KEY (answerer_id) REFERENCES users(user_id);


--
-- Name: session_series session_series_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_series
    ADD CONSTRAINT session_series_fk0 FOREIGN KEY (course_id) REFERENCES courses(course_id);


--
-- Name: session_series_tas session_series_tas_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_series_tas
    ADD CONSTRAINT session_series_tas_fk0 FOREIGN KEY (session_series_id) REFERENCES session_series(session_series_id);


--
-- Name: session_series_tas session_series_tas_fk1; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_series_tas
    ADD CONSTRAINT session_series_tas_fk1 FOREIGN KEY (user_id) REFERENCES users(user_id);


--
-- Name: session_tas session_tas_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_tas
    ADD CONSTRAINT session_tas_fk0 FOREIGN KEY (session_id) REFERENCES sessions(session_id);


--
-- Name: session_tas session_tas_fk1; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY session_tas
    ADD CONSTRAINT session_tas_fk1 FOREIGN KEY (user_id) REFERENCES users(user_id);


--
-- Name: sessions sessions_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_fk0 FOREIGN KEY (course_id) REFERENCES courses(course_id);


--
-- Name: sessions sessions_fk1; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_fk1 FOREIGN KEY (session_series_id) REFERENCES session_series(session_series_id);


--
-- Name: tag_relations tag_relations_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY tag_relations
    ADD CONSTRAINT tag_relations_fk0 FOREIGN KEY (parent_id) REFERENCES tags(tag_id);


--
-- Name: tag_relations tag_relations_fk1; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY tag_relations
    ADD CONSTRAINT tag_relations_fk1 FOREIGN KEY (child_id) REFERENCES tags(tag_id);


--
-- Name: tags tags_fk0; Type: FK CONSTRAINT; Schema: public; Owner: ryan
--

ALTER TABLE ONLY tags
    ADD CONSTRAINT tags_fk0 FOREIGN KEY (course_id) REFERENCES courses(course_id);

--
-- PostgreSQL database dump complete
--

`
