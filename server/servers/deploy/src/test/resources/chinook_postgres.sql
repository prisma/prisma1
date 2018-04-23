--CREATE SCHEMA IF NOT EXISTS chinook;
--SET search_path TO chinook;

-- ----------------------------
-- Sequence structure for album_album_id_seq
-- ----------------------------
CREATE SEQUENCE "album_album_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 347
 CACHE 1;
SELECT setval('"public"."album_album_id_seq"', 347, true);


-- ----------------------------
-- Sequence structure for artist_artist_id_seq
-- ----------------------------
CREATE SEQUENCE "artist_artist_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 275
 CACHE 1;
SELECT setval('"public"."artist_artist_id_seq"', 275, true);


-- ----------------------------
-- Sequence structure for customer_customer_id_seq
-- ----------------------------
CREATE SEQUENCE "customer_customer_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 59
 CACHE 1;
SELECT setval('"public"."customer_customer_id_seq"', 59, true);


-- ----------------------------
-- Sequence structure for genre_genre_id_seq
-- ----------------------------
CREATE SEQUENCE "genre_genre_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 25
 CACHE 1;
SELECT setval('"public"."genre_genre_id_seq"', 25, true);


-- ----------------------------
-- Sequence structure for employee_employee_id_seq
-- ----------------------------
CREATE SEQUENCE "employee_employee_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 8
 CACHE 1;
SELECT setval('"public"."employee_employee_id_seq"', 8, true);


-- ----------------------------
-- Sequence structure for invoice_invoice_id_seq
-- ----------------------------
CREATE SEQUENCE "invoice_invoice_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 412
 CACHE 1;
SELECT setval('"public"."invoice_invoice_id_seq"', 412, true);


-- ----------------------------
-- Sequence structure for invoiceline_invoiceline_id_seq
-- ----------------------------
CREATE SEQUENCE "invoiceline_invoiceline_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 2240
 CACHE 1;
SELECT setval('"public"."invoiceline_invoiceline_id_seq"', 2240, true);


-- ----------------------------
-- Sequence structure for mediatype_mediatype_id_seq
-- ----------------------------
CREATE SEQUENCE "mediatype_mediatype_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 5
 CACHE 1;
SELECT setval('"public"."mediatype_mediatype_id_seq"', 5, true);


-- ----------------------------
-- Sequence structure for playlist_playlist_id_seq
-- ----------------------------
CREATE SEQUENCE "playlist_playlist_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 18
 CACHE 1;
SELECT setval('"public"."playlist_playlist_id_seq"', 18, true);


-- ----------------------------
-- Sequence structure for track_track_id_seq
-- ----------------------------
CREATE SEQUENCE "track_track_id_seq"
 INCREMENT 1
 MINVALUE 1
 MAXVALUE 2147483647
 START 3503
 CACHE 1;
SELECT setval('"public"."track_track_id_seq"', 3503, true);

/*******************************************************************************
   Create Tables
********************************************************************************/
CREATE TABLE album
(
    album_id int DEFAULT nextval('album_album_id_seq'::regclass) NOT NULL,
    title VARCHAR(160) NOT NULL,
    artist_id INT NOT NULL,
    CONSTRAINT pk_album PRIMARY KEY  (album_id)
);

CREATE TABLE artist
(
    artist_id int DEFAULT nextval('artist_artist_id_seq'::regclass) NOT NULL,
    name VARCHAR(120),
    CONSTRAINT pk_artist PRIMARY KEY  (artist_id)
);

CREATE TABLE customer
(
    customer_id int DEFAULT nextval('customer_customer_id_seq'::regclass) NOT NULL,
    first_name VARCHAR(40) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    company VARCHAR(80),
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60) NOT NULL,
    support_rep_id INT,
    CONSTRAINT pk_customer PRIMARY KEY  (customer_id)
);

CREATE TABLE employee
(
    employee_id int DEFAULT nextval('employee_employee_id_seq'::regclass) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    title VARCHAR(30),
    reports_to INT,
    birth_date TIMESTAMP,
    hire_date TIMESTAMP,
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60),
    CONSTRAINT pk_employee PRIMARY KEY  (employee_id)
);

