import testSchema from '../common'

describe('Introspector', () => {
  test('issue4081 relations', async () => {
    await testSchema(`
        -- PostgreSQL database dump
        
        -- Dumped from database version 9.5.10
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
        
        
        
        
        CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
        COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';
        
        
        SET default_tablespace = '';
        
        SET default_with_oids = false;
        
        
        CREATE TABLE issue4081.about_aboutpage (
            id integer NOT NULL,
            heading character varying(300) NOT NULL,
            text text NOT NULL,
            heading_image character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.about_aboutpage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.about_aboutpage_id_seq OWNED BY issue4081.about_aboutpage.id;
        
        
        
        CREATE TABLE issue4081.about_teammember (
            id integer NOT NULL,
            picture character varying(100) NOT NULL,
            name character varying(300) NOT NULL,
            "position" character varying(300) NOT NULL,
            description text NOT NULL,
            facebook_url character varying(200) NOT NULL,
            twitter_url character varying(200) NOT NULL,
            youtube_url character varying(200) NOT NULL,
            instagram_url character varying(200) NOT NULL,
            "order" integer NOT NULL,
            visible boolean NOT NULL,
            page_id integer NOT NULL,
            CONSTRAINT about_teammember_order_check CHECK (("order" >= 0))
        );
        
        
        
        
        CREATE SEQUENCE issue4081.about_teammember_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.about_teammember_id_seq OWNED BY issue4081.about_teammember.id;
        
        
        
        CREATE TABLE issue4081.account_emailaddress (
            id integer NOT NULL,
            email character varying(254) NOT NULL,
            verified boolean NOT NULL,
            "primary" boolean NOT NULL,
            user_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.account_emailaddress_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.account_emailaddress_id_seq OWNED BY issue4081.account_emailaddress.id;
        
        
        
        CREATE TABLE issue4081.account_emailconfirmation (
            id integer NOT NULL,
            created timestamp with time zone NOT NULL,
            sent timestamp with time zone,
            key character varying(64) NOT NULL,
            email_address_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.account_emailconfirmation_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.account_emailconfirmation_id_seq OWNED BY issue4081.account_emailconfirmation.id;
        
        
        
        CREATE TABLE issue4081.auth_group (
            id integer NOT NULL,
            name character varying(80) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.auth_group_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.auth_group_id_seq OWNED BY issue4081.auth_group.id;
        
        
        
        CREATE TABLE issue4081.auth_group_permissions (
            id integer NOT NULL,
            group_id integer NOT NULL,
            permission_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.auth_group_permissions_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.auth_group_permissions_id_seq OWNED BY issue4081.auth_group_permissions.id;
        
        
        
        CREATE TABLE issue4081.auth_permission (
            id integer NOT NULL,
            name character varying(255) NOT NULL,
            content_type_id integer NOT NULL,
            codename character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.auth_permission_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.auth_permission_id_seq OWNED BY issue4081.auth_permission.id;
        
        
        
        CREATE TABLE issue4081.contact_contactformcontent (
            id integer NOT NULL,
            title character varying(300) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.contact_contactform_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.contact_contactform_id_seq OWNED BY issue4081.contact_contactformcontent.id;
        
        
        
        CREATE TABLE issue4081.contact_contactpage (
            id integer NOT NULL,
            heading character varying(300) NOT NULL,
            heading_image character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.contact_contactpage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.contact_contactpage_id_seq OWNED BY issue4081.contact_contactpage.id;
        
        
        
        CREATE TABLE issue4081.contact_thankyoupage (
            id integer NOT NULL,
            heading character varying(300) NOT NULL,
            title character varying(300) NOT NULL,
            text text NOT NULL,
            button_link character varying(200) NOT NULL,
            button_text character varying(10) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.contact_thankyoupage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.contact_thankyoupage_id_seq OWNED BY issue4081.contact_thankyoupage.id;
        
        
        
        CREATE TABLE issue4081.django_admin_log (
            id integer NOT NULL,
            action_time timestamp with time zone NOT NULL,
            object_id text,
            object_repr character varying(200) NOT NULL,
            action_flag smallint NOT NULL,
            change_message text NOT NULL,
            content_type_id integer,
            user_id integer NOT NULL,
            CONSTRAINT django_admin_log_action_flag_check CHECK ((action_flag >= 0))
        );
        
        
        
        
        CREATE SEQUENCE issue4081.django_admin_log_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.django_admin_log_id_seq OWNED BY issue4081.django_admin_log.id;
        
        
        
        CREATE TABLE issue4081.django_content_type (
            id integer NOT NULL,
            app_label character varying(100) NOT NULL,
            model character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.django_content_type_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.django_content_type_id_seq OWNED BY issue4081.django_content_type.id;
        
        
        
        CREATE TABLE issue4081.django_migrations (
            id integer NOT NULL,
            app character varying(255) NOT NULL,
            name character varying(255) NOT NULL,
            applied timestamp with time zone NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.django_migrations_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.django_migrations_id_seq OWNED BY issue4081.django_migrations.id;
        
        
        
        CREATE TABLE issue4081.django_session (
            session_key character varying(40) NOT NULL,
            session_data text NOT NULL,
            expire_date timestamp with time zone NOT NULL
        );
        
        
        
        
        CREATE TABLE issue4081.django_site (
            id integer NOT NULL,
            domain character varying(100) NOT NULL,
            name character varying(50) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.django_site_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.django_site_id_seq OWNED BY issue4081.django_site.id;
        
        
        
        CREATE TABLE issue4081.events_event (
            id integer NOT NULL,
            date date NOT NULL,
            "time" time without time zone NOT NULL,
            picture character varying(100) NOT NULL,
            name character varying(300) NOT NULL,
            hosted_by character varying(300) NOT NULL,
            topic character varying(600) NOT NULL,
            location_name character varying(300) NOT NULL,
            address_line_one character varying(300) NOT NULL,
            address_line_two character varying(300) NOT NULL,
            visible boolean NOT NULL,
            map_url character varying(200) NOT NULL,
            country character varying(300) NOT NULL,
            city character varying(300) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.events_event_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.events_event_id_seq OWNED BY issue4081.events_event.id;
        
        
        
        CREATE TABLE issue4081.events_eventspage (
            id integer NOT NULL,
            heading_image character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.events_eventpage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.events_eventpage_id_seq OWNED BY issue4081.events_eventspage.id;
        
        
        
        CREATE TABLE issue4081.help_helppage (
            id integer NOT NULL,
            heading character varying(300) NOT NULL,
            heading_image character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.help_helppage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.help_helppage_id_seq OWNED BY issue4081.help_helppage.id;
        
        
        
        CREATE TABLE issue4081.help_helpquestion (
            id integer NOT NULL,
            title character varying(300) NOT NULL,
            answer text NOT NULL,
            section_id integer NOT NULL,
            thumbnail character varying(100) NOT NULL,
            "order" integer,
            CONSTRAINT help_helpquestion_order_check CHECK (("order" >= 0))
        );
        
        
        
        
        CREATE SEQUENCE issue4081.help_helpquestion_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.help_helpquestion_id_seq OWNED BY issue4081.help_helpquestion.id;
        
        
        
        CREATE TABLE issue4081.help_helpsection (
            id integer NOT NULL,
            title character varying(300) NOT NULL,
            "order" integer NOT NULL,
            page_id integer NOT NULL,
            CONSTRAINT help_helpsection_order_check CHECK (("order" >= 0))
        );
        
        
        
        
        CREATE SEQUENCE issue4081.help_helpsection_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.help_helpsection_id_seq OWNED BY issue4081.help_helpsection.id;
        
        
        
        CREATE TABLE issue4081.jobs_job (
            id integer NOT NULL,
            picture character varying(100) NOT NULL,
            position_name character varying(300) NOT NULL,
            position_location character varying(300) NOT NULL,
            "order" integer,
            visible boolean NOT NULL,
            position_description text NOT NULL,
            apply_page_title character varying(300) NOT NULL,
            apply_page_form_title character varying(300) NOT NULL,
            page_id integer NOT NULL,
            CONSTRAINT jobs_job_order_check CHECK (("order" >= 0))
        );
        
        
        
        
        CREATE SEQUENCE issue4081.jobs_job_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.jobs_job_id_seq OWNED BY issue4081.jobs_job.id;
        
        
        
        CREATE TABLE issue4081.jobs_jobspage (
            id integer NOT NULL,
            heading character varying(300) NOT NULL,
            text text NOT NULL,
            heading_image character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.jobs_jobspage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.jobs_jobspage_id_seq OWNED BY issue4081.jobs_jobspage.id;
        
        
        
        CREATE TABLE issue4081.jobs_nojobspage (
            id integer NOT NULL,
            heading character varying(300) NOT NULL,
            title character varying(300) NOT NULL,
            text text NOT NULL,
            video_thumbnail character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.jobs_nojobspage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.jobs_nojobspage_id_seq OWNED BY issue4081.jobs_nojobspage.id;
        
        
        
        CREATE TABLE issue4081.jobs_thankyoupage (
            id integer NOT NULL,
            heading character varying(300) NOT NULL,
            title character varying(300) NOT NULL,
            text text NOT NULL,
            button_link character varying(200) NOT NULL,
            button_text character varying(10) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.jobs_thankyoupage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.jobs_thankyoupage_id_seq OWNED BY issue4081.jobs_thankyoupage.id;
        
        
        
        CREATE TABLE issue4081.landing_landingpage (
            id integer NOT NULL,
            section_one_title character varying(300) NOT NULL,
            section_two_title character varying(300) NOT NULL,
            section_two_text text NOT NULL,
            section_three_title character varying(300) NOT NULL,
            section_three_text text NOT NULL,
            section_four_title character varying(300) NOT NULL,
            section_four_text text NOT NULL,
            section_one_image character varying(100) NOT NULL,
            section_three_image character varying(100) NOT NULL,
            section_two_image character varying(100) NOT NULL,
            section_four_image character varying(100) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.landing_landingpage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.landing_landingpage_id_seq OWNED BY issue4081.landing_landingpage.id;
        
        
        
        CREATE TABLE issue4081.languages_language (
            id integer NOT NULL,
            language character varying(300) NOT NULL,
            language_code character varying(7) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.languages_language_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.languages_language_id_seq OWNED BY issue4081.languages_language.id;
        
        
        
        CREATE TABLE issue4081.media_artist (
            id integer NOT NULL,
            artist character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.media_artist_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.media_artist_id_seq OWNED BY issue4081.media_artist.id;
        
        
        
        CREATE TABLE issue4081.media_category (
            id integer NOT NULL,
            category character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.media_category_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.media_category_id_seq OWNED BY issue4081.media_category.id;
        
        
        
        CREATE TABLE issue4081.media_genre (
            id integer NOT NULL,
            genre character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.media_genre_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.media_genre_id_seq OWNED BY issue4081.media_genre.id;
        
        
        
        CREATE TABLE issue4081.media_media (
            id integer NOT NULL,
            approved boolean NOT NULL,
            title character varying(300) NOT NULL,
            note text NOT NULL,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            artist_id integer NOT NULL,
            created_by_id integer,
            genre_id integer,
            media_language_id integer NOT NULL,
            region_id integer,
            translation_language_id integer NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE TABLE issue4081.media_media_categories (
            id integer NOT NULL,
            media_id integer NOT NULL,
            category_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.media_media_categories_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.media_media_categories_id_seq OWNED BY issue4081.media_media_categories.id;
        
        
        
        CREATE SEQUENCE issue4081.media_media_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.media_media_id_seq OWNED BY issue4081.media_media.id;
        
        
        
        CREATE TABLE issue4081.media_mediasentence (
            id integer NOT NULL,
            repeated_in_line character varying(140) NOT NULL,
            place_in_line integer NOT NULL,
            word_count integer NOT NULL,
            lyrics_sentence character varying(280) NOT NULL,
            lyrics_translation character varying(280) NOT NULL,
            note character varying(1000) NOT NULL,
            approved boolean NOT NULL,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            created_by_id integer,
            learning_sentence_id integer NOT NULL,
            updated_by_id integer,
            media_id integer NOT NULL,
            "order" integer NOT NULL,
            line integer NOT NULL,
            repeat boolean NOT NULL,
            repeat_of_line integer NOT NULL,
            lyrics_translation_chunks character varying(280) NOT NULL,
            multiple boolean NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.media_mediasentence_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.media_mediasentence_id_seq OWNED BY issue4081.media_mediasentence.id;
        
        
        
        CREATE TABLE issue4081.media_region (
            id integer NOT NULL,
            region character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.media_region_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.media_region_id_seq OWNED BY issue4081.media_region.id;
        
        
        
        CREATE TABLE issue4081.sentences_customhint (
            id integer NOT NULL,
            hint text NOT NULL,
            hint_type_id integer NOT NULL,
            sentence_id integer,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_customhint_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_customhint_id_seq OWNED BY issue4081.sentences_customhint.id;
        
        
        
        CREATE TABLE issue4081.sentences_ecast (
            id integer NOT NULL,
            description text NOT NULL,
            action_id integer NOT NULL,
            character_id integer NOT NULL,
            emotion_id integer NOT NULL,
            sentence_id integer NOT NULL,
            setting_id integer NOT NULL,
            theme_id integer,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_ecast_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_ecast_id_seq OWNED BY issue4081.sentences_ecast.id;
        
        
        
        CREATE TABLE issue4081.sentences_ecastaction (
            id integer NOT NULL,
            action character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_ecastaction_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_ecastaction_id_seq OWNED BY issue4081.sentences_ecastaction.id;
        
        
        
        CREATE TABLE issue4081.sentences_ecastcharacter (
            id integer NOT NULL,
            "character" character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_ecastcharacter_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_ecastcharacter_id_seq OWNED BY issue4081.sentences_ecastcharacter.id;
        
        
        
        CREATE TABLE issue4081.sentences_ecastemotion (
            id integer NOT NULL,
            emotion character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_ecastemotion_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_ecastemotion_id_seq OWNED BY issue4081.sentences_ecastemotion.id;
        
        
        
        CREATE TABLE issue4081.sentences_ecastsetting (
            id integer NOT NULL,
            setting character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_ecastsetting_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_ecastsetting_id_seq OWNED BY issue4081.sentences_ecastsetting.id;
        
        
        
        CREATE TABLE issue4081.sentences_ecasttheme (
            id integer NOT NULL,
            theme character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_ecasttheme_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_ecasttheme_id_seq OWNED BY issue4081.sentences_ecasttheme.id;
        
        
        
        CREATE TABLE issue4081.sentences_hint (
            id integer NOT NULL,
            hint text NOT NULL,
            hint_type_id integer NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_hint_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_hint_id_seq OWNED BY issue4081.sentences_hint.id;
        
        
        
        CREATE TABLE issue4081.sentences_hinttype (
            id integer NOT NULL,
            type_name character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_hinttype_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_hinttype_id_seq OWNED BY issue4081.sentences_hinttype.id;
        
        
        
        CREATE TABLE issue4081.sentences_imagecategory (
            id integer NOT NULL,
            category character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_imagecategory_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_imagecategory_id_seq OWNED BY issue4081.sentences_imagecategory.id;
        
        
        
        CREATE TABLE issue4081.sentences_imagetype (
            id integer NOT NULL,
            name character varying(300) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_imagetype_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_imagetype_id_seq OWNED BY issue4081.sentences_imagetype.id;
        
        
        
        CREATE TABLE issue4081.sentences_posttranslationword (
            id integer NOT NULL,
            post character varying(140) NOT NULL,
            language_id integer,
            note text NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_posttranslationword_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_posttranslationword_id_seq OWNED BY issue4081.sentences_posttranslationword.id;
        
        
        
        CREATE TABLE issue4081.sentences_pretranslationword (
            id integer NOT NULL,
            pre character varying(140) NOT NULL,
            language_id integer,
            note text NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_pretranslationword_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_pretranslationword_id_seq OWNED BY issue4081.sentences_pretranslationword.id;
        
        
        
        CREATE TABLE issue4081.sentences_sentence (
            id integer NOT NULL,
            to_check character varying(300),
            note character varying(300),
            chunk_id integer,
            fifth_chunk_id integer,
            first_chunk_id integer,
            fourth_chunk_id integer,
            learning_language_id integer,
            second_chunk_id integer,
            sentence_category_id integer,
            sentence_audio_id integer,
            seventh_chunk_id integer,
            sixth_chunk_id integer,
            third_chunk_id integer,
            translation_language_id integer,
            approved boolean NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer,
            english_sentence_punctuation_end character varying(140) NOT NULL,
            english_sentence_punctuation_start character varying(140) NOT NULL,
            learning_sentence_punctuation_end character varying(140) NOT NULL,
            learning_sentence_punctuation_start character varying(140) NOT NULL,
            translation_sentence_punctuation_end character varying(140) NOT NULL,
            translation_sentence_punctuation_start character varying(140) NOT NULL,
            first_post_id integer,
            first_pre_id integer,
            first_underlined boolean NOT NULL,
            first_order boolean NOT NULL,
            fifth_order boolean NOT NULL,
            fifth_post_id integer,
            fifth_pre_id integer,
            fifth_underlined boolean NOT NULL,
            fourth_order boolean NOT NULL,
            fourth_post_id integer,
            fourth_pre_id integer,
            fourth_underlined boolean NOT NULL,
            second_order boolean NOT NULL,
            second_post_id integer,
            second_pre_id integer,
            second_underlined boolean NOT NULL,
            seventh_order boolean NOT NULL,
            seventh_post_id integer,
            seventh_pre_id integer,
            seventh_underlined boolean NOT NULL,
            sixth_order boolean NOT NULL,
            sixth_post_id integer,
            sixth_pre_id integer,
            sixth_underlined boolean NOT NULL,
            third_order boolean NOT NULL,
            third_post_id integer,
            third_pre_id integer,
            third_underlined boolean NOT NULL
        );
        
        
        
        
        CREATE TABLE issue4081.sentences_sentence_hints (
            id integer NOT NULL,
            sentence_id integer NOT NULL,
            hint_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_sentence_hints_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_sentence_hints_id_seq OWNED BY issue4081.sentences_sentence_hints.id;
        
        
        
        CREATE SEQUENCE issue4081.sentences_sentence_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_sentence_id_seq OWNED BY issue4081.sentences_sentence.id;
        
        
        
        CREATE TABLE issue4081.sentences_sentenceaudio (
            id integer NOT NULL,
            sentence_in_audio character varying(300) NOT NULL,
            audio_file character varying(100),
            audio_language_id integer NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_sentenceaudio_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_sentenceaudio_id_seq OWNED BY issue4081.sentences_sentenceaudio.id;
        
        
        
        CREATE TABLE issue4081.sentences_sentencecategory (
            id integer NOT NULL,
            category_type character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_sentencecategory_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_sentencecategory_id_seq OWNED BY issue4081.sentences_sentencecategory.id;
        
        
        
        CREATE TABLE issue4081.sentences_sentencegluer (
            id integer NOT NULL,
            learning_sentence_id integer NOT NULL,
            translation_sentence_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_sentencegluer_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_sentencegluer_id_seq OWNED BY issue4081.sentences_sentencegluer.id;
        
        
        
        CREATE TABLE issue4081.sentences_sentenceimage (
            id integer NOT NULL,
            file character varying(100),
            description text NOT NULL,
            image_category_id integer NOT NULL,
            image_text_language_id integer,
            sentence_id integer,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_sentenceimage_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_sentenceimage_id_seq OWNED BY issue4081.sentences_sentenceimage.id;
        
        
        
        CREATE TABLE issue4081.sentences_sentencevideo (
            id integer NOT NULL,
            file character varying(100),
            description text NOT NULL,
            audio_language_id integer NOT NULL,
            sentence_id integer,
            video_category_id integer NOT NULL,
            video_thumbnail character varying(100),
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_sentencevideo_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_sentencevideo_id_seq OWNED BY issue4081.sentences_sentencevideo.id;
        
        
        
        CREATE TABLE issue4081.sentences_videocategory (
            id integer NOT NULL,
            category character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.sentences_videocategory_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.sentences_videocategory_id_seq OWNED BY issue4081.sentences_videocategory.id;
        
        
        
        CREATE TABLE issue4081.socialaccount_socialaccount (
            id integer NOT NULL,
            provider character varying(30) NOT NULL,
            uid character varying(191) NOT NULL,
            last_login timestamp with time zone NOT NULL,
            date_joined timestamp with time zone NOT NULL,
            extra_data text NOT NULL,
            user_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.socialaccount_socialaccount_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.socialaccount_socialaccount_id_seq OWNED BY issue4081.socialaccount_socialaccount.id;
        
        
        
        CREATE TABLE issue4081.socialaccount_socialapp (
            id integer NOT NULL,
            provider character varying(30) NOT NULL,
            name character varying(40) NOT NULL,
            client_id character varying(191) NOT NULL,
            secret character varying(191) NOT NULL,
            key character varying(191) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.socialaccount_socialapp_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.socialaccount_socialapp_id_seq OWNED BY issue4081.socialaccount_socialapp.id;
        
        
        
        CREATE TABLE issue4081.socialaccount_socialapp_sites (
            id integer NOT NULL,
            socialapp_id integer NOT NULL,
            site_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.socialaccount_socialapp_sites_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.socialaccount_socialapp_sites_id_seq OWNED BY issue4081.socialaccount_socialapp_sites.id;
        
        
        
        CREATE TABLE issue4081.socialaccount_socialtoken (
            id integer NOT NULL,
            token text NOT NULL,
            token_secret text NOT NULL,
            expires_at timestamp with time zone,
            account_id integer NOT NULL,
            app_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.socialaccount_socialtoken_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.socialaccount_socialtoken_id_seq OWNED BY issue4081.socialaccount_socialtoken.id;
        
        
        
        CREATE TABLE issue4081.users_emailnotifications (
            id integer NOT NULL,
            events boolean NOT NULL,
            promotions boolean NOT NULL,
            new_features boolean NOT NULL,
            news boolean NOT NULL,
            progress boolean NOT NULL,
            user_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_emailnotifications_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_emailnotifications_id_seq OWNED BY issue4081.users_emailnotifications.id;
        
        
        
        CREATE TABLE issue4081.users_profile (
            id integer NOT NULL,
            profile_picture character varying(100) NOT NULL,
            censored_content boolean NOT NULL,
            date_of_birth date,
            gender character varying(10) NOT NULL,
            country character varying(2) NOT NULL,
            learning_language_id integer,
            speaking_language_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_profile_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_profile_id_seq OWNED BY issue4081.users_profile.id;
        
        
        
        CREATE TABLE issue4081.users_spotssettings (
            id integer NOT NULL,
            autoplay_word_audio boolean NOT NULL,
            autoplay_sentence_audio boolean NOT NULL,
            play_videos boolean NOT NULL,
            show_main_images boolean NOT NULL,
            show_submitted_images boolean NOT NULL,
            reveal_seconds integer NOT NULL,
            next_word_seconds integer NOT NULL,
            show_keyboard_shortcuts boolean NOT NULL,
            user_id integer NOT NULL,
            spots_page_auto boolean NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_spotssettings_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_spotssettings_id_seq OWNED BY issue4081.users_spotssettings.id;
        
        
        
        CREATE TABLE issue4081.users_subscription (
            id integer NOT NULL,
            expiration_date date NOT NULL,
            expired boolean NOT NULL,
            subscription_type_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_subscription_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_subscription_id_seq OWNED BY issue4081.users_subscription.id;
        
        
        
        CREATE TABLE issue4081.users_subscriptiontype (
            id integer NOT NULL,
            price numeric(6,2) NOT NULL,
            name character varying(140) NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_subscriptiontype_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_subscriptiontype_id_seq OWNED BY issue4081.users_subscriptiontype.id;
        
        
        
        CREATE TABLE issue4081.users_user (
            id integer NOT NULL,
            password character varying(128) NOT NULL,
            last_login timestamp with time zone,
            is_superuser boolean NOT NULL,
            username character varying(150) NOT NULL,
            first_name character varying(30) NOT NULL,
            last_name character varying(30) NOT NULL,
            email character varying(254) NOT NULL,
            is_staff boolean NOT NULL,
            is_active boolean NOT NULL,
            date_joined timestamp with time zone NOT NULL,
            free_account boolean NOT NULL,
            first_time boolean NOT NULL,
            profile_id integer
        );
        
        
        
        
        CREATE TABLE issue4081.users_user_groups (
            id integer NOT NULL,
            user_id integer NOT NULL,
            group_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_user_groups_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_user_groups_id_seq OWNED BY issue4081.users_user_groups.id;
        
        
        
        CREATE SEQUENCE issue4081.users_user_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_user_id_seq OWNED BY issue4081.users_user.id;
        
        
        
        CREATE TABLE issue4081.users_user_user_permissions (
            id integer NOT NULL,
            user_id integer NOT NULL,
            permission_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_user_user_permissions_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_user_user_permissions_id_seq OWNED BY issue4081.users_user_user_permissions.id;
        
        
        
        CREATE TABLE issue4081.users_vocabularysettings (
            id integer NOT NULL,
            word_audio_play_click boolean NOT NULL,
            sentence_audio_play_click boolean NOT NULL,
            show_keyboard_shortcuts boolean NOT NULL,
            user_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.users_vocabularysettings_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.users_vocabularysettings_id_seq OWNED BY issue4081.users_vocabularysettings.id;
        
        
        
        CREATE TABLE issue4081.words_chunk (
            id integer NOT NULL,
            learning_word_id integer NOT NULL,
            translation_word_id integer NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer,
            vocab boolean NOT NULL,
            chunk_type_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_chunk_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_chunk_id_seq OWNED BY issue4081.words_chunk.id;
        
        
        
        CREATE TABLE issue4081.words_chunktype (
            id integer NOT NULL,
            chunk_type character varying(140),
            note text NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_chunktype_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_chunktype_id_seq OWNED BY issue4081.words_chunktype.id;
        
        
        
        CREATE TABLE issue4081.words_englishword (
            id integer NOT NULL,
            word_in_english character varying(300) NOT NULL,
            definition text NOT NULL,
            grammar_id integer NOT NULL,
            example_sentence text NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            english_word_note text NOT NULL,
            modified_date timestamp with time zone,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_englishword_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_englishword_id_seq OWNED BY issue4081.words_englishword.id;
        
        
        
        CREATE TABLE issue4081.words_gender (
            id integer NOT NULL,
            gender character varying(140) NOT NULL,
            note text NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_gender_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_gender_id_seq OWNED BY issue4081.words_gender.id;
        
        
        
        CREATE TABLE issue4081.words_grammar (
            id integer NOT NULL,
            grammar_note text NOT NULL,
            name character varying(300) NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer,
            examples text NOT NULL,
            syntax character varying(300) NOT NULL,
            grammar_group character varying(300) NOT NULL,
            user_grammar character varying(300) NOT NULL,
            tense_type_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_grammar_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_grammar_id_seq OWNED BY issue4081.words_grammar.id;
        
        
        
        CREATE TABLE issue4081.words_knownword (
            id integer NOT NULL,
            created_at timestamp with time zone NOT NULL,
            user_id integer NOT NULL,
            word_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_knownword_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_knownword_id_seq OWNED BY issue4081.words_knownword.id;
        
        
        
        CREATE TABLE issue4081.words_masteredword (
            id integer NOT NULL,
            created_at timestamp with time zone NOT NULL,
            user_id integer NOT NULL,
            word_id integer NOT NULL
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_masteredword_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_masteredword_id_seq OWNED BY issue4081.words_masteredword.id;
        
        
        
        CREATE TABLE issue4081.words_person (
            id integer NOT NULL,
            person character varying(140) NOT NULL,
            note text NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_person_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_person_id_seq OWNED BY issue4081.words_person.id;
        
        
        
        CREATE TABLE issue4081.words_tensetype (
            id integer NOT NULL,
            tense_type character varying(140) NOT NULL,
            note text NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_tensetype_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_tensetype_id_seq OWNED BY issue4081.words_tensetype.id;
        
        
        
        CREATE TABLE issue4081.words_word (
            id integer NOT NULL,
            word character varying(300) NOT NULL,
            word_language_id integer NOT NULL,
            word_in_english_id integer NOT NULL,
            word_audio_id integer,
            word_grammar_id integer,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer,
            gender_id integer,
            person_id integer,
            number character varying(100)
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_word_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_word_id_seq OWNED BY issue4081.words_word.id;
        
        
        
        CREATE TABLE issue4081.words_wordaudio (
            id integer NOT NULL,
            word_in_audio character varying(300) NOT NULL,
            ipa character varying(600) NOT NULL,
            example_sentence text NOT NULL,
            audio_file character varying(100),
            audio_language_id integer NOT NULL,
            created_by_id integer,
            created_date timestamp with time zone,
            modified_date timestamp with time zone,
            note text NOT NULL,
            updated_by_id integer
        );
        
        
        
        
        CREATE SEQUENCE issue4081.words_wordaudio_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
        
        
        
        
        ALTER SEQUENCE issue4081.words_wordaudio_id_seq OWNED BY issue4081.words_wordaudio.id;
        
        
        
        ALTER TABLE ONLY issue4081.about_aboutpage ALTER COLUMN id SET DEFAULT nextval('issue4081.about_aboutpage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.about_teammember ALTER COLUMN id SET DEFAULT nextval('issue4081.about_teammember_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.account_emailaddress ALTER COLUMN id SET DEFAULT nextval('issue4081.account_emailaddress_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.account_emailconfirmation ALTER COLUMN id SET DEFAULT nextval('issue4081.account_emailconfirmation_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.auth_group ALTER COLUMN id SET DEFAULT nextval('issue4081.auth_group_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.auth_group_permissions ALTER COLUMN id SET DEFAULT nextval('issue4081.auth_group_permissions_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.auth_permission ALTER COLUMN id SET DEFAULT nextval('issue4081.auth_permission_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.contact_contactformcontent ALTER COLUMN id SET DEFAULT nextval('issue4081.contact_contactform_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.contact_contactpage ALTER COLUMN id SET DEFAULT nextval('issue4081.contact_contactpage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.contact_thankyoupage ALTER COLUMN id SET DEFAULT nextval('issue4081.contact_thankyoupage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.django_admin_log ALTER COLUMN id SET DEFAULT nextval('issue4081.django_admin_log_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.django_content_type ALTER COLUMN id SET DEFAULT nextval('issue4081.django_content_type_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.django_migrations ALTER COLUMN id SET DEFAULT nextval('issue4081.django_migrations_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.django_site ALTER COLUMN id SET DEFAULT nextval('issue4081.django_site_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.events_event ALTER COLUMN id SET DEFAULT nextval('issue4081.events_event_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.events_eventspage ALTER COLUMN id SET DEFAULT nextval('issue4081.events_eventpage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.help_helppage ALTER COLUMN id SET DEFAULT nextval('issue4081.help_helppage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.help_helpquestion ALTER COLUMN id SET DEFAULT nextval('issue4081.help_helpquestion_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.help_helpsection ALTER COLUMN id SET DEFAULT nextval('issue4081.help_helpsection_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.jobs_job ALTER COLUMN id SET DEFAULT nextval('issue4081.jobs_job_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.jobs_jobspage ALTER COLUMN id SET DEFAULT nextval('issue4081.jobs_jobspage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.jobs_nojobspage ALTER COLUMN id SET DEFAULT nextval('issue4081.jobs_nojobspage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.jobs_thankyoupage ALTER COLUMN id SET DEFAULT nextval('issue4081.jobs_thankyoupage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.landing_landingpage ALTER COLUMN id SET DEFAULT nextval('issue4081.landing_landingpage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.languages_language ALTER COLUMN id SET DEFAULT nextval('issue4081.languages_language_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.media_artist ALTER COLUMN id SET DEFAULT nextval('issue4081.media_artist_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.media_category ALTER COLUMN id SET DEFAULT nextval('issue4081.media_category_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.media_genre ALTER COLUMN id SET DEFAULT nextval('issue4081.media_genre_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.media_media ALTER COLUMN id SET DEFAULT nextval('issue4081.media_media_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.media_media_categories ALTER COLUMN id SET DEFAULT nextval('issue4081.media_media_categories_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.media_mediasentence ALTER COLUMN id SET DEFAULT nextval('issue4081.media_mediasentence_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.media_region ALTER COLUMN id SET DEFAULT nextval('issue4081.media_region_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_customhint ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_customhint_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_ecast ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_ecast_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_ecastaction ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_ecastaction_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_ecastcharacter ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_ecastcharacter_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_ecastemotion ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_ecastemotion_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_ecastsetting ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_ecastsetting_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_ecasttheme ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_ecasttheme_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_hint ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_hint_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_hinttype ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_hinttype_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_imagecategory ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_imagecategory_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_imagetype ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_imagetype_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_posttranslationword ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_posttranslationword_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_pretranslationword ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_pretranslationword_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_sentence ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_sentence_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_sentence_hints ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_sentence_hints_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_sentenceaudio ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_sentenceaudio_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_sentencecategory ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_sentencecategory_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_sentencegluer ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_sentencegluer_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_sentenceimage ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_sentenceimage_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_sentencevideo ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_sentencevideo_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.sentences_videocategory ALTER COLUMN id SET DEFAULT nextval('issue4081.sentences_videocategory_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.socialaccount_socialaccount ALTER COLUMN id SET DEFAULT nextval('issue4081.socialaccount_socialaccount_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.socialaccount_socialapp ALTER COLUMN id SET DEFAULT nextval('issue4081.socialaccount_socialapp_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.socialaccount_socialapp_sites ALTER COLUMN id SET DEFAULT nextval('issue4081.socialaccount_socialapp_sites_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.socialaccount_socialtoken ALTER COLUMN id SET DEFAULT nextval('issue4081.socialaccount_socialtoken_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_emailnotifications ALTER COLUMN id SET DEFAULT nextval('issue4081.users_emailnotifications_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_profile ALTER COLUMN id SET DEFAULT nextval('issue4081.users_profile_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_spotssettings ALTER COLUMN id SET DEFAULT nextval('issue4081.users_spotssettings_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_subscription ALTER COLUMN id SET DEFAULT nextval('issue4081.users_subscription_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_subscriptiontype ALTER COLUMN id SET DEFAULT nextval('issue4081.users_subscriptiontype_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_user ALTER COLUMN id SET DEFAULT nextval('issue4081.users_user_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_user_groups ALTER COLUMN id SET DEFAULT nextval('issue4081.users_user_groups_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_user_user_permissions ALTER COLUMN id SET DEFAULT nextval('issue4081.users_user_user_permissions_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.users_vocabularysettings ALTER COLUMN id SET DEFAULT nextval('issue4081.users_vocabularysettings_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_chunk ALTER COLUMN id SET DEFAULT nextval('issue4081.words_chunk_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_chunktype ALTER COLUMN id SET DEFAULT nextval('issue4081.words_chunktype_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_englishword ALTER COLUMN id SET DEFAULT nextval('issue4081.words_englishword_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_gender ALTER COLUMN id SET DEFAULT nextval('issue4081.words_gender_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_grammar ALTER COLUMN id SET DEFAULT nextval('issue4081.words_grammar_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_knownword ALTER COLUMN id SET DEFAULT nextval('issue4081.words_knownword_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_masteredword ALTER COLUMN id SET DEFAULT nextval('issue4081.words_masteredword_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_person ALTER COLUMN id SET DEFAULT nextval('issue4081.words_person_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_tensetype ALTER COLUMN id SET DEFAULT nextval('issue4081.words_tensetype_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_word ALTER COLUMN id SET DEFAULT nextval('issue4081.words_word_id_seq'::regclass);
        
        
        
        ALTER TABLE ONLY issue4081.words_wordaudio ALTER COLUMN id SET DEFAULT nextval('issue4081.words_wordaudio_id_seq'::regclass);`,
      'issue4081')
  })
})
