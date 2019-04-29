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
-- Name: schema-generator$relations-4424; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "schema-generator$relations-4424";


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: Customer; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."Customer" (
    id character varying(25) NOT NULL,
    uid text NOT NULL,
    "emailId" text NOT NULL,
    "mobileNumber" text,
    name text,
    "createdAt" timestamp(3) without time zone NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: CustomerCartItem; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."CustomerCartItem" (
    id character varying(25) NOT NULL,
    "addedAtPrice" integer NOT NULL,
    quantity integer NOT NULL,
    "savedForLater" boolean NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    product character varying(25),
    customer character varying(25)
);


--
-- Name: CustomerWishlist; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."CustomerWishlist" (
    id character varying(25) NOT NULL,
    "listName" text NOT NULL,
    customer character varying(25)
);


--
-- Name: Post; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."Post" (
    id character varying(25) NOT NULL,
    text text,
    "createdAt" timestamp(3) without time zone NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: PostsProductsRelation; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."PostsProductsRelation" (
    post character varying(25) NOT NULL,
    product character varying(25) NOT NULL
);


--
-- Name: Product; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."Product" (
    id character varying(25) NOT NULL,
    name text NOT NULL,
    brand text NOT NULL,
    description text NOT NULL,
    "createdAt" timestamp(3) without time zone NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Product_ratingsDistribution; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."Product_ratingsDistribution" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value integer NOT NULL
);


--
-- Name: Product_tags; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."Product_tags" (
    "nodeId" character varying(25) NOT NULL,
    "position" integer NOT NULL,
    value text NOT NULL
);


--
-- Name: WishlistedProductsRelation; Type: TABLE; Schema: schema-generator$relations-4424; Owner: -
--

CREATE TABLE "schema-generator$relations-4424"."WishlistedProductsRelation" (
    wishlist character varying(25) NOT NULL,
    product character varying(25) NOT NULL
);


--
-- Name: CustomerCartItem CustomerCartItem_pkey; Type: CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."CustomerCartItem"
    ADD CONSTRAINT "CustomerCartItem_pkey" PRIMARY KEY (id);


--
-- Name: CustomerWishlist CustomerWishlist_pkey; Type: CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."CustomerWishlist"
    ADD CONSTRAINT "CustomerWishlist_pkey" PRIMARY KEY (id);


--
-- Name: Customer Customer_pkey; Type: CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."Customer"
    ADD CONSTRAINT "Customer_pkey" PRIMARY KEY (id);


--
-- Name: Post Post_pkey; Type: CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."Post"
    ADD CONSTRAINT "Post_pkey" PRIMARY KEY (id);


--
-- Name: Product Product_pkey; Type: CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."Product"
    ADD CONSTRAINT "Product_pkey" PRIMARY KEY (id);


--
-- Name: Product_ratingsDistribution Product_ratingsDistribution_pkey; Type: CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."Product_ratingsDistribution"
    ADD CONSTRAINT "Product_ratingsDistribution_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: Product_tags Product_tags_pkey; Type: CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."Product_tags"
    ADD CONSTRAINT "Product_tags_pkey" PRIMARY KEY ("nodeId", "position");


--
-- Name: PostsProductsRelation_AB_unique; Type: INDEX; Schema: schema-generator$relations-4424; Owner: -
--

CREATE UNIQUE INDEX "PostsProductsRelation_AB_unique" ON "schema-generator$relations-4424"."PostsProductsRelation" USING btree (post, product);


--
-- Name: PostsProductsRelation_B; Type: INDEX; Schema: schema-generator$relations-4424; Owner: -
--

CREATE INDEX "PostsProductsRelation_B" ON "schema-generator$relations-4424"."PostsProductsRelation" USING btree (product);


--
-- Name: WishlistedProductsRelation_AB_unique; Type: INDEX; Schema: schema-generator$relations-4424; Owner: -
--

CREATE UNIQUE INDEX "WishlistedProductsRelation_AB_unique" ON "schema-generator$relations-4424"."WishlistedProductsRelation" USING btree (wishlist, product);


--
-- Name: WishlistedProductsRelation_B; Type: INDEX; Schema: schema-generator$relations-4424; Owner: -
--

CREATE INDEX "WishlistedProductsRelation_B" ON "schema-generator$relations-4424"."WishlistedProductsRelation" USING btree (product);


--
-- Name: schema-generator$relations-4424.Customer.emailId._UNIQUE; Type: INDEX; Schema: schema-generator$relations-4424; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$relations-4424.Customer.emailId._UNIQUE" ON "schema-generator$relations-4424"."Customer" USING btree ("emailId");


--
-- Name: schema-generator$relations-4424.Customer.uid._UNIQUE; Type: INDEX; Schema: schema-generator$relations-4424; Owner: -
--

CREATE UNIQUE INDEX "schema-generator$relations-4424.Customer.uid._UNIQUE" ON "schema-generator$relations-4424"."Customer" USING btree (uid);


--
-- Name: CustomerCartItem CustomerCartItem_customer_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."CustomerCartItem"
    ADD CONSTRAINT "CustomerCartItem_customer_fkey" FOREIGN KEY (customer) REFERENCES "schema-generator$relations-4424"."Customer"(id) ON DELETE SET NULL;


--
-- Name: CustomerCartItem CustomerCartItem_product_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."CustomerCartItem"
    ADD CONSTRAINT "CustomerCartItem_product_fkey" FOREIGN KEY (product) REFERENCES "schema-generator$relations-4424"."Product"(id) ON DELETE SET NULL;


--
-- Name: CustomerWishlist CustomerWishlist_customer_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."CustomerWishlist"
    ADD CONSTRAINT "CustomerWishlist_customer_fkey" FOREIGN KEY (customer) REFERENCES "schema-generator$relations-4424"."Customer"(id) ON DELETE SET NULL;


--
-- Name: PostsProductsRelation PostsProductsRelation_post_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."PostsProductsRelation"
    ADD CONSTRAINT "PostsProductsRelation_post_fkey" FOREIGN KEY (post) REFERENCES "schema-generator$relations-4424"."Post"(id) ON DELETE CASCADE;


--
-- Name: PostsProductsRelation PostsProductsRelation_product_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."PostsProductsRelation"
    ADD CONSTRAINT "PostsProductsRelation_product_fkey" FOREIGN KEY (product) REFERENCES "schema-generator$relations-4424"."Product"(id) ON DELETE CASCADE;


--
-- Name: Product_ratingsDistribution Product_ratingsDistribution_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."Product_ratingsDistribution"
    ADD CONSTRAINT "Product_ratingsDistribution_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$relations-4424"."Product"(id);


--
-- Name: Product_tags Product_tags_nodeId_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."Product_tags"
    ADD CONSTRAINT "Product_tags_nodeId_fkey" FOREIGN KEY ("nodeId") REFERENCES "schema-generator$relations-4424"."Product"(id);


--
-- Name: WishlistedProductsRelation WishlistedProductsRelation_product_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."WishlistedProductsRelation"
    ADD CONSTRAINT "WishlistedProductsRelation_product_fkey" FOREIGN KEY (product) REFERENCES "schema-generator$relations-4424"."Product"(id) ON DELETE CASCADE;


--
-- Name: WishlistedProductsRelation WishlistedProductsRelation_wishlist_fkey; Type: FK CONSTRAINT; Schema: schema-generator$relations-4424; Owner: -
--

ALTER TABLE ONLY "schema-generator$relations-4424"."WishlistedProductsRelation"
    ADD CONSTRAINT "WishlistedProductsRelation_wishlist_fkey" FOREIGN KEY (wishlist) REFERENCES "schema-generator$relations-4424"."CustomerWishlist"(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