CREATE TABLE genre
(
    genre_id int DEFAULT nextval('genre_genre_id_seq'::regclass) NOT NULL,
    name VARCHAR(120),
    CONSTRAINT pk_genre PRIMARY KEY  (genre_id)
);

CREATE TABLE invoice
(
    invoice_id int DEFAULT nextval('invoice_invoice_id_seq'::regclass) NOT NULL,
    customer_id INT NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    billing_address VARCHAR(70),
    billing_city VARCHAR(40),
    billing_state VARCHAR(40),
    billing_country VARCHAR(40),
    billing_postal_code VARCHAR(10),
    total NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_invoice PRIMARY KEY  (invoice_id)
);

CREATE TABLE invoice_line
(
    invoice_line_id int DEFAULT nextval('invoiceline_invoiceline_id_seq'::regclass) NOT NULL,
    invoice_id INT NOT NULL,
    track_id INT NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT ok_invoice_line PRIMARY KEY  (invoice_line_id)
);

CREATE TABLE media_type
(
    media_type_id int DEFAULT nextval('mediatype_mediatype_id_seq'::regclass) NOT NULL,
    name VARCHAR(120),
    CONSTRAINT pk_media_type PRIMARY KEY  (media_type_id)
);

CREATE TABLE playlist
(
    playlist_id int DEFAULT nextval('playlist_playlist_id_seq'::regclass) NOT NULL,
    name VARCHAR(120),
    CONSTRAINT pk_playlist PRIMARY KEY  (playlist_id)
);

CREATE TABLE playlist_track
(
    playlist_id INT NOT NULL,
    track_id INT NOT NULL,
    CONSTRAINT pk_playlist_track PRIMARY KEY  (playlist_id, track_id)
);

CREATE TABLE track
(
    track_id int DEFAULT nextval('track_track_id_seq'::regclass) NOT NULL,
    name VARCHAR(200) NOT NULL,
    album_id INT,
    media_type_id INT NOT NULL,
    genre_id INT,
    composer VARCHAR(220),
    milliseconds INT NOT NULL,
    bytes INT,
    unit_price NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_track PRIMARY KEY  (track_id)
);



/*******************************************************************************
   Create Primary Key Unique Indexes
********************************************************************************/

/*******************************************************************************
   Create Foreign Keys
********************************************************************************/
ALTER TABLE album ADD CONSTRAINT fk_album_artist_id
    FOREIGN KEY (artist_id) REFERENCES artist (artist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_album_artist_id ON album (artist_id);

ALTER TABLE customer ADD CONSTRAINT fk_customer_support_rep_id
    FOREIGN KEY (support_rep_id) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_customer_support_rep_id ON customer (support_rep_id);

ALTER TABLE employee ADD CONSTRAINT fk_employee_reports_to
    FOREIGN KEY (reports_to) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_employee_reports_to ON employee (reports_to);

ALTER TABLE invoice ADD CONSTRAINT fk_invoice_customer_id
    FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_invoice_customer_id ON invoice (customer_id);

ALTER TABLE invoice_line ADD CONSTRAINT fk_invoice_line_invoice_id
    FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_invoice_line_invoice_id ON invoice_line (invoice_id);

ALTER TABLE invoice_line ADD CONSTRAINT fk_invoice_line_track_id
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_invoice_line_track_id ON invoice_line (track_id);

ALTER TABLE playlist_track ADD CONSTRAINT fk_playlist_track_playlist_id
    FOREIGN KEY (playlist_id) REFERENCES playlist (playlist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE playlist_track ADD CONSTRAINT fk_playlist_track_track_id
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_playlist_track_track_id ON playlist_track (track_id);

ALTER TABLE track ADD CONSTRAINT fk_track_album_id
    FOREIGN KEY (album_id) REFERENCES album (album_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_track_album_id ON track (album_id);

ALTER TABLE track ADD CONSTRAINT fk_track_genre_id
    FOREIGN KEY (genre_id) REFERENCES genre (genre_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_track_genre_id ON track (genre_id);

ALTER TABLE track ADD CONSTRAINT fk_track_media_type_id
    FOREIGN KEY (media_type_id) REFERENCES media_type (media_type_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX ifk_track_media_type_id ON track (media_type_id);