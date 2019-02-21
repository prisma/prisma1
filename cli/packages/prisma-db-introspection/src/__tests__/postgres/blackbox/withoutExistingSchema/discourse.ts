import testSchema from '../common'

describe('Introspector', () => {
  test('discourse', async () => {
    await testSchema(`--
    -- PostgreSQL database dump
    --
    
    -- Dumped from database version 10.6
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
    -- Name: hstore; Type: EXTENSION; Schema: -; Owner: -
    --
    
    CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;
    
    
    --
    -- Name: EXTENSION hstore; Type: COMMENT; Schema: -; Owner: -
    --
    
    COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';
    
    
    --
    -- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
    --
    
    CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
    
    
    --
    -- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: -
    --
    
    COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';
    
    
    SET default_tablespace = '';
    
    SET default_with_oids = false;
    
    --
    -- Name: api_keys; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.api_keys (
        id integer NOT NULL,
        key character varying(64) NOT NULL,
        user_id integer,
        created_by_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        allowed_ips inet[],
        hidden boolean DEFAULT false NOT NULL
    );
    
    
    --
    -- Name: api_keys_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.api_keys_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: api_keys_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.api_keys_id_seq OWNED BY public.api_keys.id;
    
    
    --
    -- Name: application_requests; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.application_requests (
        id integer NOT NULL,
        date date NOT NULL,
        req_type integer NOT NULL,
        count integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: application_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.application_requests_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: application_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.application_requests_id_seq OWNED BY public.application_requests.id;
    
    
    --
    -- Name: ar_internal_metadata; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.ar_internal_metadata (
        key character varying NOT NULL,
        value character varying,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: badge_groupings; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.badge_groupings (
        id integer NOT NULL,
        name character varying NOT NULL,
        description text,
        "position" integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: badge_groupings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.badge_groupings_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: badge_groupings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.badge_groupings_id_seq OWNED BY public.badge_groupings.id;
    
    
    --
    -- Name: categories; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.categories (
        id integer NOT NULL,
        name character varying(50) NOT NULL,
        color character varying(6) DEFAULT 'AB9364'::character varying NOT NULL,
        topic_id integer,
        topic_count integer DEFAULT 0 NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        user_id integer NOT NULL,
        topics_year integer DEFAULT 0,
        topics_month integer DEFAULT 0,
        topics_week integer DEFAULT 0,
        slug character varying NOT NULL,
        description text,
        text_color character varying(6) DEFAULT 'FFFFFF'::character varying NOT NULL,
        read_restricted boolean DEFAULT false NOT NULL,
        auto_close_hours double precision,
        post_count integer DEFAULT 0 NOT NULL,
        latest_post_id integer,
        latest_topic_id integer,
        "position" integer,
        parent_category_id integer,
        posts_year integer DEFAULT 0,
        posts_month integer DEFAULT 0,
        posts_week integer DEFAULT 0,
        email_in character varying,
        email_in_allow_strangers boolean DEFAULT false,
        topics_day integer DEFAULT 0,
        posts_day integer DEFAULT 0,
        allow_badges boolean DEFAULT true NOT NULL,
        name_lower character varying(50) NOT NULL,
        auto_close_based_on_last_post boolean DEFAULT false,
        topic_template text,
        contains_messages boolean,
        sort_order character varying,
        sort_ascending boolean,
        uploaded_logo_id integer,
        uploaded_background_id integer,
        topic_featured_link_allowed boolean DEFAULT true,
        all_topics_wiki boolean DEFAULT false NOT NULL,
        show_subcategory_list boolean DEFAULT false,
        num_featured_topics integer DEFAULT 3,
        default_view character varying(50),
        subcategory_list_style character varying(50) DEFAULT 'rows_with_featured_topics'::character varying,
        default_top_period character varying(20) DEFAULT 'all'::character varying,
        mailinglist_mirror boolean DEFAULT false NOT NULL,
        suppress_from_latest boolean DEFAULT false,
        minimum_required_tags integer DEFAULT 0,
        navigate_to_first_post_after_read boolean DEFAULT false NOT NULL
    );
    
    
    --
    -- Name: posts; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.posts (
        id integer NOT NULL,
        user_id integer,
        topic_id integer NOT NULL,
        post_number integer NOT NULL,
        raw text NOT NULL,
        cooked text NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        reply_to_post_number integer,
        reply_count integer DEFAULT 0 NOT NULL,
        quote_count integer DEFAULT 0 NOT NULL,
        deleted_at timestamp without time zone,
        off_topic_count integer DEFAULT 0 NOT NULL,
        like_count integer DEFAULT 0 NOT NULL,
        incoming_link_count integer DEFAULT 0 NOT NULL,
        bookmark_count integer DEFAULT 0 NOT NULL,
        avg_time integer,
        score double precision,
        reads integer DEFAULT 0 NOT NULL,
        post_type integer DEFAULT 1 NOT NULL,
        sort_order integer,
        last_editor_id integer,
        hidden boolean DEFAULT false NOT NULL,
        hidden_reason_id integer,
        notify_moderators_count integer DEFAULT 0 NOT NULL,
        spam_count integer DEFAULT 0 NOT NULL,
        illegal_count integer DEFAULT 0 NOT NULL,
        inappropriate_count integer DEFAULT 0 NOT NULL,
        last_version_at timestamp without time zone NOT NULL,
        user_deleted boolean DEFAULT false NOT NULL,
        reply_to_user_id integer,
        percent_rank double precision DEFAULT 1.0,
        notify_user_count integer DEFAULT 0 NOT NULL,
        like_score integer DEFAULT 0 NOT NULL,
        deleted_by_id integer,
        edit_reason character varying,
        word_count integer,
        version integer DEFAULT 1 NOT NULL,
        cook_method integer DEFAULT 1 NOT NULL,
        wiki boolean DEFAULT false NOT NULL,
        baked_at timestamp without time zone,
        baked_version integer,
        hidden_at timestamp without time zone,
        self_edits integer DEFAULT 0 NOT NULL,
        reply_quoted boolean DEFAULT false NOT NULL,
        via_email boolean DEFAULT false NOT NULL,
        raw_email text,
        public_version integer DEFAULT 1 NOT NULL,
        action_code character varying,
        image_url character varying,
        locked_by_id integer
    );
    
    
    --
    -- Name: TABLE posts; Type: COMMENT; Schema: public; Owner: -
    --
    
    COMMENT ON TABLE public.posts IS 'If you want to query public posts only, use the badge_posts view.';
    
    
    --
    -- Name: COLUMN posts.post_number; Type: COMMENT; Schema: public; Owner: -
    --
    
    COMMENT ON COLUMN public.posts.post_number IS 'The position of this post in the topic. The pair (topic_id, post_number) forms a natural key on the posts table.';
    
    
    --
    -- Name: COLUMN posts.raw; Type: COMMENT; Schema: public; Owner: -
    --
    
    COMMENT ON COLUMN public.posts.raw IS 'The raw Markdown that the user entered into the composer.';
    
    
    --
    -- Name: COLUMN posts.cooked; Type: COMMENT; Schema: public; Owner: -
    --
    
    COMMENT ON COLUMN public.posts.cooked IS 'The processed HTML that is presented in a topic.';
    
    
    --
    -- Name: COLUMN posts.reply_to_post_number; Type: COMMENT; Schema: public; Owner: -
    --
    
    COMMENT ON COLUMN public.posts.reply_to_post_number IS 'If this post is a reply to another, this column is the post_number of the post it''s replying to. [FKEY posts.topic_id, posts.post_number]';
    
    
    --
    -- Name: COLUMN posts.reply_quoted; Type: COMMENT; Schema: public; Owner: -
    --
    
    COMMENT ON COLUMN public.posts.reply_quoted IS 'This column is true if the post contains a quote-reply, which causes the in-reply-to indicator to be absent.';
    
    
    --
    -- Name: topics; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topics (
        id integer NOT NULL,
        title character varying NOT NULL,
        last_posted_at timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        views integer DEFAULT 0 NOT NULL,
        posts_count integer DEFAULT 0 NOT NULL,
        user_id integer,
        last_post_user_id integer NOT NULL,
        reply_count integer DEFAULT 0 NOT NULL,
        featured_user1_id integer,
        featured_user2_id integer,
        featured_user3_id integer,
        avg_time integer,
        deleted_at timestamp without time zone,
        highest_post_number integer DEFAULT 0 NOT NULL,
        image_url character varying,
        like_count integer DEFAULT 0 NOT NULL,
        incoming_link_count integer DEFAULT 0 NOT NULL,
        category_id integer,
        visible boolean DEFAULT true NOT NULL,
        moderator_posts_count integer DEFAULT 0 NOT NULL,
        closed boolean DEFAULT false NOT NULL,
        archived boolean DEFAULT false NOT NULL,
        bumped_at timestamp without time zone NOT NULL,
        has_summary boolean DEFAULT false NOT NULL,
        archetype character varying DEFAULT 'regular'::character varying NOT NULL,
        featured_user4_id integer,
        notify_moderators_count integer DEFAULT 0 NOT NULL,
        spam_count integer DEFAULT 0 NOT NULL,
        pinned_at timestamp without time zone,
        score double precision,
        percent_rank double precision DEFAULT 1.0 NOT NULL,
        subtype character varying,
        slug character varying,
        deleted_by_id integer,
        participant_count integer DEFAULT 1,
        word_count integer,
        excerpt character varying(1000),
        pinned_globally boolean DEFAULT false NOT NULL,
        pinned_until timestamp without time zone,
        fancy_title character varying(400),
        highest_staff_post_number integer DEFAULT 0 NOT NULL,
        featured_link character varying,
        CONSTRAINT has_category_id CHECK (((category_id IS NOT NULL) OR ((archetype)::text <> 'regular'::text))),
        CONSTRAINT pm_has_no_category CHECK (((category_id IS NULL) OR ((archetype)::text <> 'private_message'::text)))
    );
    
    
    --
    -- Name: TABLE topics; Type: COMMENT; Schema: public; Owner: -
    --
    
    COMMENT ON TABLE public.topics IS 'To query public topics only: SELECT ... FROM topics t LEFT INNER JOIN categories c ON (t.category_id = c.id AND c.read_restricted = false)';
    
    
    --
    -- Name: badge_posts; Type: VIEW; Schema: public; Owner: -
    --
    
    CREATE VIEW public.badge_posts AS
     SELECT p.id,
        p.user_id,
        p.topic_id,
        p.post_number,
        p.raw,
        p.cooked,
        p.created_at,
        p.updated_at,
        p.reply_to_post_number,
        p.reply_count,
        p.quote_count,
        p.deleted_at,
        p.off_topic_count,
        p.like_count,
        p.incoming_link_count,
        p.bookmark_count,
        p.avg_time,
        p.score,
        p.reads,
        p.post_type,
        p.sort_order,
        p.last_editor_id,
        p.hidden,
        p.hidden_reason_id,
        p.notify_moderators_count,
        p.spam_count,
        p.illegal_count,
        p.inappropriate_count,
        p.last_version_at,
        p.user_deleted,
        p.reply_to_user_id,
        p.percent_rank,
        p.notify_user_count,
        p.like_score,
        p.deleted_by_id,
        p.edit_reason,
        p.word_count,
        p.version,
        p.cook_method,
        p.wiki,
        p.baked_at,
        p.baked_version,
        p.hidden_at,
        p.self_edits,
        p.reply_quoted,
        p.via_email,
        p.raw_email,
        p.public_version,
        p.action_code,
        p.image_url,
        p.locked_by_id
       FROM ((public.posts p
         JOIN public.topics t ON ((t.id = p.topic_id)))
         JOIN public.categories c ON ((c.id = t.category_id)))
      WHERE (c.allow_badges AND (p.deleted_at IS NULL) AND (t.deleted_at IS NULL) AND (NOT c.read_restricted) AND t.visible AND (p.post_type = ANY (ARRAY[1, 2, 3])));
    
    
    --
    -- Name: badge_types; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.badge_types (
        id integer NOT NULL,
        name character varying NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: badge_types_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.badge_types_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: badge_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.badge_types_id_seq OWNED BY public.badge_types.id;
    
    
    --
    -- Name: badges; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.badges (
        id integer NOT NULL,
        name character varying NOT NULL,
        description text,
        badge_type_id integer NOT NULL,
        grant_count integer DEFAULT 0 NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        allow_title boolean DEFAULT false NOT NULL,
        multiple_grant boolean DEFAULT false NOT NULL,
        icon character varying DEFAULT 'fa-certificate'::character varying,
        listable boolean DEFAULT true,
        target_posts boolean DEFAULT false,
        query text,
        enabled boolean DEFAULT true NOT NULL,
        auto_revoke boolean DEFAULT true NOT NULL,
        badge_grouping_id integer DEFAULT 5 NOT NULL,
        trigger integer,
        show_posts boolean DEFAULT false NOT NULL,
        system boolean DEFAULT false NOT NULL,
        image character varying(255),
        long_description text
    );
    
    
    --
    -- Name: badges_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.badges_id_seq
        START WITH 100
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: badges_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.badges_id_seq OWNED BY public.badges.id;
    
    
    --
    -- Name: categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.categories_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;
    
    
    --
    -- Name: categories_web_hooks; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.categories_web_hooks (
        web_hook_id integer NOT NULL,
        category_id integer NOT NULL
    );
    
    
    --
    -- Name: category_custom_fields; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_custom_fields (
        id integer NOT NULL,
        category_id integer NOT NULL,
        name character varying(256) NOT NULL,
        value text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: category_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.category_custom_fields_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: category_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.category_custom_fields_id_seq OWNED BY public.category_custom_fields.id;
    
    
    --
    -- Name: category_featured_topics; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_featured_topics (
        category_id integer NOT NULL,
        topic_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        rank integer DEFAULT 0 NOT NULL,
        id integer NOT NULL
    );
    
    
    --
    -- Name: category_featured_topics_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.category_featured_topics_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: category_featured_topics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.category_featured_topics_id_seq OWNED BY public.category_featured_topics.id;
    
    
    --
    -- Name: category_groups; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_groups (
        id integer NOT NULL,
        category_id integer NOT NULL,
        group_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        permission_type integer DEFAULT 1
    );
    
    
    --
    -- Name: category_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.category_groups_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: category_groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.category_groups_id_seq OWNED BY public.category_groups.id;
    
    
    --
    -- Name: category_search_data; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_search_data (
        category_id integer NOT NULL,
        search_data tsvector,
        raw_data text,
        locale text,
        version integer DEFAULT 0
    );
    
    
    --
    -- Name: category_tag_groups; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_tag_groups (
        id integer NOT NULL,
        category_id integer NOT NULL,
        tag_group_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: category_tag_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.category_tag_groups_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: category_tag_groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.category_tag_groups_id_seq OWNED BY public.category_tag_groups.id;
    
    
    --
    -- Name: category_tag_stats; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_tag_stats (
        id bigint NOT NULL,
        category_id bigint NOT NULL,
        tag_id bigint NOT NULL,
        topic_count integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: category_tag_stats_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.category_tag_stats_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: category_tag_stats_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.category_tag_stats_id_seq OWNED BY public.category_tag_stats.id;
    
    
    --
    -- Name: category_tags; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_tags (
        id integer NOT NULL,
        category_id integer NOT NULL,
        tag_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: category_tags_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.category_tags_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: category_tags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.category_tags_id_seq OWNED BY public.category_tags.id;
    
    
    --
    -- Name: category_users; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.category_users (
        id integer NOT NULL,
        category_id integer NOT NULL,
        user_id integer NOT NULL,
        notification_level integer NOT NULL
    );
    
    
    --
    -- Name: category_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.category_users_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: category_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.category_users_id_seq OWNED BY public.category_users.id;
    
    
    --
    -- Name: child_themes; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.child_themes (
        id integer NOT NULL,
        parent_theme_id integer,
        child_theme_id integer,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: child_themes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.child_themes_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: child_themes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.child_themes_id_seq OWNED BY public.child_themes.id;
    
    
    --
    -- Name: color_scheme_colors; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.color_scheme_colors (
        id integer NOT NULL,
        name character varying NOT NULL,
        hex character varying NOT NULL,
        color_scheme_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: color_scheme_colors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.color_scheme_colors_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: color_scheme_colors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.color_scheme_colors_id_seq OWNED BY public.color_scheme_colors.id;
    
    
    --
    -- Name: color_schemes; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.color_schemes (
        id integer NOT NULL,
        name character varying NOT NULL,
        version integer DEFAULT 1 NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        via_wizard boolean DEFAULT false NOT NULL,
        base_scheme_id character varying,
        theme_id integer
    );
    
    
    --
    -- Name: color_schemes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.color_schemes_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: color_schemes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.color_schemes_id_seq OWNED BY public.color_schemes.id;
    
    
    --
    -- Name: custom_emojis; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.custom_emojis (
        id integer NOT NULL,
        name character varying NOT NULL,
        upload_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: custom_emojis_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.custom_emojis_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: custom_emojis_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.custom_emojis_id_seq OWNED BY public.custom_emojis.id;
    
    
    --
    -- Name: developers; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.developers (
        id integer NOT NULL,
        user_id integer NOT NULL
    );
    
    
    --
    -- Name: developers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.developers_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: developers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.developers_id_seq OWNED BY public.developers.id;
    
    
    --
    -- Name: directory_items; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.directory_items (
        id integer NOT NULL,
        period_type integer NOT NULL,
        user_id integer NOT NULL,
        likes_received integer NOT NULL,
        likes_given integer NOT NULL,
        topics_entered integer NOT NULL,
        topic_count integer NOT NULL,
        post_count integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        days_visited integer DEFAULT 0 NOT NULL,
        posts_read integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: directory_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.directory_items_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: directory_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.directory_items_id_seq OWNED BY public.directory_items.id;
    
    
    --
    -- Name: draft_sequences; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.draft_sequences (
        id integer NOT NULL,
        user_id integer NOT NULL,
        draft_key character varying NOT NULL,
        sequence integer NOT NULL
    );
    
    
    --
    -- Name: draft_sequences_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.draft_sequences_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: draft_sequences_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.draft_sequences_id_seq OWNED BY public.draft_sequences.id;
    
    
    --
    -- Name: drafts; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.drafts (
        id integer NOT NULL,
        user_id integer NOT NULL,
        draft_key character varying NOT NULL,
        data text NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        sequence integer DEFAULT 0 NOT NULL,
        revisions integer DEFAULT 1 NOT NULL
    );
    
    
    --
    -- Name: drafts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.drafts_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: drafts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.drafts_id_seq OWNED BY public.drafts.id;
    
    
    --
    -- Name: email_change_requests; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.email_change_requests (
        id integer NOT NULL,
        user_id integer NOT NULL,
        old_email character varying NOT NULL,
        new_email character varying NOT NULL,
        old_email_token_id integer,
        new_email_token_id integer,
        change_state integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: email_change_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.email_change_requests_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: email_change_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.email_change_requests_id_seq OWNED BY public.email_change_requests.id;
    
    
    --
    -- Name: email_logs; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.email_logs (
        id integer NOT NULL,
        to_address character varying NOT NULL,
        email_type character varying NOT NULL,
        user_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        post_id integer,
        bounce_key uuid,
        bounced boolean DEFAULT false NOT NULL,
        message_id character varying
    );
    
    
    --
    -- Name: email_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.email_logs_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: email_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.email_logs_id_seq OWNED BY public.email_logs.id;
    
    
    --
    -- Name: email_tokens; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.email_tokens (
        id integer NOT NULL,
        user_id integer NOT NULL,
        email character varying NOT NULL,
        token character varying NOT NULL,
        confirmed boolean DEFAULT false NOT NULL,
        expired boolean DEFAULT false NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: email_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.email_tokens_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: email_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.email_tokens_id_seq OWNED BY public.email_tokens.id;
    
    
    --
    -- Name: embeddable_hosts; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.embeddable_hosts (
        id integer NOT NULL,
        host character varying NOT NULL,
        category_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        path_whitelist character varying,
        class_name character varying
    );
    
    
    --
    -- Name: embeddable_hosts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.embeddable_hosts_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: embeddable_hosts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.embeddable_hosts_id_seq OWNED BY public.embeddable_hosts.id;
    
    
    --
    -- Name: facebook_user_infos; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.facebook_user_infos (
        id integer NOT NULL,
        user_id integer NOT NULL,
        facebook_user_id bigint NOT NULL,
        username character varying,
        first_name character varying,
        last_name character varying,
        email character varying,
        gender character varying,
        name character varying,
        link character varying,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        avatar_url character varying,
        about_me text,
        location character varying,
        website text
    );
    
    
    --
    -- Name: facebook_user_infos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.facebook_user_infos_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: facebook_user_infos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.facebook_user_infos_id_seq OWNED BY public.facebook_user_infos.id;
    
    
    --
    -- Name: github_user_infos; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.github_user_infos (
        id integer NOT NULL,
        user_id integer NOT NULL,
        screen_name character varying NOT NULL,
        github_user_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: github_user_infos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.github_user_infos_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: github_user_infos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.github_user_infos_id_seq OWNED BY public.github_user_infos.id;
    
    
    --
    -- Name: given_daily_likes; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.given_daily_likes (
        user_id integer NOT NULL,
        likes_given integer NOT NULL,
        given_date date NOT NULL,
        limit_reached boolean DEFAULT false NOT NULL
    );
    
    
    --
    -- Name: google_user_infos; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.google_user_infos (
        id integer NOT NULL,
        user_id integer NOT NULL,
        google_user_id character varying NOT NULL,
        first_name character varying,
        last_name character varying,
        email character varying,
        gender character varying,
        name character varying,
        link character varying,
        profile_link character varying,
        picture character varying,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: google_user_infos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.google_user_infos_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: google_user_infos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.google_user_infos_id_seq OWNED BY public.google_user_infos.id;
    
    
    --
    -- Name: group_archived_messages; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.group_archived_messages (
        id integer NOT NULL,
        group_id integer NOT NULL,
        topic_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: group_archived_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.group_archived_messages_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: group_archived_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.group_archived_messages_id_seq OWNED BY public.group_archived_messages.id;
    
    
    --
    -- Name: group_custom_fields; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.group_custom_fields (
        id integer NOT NULL,
        group_id integer NOT NULL,
        name character varying(256) NOT NULL,
        value text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: group_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.group_custom_fields_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: group_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.group_custom_fields_id_seq OWNED BY public.group_custom_fields.id;
    
    
    --
    -- Name: group_histories; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.group_histories (
        id integer NOT NULL,
        group_id integer NOT NULL,
        acting_user_id integer NOT NULL,
        target_user_id integer,
        action integer NOT NULL,
        subject character varying,
        prev_value text,
        new_value text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: group_histories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.group_histories_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: group_histories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.group_histories_id_seq OWNED BY public.group_histories.id;
    
    
    --
    -- Name: group_mentions; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.group_mentions (
        id integer NOT NULL,
        post_id integer,
        group_id integer,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: group_mentions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.group_mentions_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: group_mentions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.group_mentions_id_seq OWNED BY public.group_mentions.id;
    
    
    --
    -- Name: group_users; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.group_users (
        id integer NOT NULL,
        group_id integer NOT NULL,
        user_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        owner boolean DEFAULT false NOT NULL,
        notification_level integer DEFAULT 2 NOT NULL
    );
    
    
    --
    -- Name: group_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.group_users_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: group_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.group_users_id_seq OWNED BY public.group_users.id;
    
    
    --
    -- Name: groups; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.groups (
        id integer NOT NULL,
        name character varying NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        automatic boolean DEFAULT false NOT NULL,
        user_count integer DEFAULT 0 NOT NULL,
        automatic_membership_email_domains text,
        automatic_membership_retroactive boolean DEFAULT false,
        primary_group boolean DEFAULT false NOT NULL,
        title character varying,
        grant_trust_level integer,
        incoming_email character varying,
        has_messages boolean DEFAULT false NOT NULL,
        flair_url character varying,
        flair_bg_color character varying,
        flair_color character varying,
        bio_raw text,
        bio_cooked text,
        allow_membership_requests boolean DEFAULT false NOT NULL,
        full_name character varying,
        default_notification_level integer DEFAULT 3 NOT NULL,
        visibility_level integer DEFAULT 0 NOT NULL,
        public_exit boolean DEFAULT false NOT NULL,
        public_admission boolean DEFAULT false NOT NULL,
        membership_request_template text,
        messageable_level integer DEFAULT 0,
        mentionable_level integer DEFAULT 0
    );
    
    
    --
    -- Name: groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.groups_id_seq
        START WITH 100
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.groups_id_seq OWNED BY public.groups.id;
    
    
    --
    -- Name: groups_web_hooks; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.groups_web_hooks (
        web_hook_id integer NOT NULL,
        group_id integer NOT NULL
    );
    
    
    --
    -- Name: incoming_domains; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.incoming_domains (
        id integer NOT NULL,
        name character varying(100) NOT NULL,
        https boolean DEFAULT false NOT NULL,
        port integer NOT NULL
    );
    
    
    --
    -- Name: incoming_domains_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.incoming_domains_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: incoming_domains_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.incoming_domains_id_seq OWNED BY public.incoming_domains.id;
    
    
    --
    -- Name: incoming_emails; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.incoming_emails (
        id integer NOT NULL,
        user_id integer,
        topic_id integer,
        post_id integer,
        raw text,
        error text,
        message_id text,
        from_address text,
        to_addresses text,
        cc_addresses text,
        subject text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        rejection_message text,
        is_auto_generated boolean DEFAULT false,
        is_bounce boolean DEFAULT false NOT NULL
    );
    
    
    --
    -- Name: incoming_emails_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.incoming_emails_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: incoming_emails_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.incoming_emails_id_seq OWNED BY public.incoming_emails.id;
    
    
    --
    -- Name: incoming_links; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.incoming_links (
        id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        user_id integer,
        ip_address inet,
        current_user_id integer,
        post_id integer NOT NULL,
        incoming_referer_id integer
    );
    
    
    --
    -- Name: incoming_links_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.incoming_links_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: incoming_links_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.incoming_links_id_seq OWNED BY public.incoming_links.id;
    
    
    --
    -- Name: incoming_referers; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.incoming_referers (
        id integer NOT NULL,
        path character varying(1000) NOT NULL,
        incoming_domain_id integer NOT NULL
    );
    
    
    --
    -- Name: incoming_referers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.incoming_referers_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: incoming_referers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.incoming_referers_id_seq OWNED BY public.incoming_referers.id;
    
    
    --
    -- Name: instagram_user_infos; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.instagram_user_infos (
        id integer NOT NULL,
        user_id integer,
        screen_name character varying,
        instagram_user_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: instagram_user_infos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.instagram_user_infos_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: instagram_user_infos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.instagram_user_infos_id_seq OWNED BY public.instagram_user_infos.id;
    
    
    --
    -- Name: invited_groups; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.invited_groups (
        id integer NOT NULL,
        group_id integer,
        invite_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: invited_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.invited_groups_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: invited_groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.invited_groups_id_seq OWNED BY public.invited_groups.id;
    
    
    --
    -- Name: invites; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.invites (
        id integer NOT NULL,
        invite_key character varying(32) NOT NULL,
        email character varying,
        invited_by_id integer NOT NULL,
        user_id integer,
        redeemed_at timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        deleted_at timestamp without time zone,
        deleted_by_id integer,
        invalidated_at timestamp without time zone,
        moderator boolean DEFAULT false NOT NULL,
        custom_message text
    );
    
    
    --
    -- Name: invites_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.invites_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: invites_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.invites_id_seq OWNED BY public.invites.id;
    
    
    --
    -- Name: javascript_caches; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.javascript_caches (
        id bigint NOT NULL,
        theme_field_id bigint NOT NULL,
        digest character varying,
        content text NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: javascript_caches_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.javascript_caches_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: javascript_caches_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.javascript_caches_id_seq OWNED BY public.javascript_caches.id;
    
    
    --
    -- Name: message_bus; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.message_bus (
        id integer NOT NULL,
        name character varying,
        context character varying,
        data text,
        created_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: message_bus_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.message_bus_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: message_bus_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.message_bus_id_seq OWNED BY public.message_bus.id;
    
    
    --
    -- Name: muted_users; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.muted_users (
        id integer NOT NULL,
        user_id integer NOT NULL,
        muted_user_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: muted_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.muted_users_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: muted_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.muted_users_id_seq OWNED BY public.muted_users.id;
    
    
    --
    -- Name: notifications; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.notifications (
        id integer NOT NULL,
        notification_type integer NOT NULL,
        user_id integer NOT NULL,
        data character varying(1000) NOT NULL,
        read boolean DEFAULT false NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        topic_id integer,
        post_number integer,
        post_action_id integer
    );
    
    
    --
    -- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.notifications_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;
    
    
    --
    -- Name: oauth2_user_infos; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.oauth2_user_infos (
        id integer NOT NULL,
        user_id integer NOT NULL,
        uid character varying NOT NULL,
        provider character varying NOT NULL,
        email character varying,
        name character varying,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: oauth2_user_infos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.oauth2_user_infos_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: oauth2_user_infos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.oauth2_user_infos_id_seq OWNED BY public.oauth2_user_infos.id;
    
    
    --
    -- Name: onceoff_logs; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.onceoff_logs (
        id integer NOT NULL,
        job_name character varying,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: onceoff_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.onceoff_logs_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: onceoff_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.onceoff_logs_id_seq OWNED BY public.onceoff_logs.id;
    
    
    --
    -- Name: optimized_images; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.optimized_images (
        id integer NOT NULL,
        sha1 character varying(40) NOT NULL,
        extension character varying(10) NOT NULL,
        width integer NOT NULL,
        height integer NOT NULL,
        upload_id integer NOT NULL,
        url character varying NOT NULL,
        filesize integer
    );
    
    
    --
    -- Name: optimized_images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.optimized_images_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: optimized_images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.optimized_images_id_seq OWNED BY public.optimized_images.id;
    
    
    --
    -- Name: permalinks; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.permalinks (
        id integer NOT NULL,
        url character varying(1000) NOT NULL,
        topic_id integer,
        post_id integer,
        category_id integer,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        external_url character varying(1000)
    );
    
    
    --
    -- Name: permalinks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.permalinks_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: permalinks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.permalinks_id_seq OWNED BY public.permalinks.id;
    
    
    --
    -- Name: plugin_store_rows; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.plugin_store_rows (
        id integer NOT NULL,
        plugin_name character varying NOT NULL,
        key character varying NOT NULL,
        type_name character varying NOT NULL,
        value text
    );
    
    
    --
    -- Name: plugin_store_rows_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.plugin_store_rows_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: plugin_store_rows_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.plugin_store_rows_id_seq OWNED BY public.plugin_store_rows.id;
    
    
    --
    -- Name: post_action_types; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_action_types (
        name_key character varying(50) NOT NULL,
        is_flag boolean DEFAULT false NOT NULL,
        icon character varying(20),
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        id integer NOT NULL,
        "position" integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: post_action_types_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_action_types_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_action_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_action_types_id_seq OWNED BY public.post_action_types.id;
    
    
    --
    -- Name: post_actions; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_actions (
        id integer NOT NULL,
        post_id integer NOT NULL,
        user_id integer NOT NULL,
        post_action_type_id integer NOT NULL,
        deleted_at timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        deleted_by_id integer,
        related_post_id integer,
        staff_took_action boolean DEFAULT false NOT NULL,
        deferred_by_id integer,
        targets_topic boolean DEFAULT false NOT NULL,
        agreed_at timestamp without time zone,
        agreed_by_id integer,
        deferred_at timestamp without time zone,
        disagreed_at timestamp without time zone,
        disagreed_by_id integer
    );
    
    
    --
    -- Name: post_actions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_actions_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_actions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_actions_id_seq OWNED BY public.post_actions.id;
    
    
    --
    -- Name: post_custom_fields; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_custom_fields (
        id integer NOT NULL,
        post_id integer NOT NULL,
        name character varying(256) NOT NULL,
        value text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: post_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_custom_fields_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_custom_fields_id_seq OWNED BY public.post_custom_fields.id;
    
    
    --
    -- Name: post_details; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_details (
        id integer NOT NULL,
        post_id integer,
        key character varying,
        value character varying,
        extra text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: post_details_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_details_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_details_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_details_id_seq OWNED BY public.post_details.id;
    
    
    --
    -- Name: post_replies; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_replies (
        post_id integer,
        reply_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: post_reply_keys; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_reply_keys (
        id bigint NOT NULL,
        user_id integer NOT NULL,
        post_id integer NOT NULL,
        reply_key uuid NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: post_reply_keys_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_reply_keys_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_reply_keys_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_reply_keys_id_seq OWNED BY public.post_reply_keys.id;
    
    
    --
    -- Name: post_revisions; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_revisions (
        id integer NOT NULL,
        user_id integer,
        post_id integer,
        modifications text,
        number integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        hidden boolean DEFAULT false NOT NULL
    );
    
    
    --
    -- Name: post_revisions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_revisions_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_revisions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_revisions_id_seq OWNED BY public.post_revisions.id;
    
    
    --
    -- Name: post_search_data; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_search_data (
        post_id integer NOT NULL,
        search_data tsvector,
        raw_data text,
        locale character varying,
        version integer DEFAULT 0
    );
    
    
    --
    -- Name: post_stats; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_stats (
        id integer NOT NULL,
        post_id integer,
        drafts_saved integer,
        typing_duration_msecs integer,
        composer_open_duration_msecs integer,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: post_stats_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_stats_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_stats_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_stats_id_seq OWNED BY public.post_stats.id;
    
    
    --
    -- Name: post_timings; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_timings (
        topic_id integer NOT NULL,
        post_number integer NOT NULL,
        user_id integer NOT NULL,
        msecs integer NOT NULL
    );
    
    
    --
    -- Name: post_uploads; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.post_uploads (
        id integer NOT NULL,
        post_id integer NOT NULL,
        upload_id integer NOT NULL
    );
    
    
    --
    -- Name: post_uploads_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.post_uploads_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: post_uploads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.post_uploads_id_seq OWNED BY public.post_uploads.id;
    
    
    --
    -- Name: posts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.posts_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: posts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.posts_id_seq OWNED BY public.posts.id;
    
    
    --
    -- Name: push_subscriptions; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.push_subscriptions (
        id bigint NOT NULL,
        user_id integer NOT NULL,
        data character varying NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: push_subscriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.push_subscriptions_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: push_subscriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.push_subscriptions_id_seq OWNED BY public.push_subscriptions.id;
    
    
    --
    -- Name: queued_posts; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.queued_posts (
        id integer NOT NULL,
        queue character varying NOT NULL,
        state integer NOT NULL,
        user_id integer NOT NULL,
        raw text NOT NULL,
        post_options json NOT NULL,
        topic_id integer,
        approved_by_id integer,
        approved_at timestamp without time zone,
        rejected_by_id integer,
        rejected_at timestamp without time zone,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: queued_posts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.queued_posts_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: queued_posts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.queued_posts_id_seq OWNED BY public.queued_posts.id;
    
    
    --
    -- Name: quoted_posts; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.quoted_posts (
        id integer NOT NULL,
        post_id integer NOT NULL,
        quoted_post_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: quoted_posts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.quoted_posts_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: quoted_posts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.quoted_posts_id_seq OWNED BY public.quoted_posts.id;
    
    
    --
    -- Name: remote_themes; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.remote_themes (
        id integer NOT NULL,
        remote_url character varying NOT NULL,
        remote_version character varying,
        local_version character varying,
        about_url character varying,
        license_url character varying,
        commits_behind integer,
        remote_updated_at timestamp without time zone,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        private_key text,
        branch character varying,
        last_error_text text
    );
    
    
    --
    -- Name: remote_themes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.remote_themes_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: remote_themes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.remote_themes_id_seq OWNED BY public.remote_themes.id;
    
    
    --
    -- Name: scheduler_stats; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.scheduler_stats (
        id integer NOT NULL,
        name character varying NOT NULL,
        hostname character varying NOT NULL,
        pid integer NOT NULL,
        duration_ms integer,
        live_slots_start integer,
        live_slots_finish integer,
        started_at timestamp without time zone NOT NULL,
        success boolean,
        error text
    );
    
    
    --
    -- Name: scheduler_stats_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.scheduler_stats_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: scheduler_stats_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.scheduler_stats_id_seq OWNED BY public.scheduler_stats.id;
    
    
    --
    -- Name: schema_migration_details; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.schema_migration_details (
        id integer NOT NULL,
        version character varying NOT NULL,
        name character varying,
        hostname character varying,
        git_version character varying,
        rails_version character varying,
        duration integer,
        direction character varying,
        created_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: schema_migration_details_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.schema_migration_details_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: schema_migration_details_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.schema_migration_details_id_seq OWNED BY public.schema_migration_details.id;
    
    
    --
    -- Name: schema_migrations; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.schema_migrations (
        version character varying NOT NULL
    );
    
    
    --
    -- Name: screened_emails; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.screened_emails (
        id integer NOT NULL,
        email character varying NOT NULL,
        action_type integer NOT NULL,
        match_count integer DEFAULT 0 NOT NULL,
        last_match_at timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        ip_address inet
    );
    
    
    --
    -- Name: screened_emails_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.screened_emails_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: screened_emails_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.screened_emails_id_seq OWNED BY public.screened_emails.id;
    
    
    --
    -- Name: screened_ip_addresses; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.screened_ip_addresses (
        id integer NOT NULL,
        ip_address inet NOT NULL,
        action_type integer NOT NULL,
        match_count integer DEFAULT 0 NOT NULL,
        last_match_at timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: screened_ip_addresses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.screened_ip_addresses_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: screened_ip_addresses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.screened_ip_addresses_id_seq OWNED BY public.screened_ip_addresses.id;
    
    
    --
    -- Name: screened_urls; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.screened_urls (
        id integer NOT NULL,
        url character varying NOT NULL,
        domain character varying NOT NULL,
        action_type integer NOT NULL,
        match_count integer DEFAULT 0 NOT NULL,
        last_match_at timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        ip_address inet
    );
    
    
    --
    -- Name: screened_urls_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.screened_urls_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: screened_urls_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.screened_urls_id_seq OWNED BY public.screened_urls.id;
    
    
    --
    -- Name: search_logs; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.search_logs (
        id integer NOT NULL,
        term character varying NOT NULL,
        user_id integer,
        ip_address inet,
        search_result_id integer,
        search_type integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        search_result_type integer
    );
    
    
    --
    -- Name: search_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.search_logs_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: search_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.search_logs_id_seq OWNED BY public.search_logs.id;
    
    
    --
    -- Name: shared_drafts; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.shared_drafts (
        topic_id integer NOT NULL,
        category_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        id bigint NOT NULL
    );
    
    
    --
    -- Name: shared_drafts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.shared_drafts_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: shared_drafts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.shared_drafts_id_seq OWNED BY public.shared_drafts.id;
    
    
    --
    -- Name: single_sign_on_records; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.single_sign_on_records (
        id integer NOT NULL,
        user_id integer NOT NULL,
        external_id character varying NOT NULL,
        last_payload text NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        external_username character varying,
        external_email character varying,
        external_name character varying,
        external_avatar_url character varying(1000),
        external_profile_background_url character varying,
        external_card_background_url character varying
    );
    
    
    --
    -- Name: single_sign_on_records_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.single_sign_on_records_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: single_sign_on_records_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.single_sign_on_records_id_seq OWNED BY public.single_sign_on_records.id;
    
    
    --
    -- Name: site_settings; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.site_settings (
        id integer NOT NULL,
        name character varying NOT NULL,
        data_type integer NOT NULL,
        value text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: site_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.site_settings_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: site_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.site_settings_id_seq OWNED BY public.site_settings.id;
    
    
    --
    -- Name: skipped_email_logs; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.skipped_email_logs (
        id bigint NOT NULL,
        email_type character varying NOT NULL,
        to_address character varying NOT NULL,
        user_id integer,
        post_id integer,
        reason_type integer NOT NULL,
        custom_reason text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: skipped_email_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.skipped_email_logs_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: skipped_email_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.skipped_email_logs_id_seq OWNED BY public.skipped_email_logs.id;
    
    
    --
    -- Name: stylesheet_cache; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.stylesheet_cache (
        id integer NOT NULL,
        target character varying NOT NULL,
        digest character varying NOT NULL,
        content text NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        theme_id integer DEFAULT '-1'::integer NOT NULL,
        source_map text
    );
    
    
    --
    -- Name: stylesheet_cache_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.stylesheet_cache_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: stylesheet_cache_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.stylesheet_cache_id_seq OWNED BY public.stylesheet_cache.id;
    
    
    --
    -- Name: tag_group_memberships; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.tag_group_memberships (
        id integer NOT NULL,
        tag_id integer NOT NULL,
        tag_group_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: tag_group_memberships_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.tag_group_memberships_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: tag_group_memberships_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.tag_group_memberships_id_seq OWNED BY public.tag_group_memberships.id;
    
    
    --
    -- Name: tag_group_permissions; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.tag_group_permissions (
        id bigint NOT NULL,
        tag_group_id bigint NOT NULL,
        group_id bigint NOT NULL,
        permission_type integer DEFAULT 1 NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: tag_group_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.tag_group_permissions_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: tag_group_permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.tag_group_permissions_id_seq OWNED BY public.tag_group_permissions.id;
    
    
    --
    -- Name: tag_groups; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.tag_groups (
        id integer NOT NULL,
        name character varying NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        parent_tag_id integer,
        one_per_topic boolean DEFAULT false
    );
    
    
    --
    -- Name: tag_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.tag_groups_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: tag_groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.tag_groups_id_seq OWNED BY public.tag_groups.id;
    
    
    --
    -- Name: tag_search_data; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.tag_search_data (
        tag_id integer NOT NULL,
        search_data tsvector,
        raw_data text,
        locale text,
        version integer DEFAULT 0
    );
    
    
    --
    -- Name: tag_search_data_tag_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.tag_search_data_tag_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: tag_search_data_tag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.tag_search_data_tag_id_seq OWNED BY public.tag_search_data.tag_id;
    
    
    --
    -- Name: tag_users; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.tag_users (
        id integer NOT NULL,
        tag_id integer NOT NULL,
        user_id integer NOT NULL,
        notification_level integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: tag_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.tag_users_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: tag_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.tag_users_id_seq OWNED BY public.tag_users.id;
    
    
    --
    -- Name: tags; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.tags (
        id integer NOT NULL,
        name character varying NOT NULL,
        topic_count integer DEFAULT 0 NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        pm_topic_count integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: tags_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.tags_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: tags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.tags_id_seq OWNED BY public.tags.id;
    
    
    --
    -- Name: theme_fields; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.theme_fields (
        id integer NOT NULL,
        theme_id integer NOT NULL,
        target_id integer NOT NULL,
        name character varying(30) NOT NULL,
        value text NOT NULL,
        value_baked text,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        compiler_version integer DEFAULT 0 NOT NULL,
        error character varying,
        upload_id integer,
        type_id integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: theme_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.theme_fields_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: theme_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.theme_fields_id_seq OWNED BY public.theme_fields.id;
    
    
    --
    -- Name: theme_settings; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.theme_settings (
        id bigint NOT NULL,
        name character varying(255) NOT NULL,
        data_type integer NOT NULL,
        value text,
        theme_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: theme_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.theme_settings_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: theme_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.theme_settings_id_seq OWNED BY public.theme_settings.id;
    
    
    --
    -- Name: themes; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.themes (
        id integer NOT NULL,
        name character varying NOT NULL,
        user_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        compiler_version integer DEFAULT 0 NOT NULL,
        user_selectable boolean DEFAULT false NOT NULL,
        hidden boolean DEFAULT false NOT NULL,
        color_scheme_id integer,
        remote_theme_id integer,
        component boolean DEFAULT false NOT NULL
    );
    
    
    --
    -- Name: themes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.themes_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: themes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.themes_id_seq OWNED BY public.themes.id;
    
    
    --
    -- Name: top_topics; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.top_topics (
        id integer NOT NULL,
        topic_id integer,
        yearly_posts_count integer DEFAULT 0 NOT NULL,
        yearly_views_count integer DEFAULT 0 NOT NULL,
        yearly_likes_count integer DEFAULT 0 NOT NULL,
        monthly_posts_count integer DEFAULT 0 NOT NULL,
        monthly_views_count integer DEFAULT 0 NOT NULL,
        monthly_likes_count integer DEFAULT 0 NOT NULL,
        weekly_posts_count integer DEFAULT 0 NOT NULL,
        weekly_views_count integer DEFAULT 0 NOT NULL,
        weekly_likes_count integer DEFAULT 0 NOT NULL,
        daily_posts_count integer DEFAULT 0 NOT NULL,
        daily_views_count integer DEFAULT 0 NOT NULL,
        daily_likes_count integer DEFAULT 0 NOT NULL,
        daily_score double precision DEFAULT 0.0,
        weekly_score double precision DEFAULT 0.0,
        monthly_score double precision DEFAULT 0.0,
        yearly_score double precision DEFAULT 0.0,
        all_score double precision DEFAULT 0.0,
        daily_op_likes_count integer DEFAULT 0 NOT NULL,
        weekly_op_likes_count integer DEFAULT 0 NOT NULL,
        monthly_op_likes_count integer DEFAULT 0 NOT NULL,
        yearly_op_likes_count integer DEFAULT 0 NOT NULL,
        quarterly_posts_count integer DEFAULT 0 NOT NULL,
        quarterly_views_count integer DEFAULT 0 NOT NULL,
        quarterly_likes_count integer DEFAULT 0 NOT NULL,
        quarterly_score double precision DEFAULT 0.0,
        quarterly_op_likes_count integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: top_topics_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.top_topics_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: top_topics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.top_topics_id_seq OWNED BY public.top_topics.id;
    
    
    --
    -- Name: topic_allowed_groups; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_allowed_groups (
        id integer NOT NULL,
        group_id integer NOT NULL,
        topic_id integer NOT NULL
    );
    
    
    --
    -- Name: topic_allowed_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_allowed_groups_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_allowed_groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_allowed_groups_id_seq OWNED BY public.topic_allowed_groups.id;
    
    
    --
    -- Name: topic_allowed_users; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_allowed_users (
        id integer NOT NULL,
        user_id integer NOT NULL,
        topic_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: topic_allowed_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_allowed_users_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_allowed_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_allowed_users_id_seq OWNED BY public.topic_allowed_users.id;
    
    
    --
    -- Name: topic_custom_fields; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_custom_fields (
        id integer NOT NULL,
        topic_id integer NOT NULL,
        name character varying(256) NOT NULL,
        value text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: topic_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_custom_fields_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_custom_fields_id_seq OWNED BY public.topic_custom_fields.id;
    
    
    --
    -- Name: topic_embeds; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_embeds (
        id integer NOT NULL,
        topic_id integer NOT NULL,
        post_id integer NOT NULL,
        embed_url character varying(1000) NOT NULL,
        content_sha1 character varying(40),
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        deleted_at timestamp without time zone,
        deleted_by_id integer
    );
    
    
    --
    -- Name: topic_embeds_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_embeds_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_embeds_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_embeds_id_seq OWNED BY public.topic_embeds.id;
    
    
    --
    -- Name: topic_invites; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_invites (
        id integer NOT NULL,
        topic_id integer NOT NULL,
        invite_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: topic_invites_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_invites_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_invites_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_invites_id_seq OWNED BY public.topic_invites.id;
    
    
    --
    -- Name: topic_link_clicks; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_link_clicks (
        id integer NOT NULL,
        topic_link_id integer NOT NULL,
        user_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        ip_address inet
    );
    
    
    --
    -- Name: topic_link_clicks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_link_clicks_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_link_clicks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_link_clicks_id_seq OWNED BY public.topic_link_clicks.id;
    
    
    --
    -- Name: topic_links; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_links (
        id integer NOT NULL,
        topic_id integer NOT NULL,
        post_id integer,
        user_id integer NOT NULL,
        url character varying(500) NOT NULL,
        domain character varying(100) NOT NULL,
        internal boolean DEFAULT false NOT NULL,
        link_topic_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        reflection boolean DEFAULT false,
        clicks integer DEFAULT 0 NOT NULL,
        link_post_id integer,
        title character varying,
        crawled_at timestamp without time zone,
        quote boolean DEFAULT false NOT NULL,
        extension character varying(10)
    );
    
    
    --
    -- Name: topic_links_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_links_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_links_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_links_id_seq OWNED BY public.topic_links.id;
    
    
    --
    -- Name: topic_search_data; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_search_data (
        topic_id integer NOT NULL,
        raw_data text,
        locale character varying NOT NULL,
        search_data tsvector,
        version integer DEFAULT 0
    );
    
    
    --
    -- Name: topic_tags; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_tags (
        id integer NOT NULL,
        topic_id integer NOT NULL,
        tag_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: topic_tags_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_tags_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_tags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_tags_id_seq OWNED BY public.topic_tags.id;
    
    
    --
    -- Name: topic_timers; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_timers (
        id integer NOT NULL,
        execute_at timestamp without time zone NOT NULL,
        status_type integer NOT NULL,
        user_id integer NOT NULL,
        topic_id integer NOT NULL,
        based_on_last_post boolean DEFAULT false NOT NULL,
        deleted_at timestamp without time zone,
        deleted_by_id integer,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        category_id integer,
        public_type boolean DEFAULT true
    );
    
    
    --
    -- Name: topic_timers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_timers_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_timers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_timers_id_seq OWNED BY public.topic_timers.id;
    
    
    --
    -- Name: topic_users; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_users (
        user_id integer NOT NULL,
        topic_id integer NOT NULL,
        posted boolean DEFAULT false NOT NULL,
        last_read_post_number integer,
        highest_seen_post_number integer,
        last_visited_at timestamp without time zone,
        first_visited_at timestamp without time zone,
        notification_level integer DEFAULT 1 NOT NULL,
        notifications_changed_at timestamp without time zone,
        notifications_reason_id integer,
        total_msecs_viewed integer DEFAULT 0 NOT NULL,
        cleared_pinned_at timestamp without time zone,
        id integer NOT NULL,
        last_emailed_post_number integer,
        liked boolean DEFAULT false,
        bookmarked boolean DEFAULT false
    );
    
    
    --
    -- Name: topic_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topic_users_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topic_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topic_users_id_seq OWNED BY public.topic_users.id;
    
    
    --
    -- Name: topic_views; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.topic_views (
        topic_id integer NOT NULL,
        viewed_at date NOT NULL,
        user_id integer,
        ip_address inet
    );
    
    
    --
    -- Name: topics_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.topics_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: topics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.topics_id_seq OWNED BY public.topics.id;
    
    
    --
    -- Name: translation_overrides; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.translation_overrides (
        id integer NOT NULL,
        locale character varying NOT NULL,
        translation_key character varying NOT NULL,
        value character varying NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        compiled_js text
    );
    
    
    --
    -- Name: translation_overrides_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.translation_overrides_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: translation_overrides_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.translation_overrides_id_seq OWNED BY public.translation_overrides.id;
    
    
    --
    -- Name: twitter_user_infos; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.twitter_user_infos (
        id integer NOT NULL,
        user_id integer NOT NULL,
        screen_name character varying NOT NULL,
        twitter_user_id bigint NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        email character varying(1000)
    );
    
    
    --
    -- Name: twitter_user_infos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.twitter_user_infos_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: twitter_user_infos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.twitter_user_infos_id_seq OWNED BY public.twitter_user_infos.id;
    
    
    --
    -- Name: unsubscribe_keys; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.unsubscribe_keys (
        key character varying(64) NOT NULL,
        user_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        unsubscribe_key_type character varying,
        topic_id integer,
        post_id integer
    );
    
    
    --
    -- Name: uploads; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.uploads (
        id integer NOT NULL,
        user_id integer NOT NULL,
        original_filename character varying NOT NULL,
        filesize integer NOT NULL,
        width integer,
        height integer,
        url character varying NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        sha1 character varying(40),
        origin character varying(1000),
        retain_hours integer,
        extension character varying(10),
        thumbnail_width integer,
        thumbnail_height integer
    );
    
    
    --
    -- Name: uploads_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.uploads_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: uploads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.uploads_id_seq OWNED BY public.uploads.id;
    
    
    --
    -- Name: user_actions; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_actions (
        id integer NOT NULL,
        action_type integer NOT NULL,
        user_id integer NOT NULL,
        target_topic_id integer,
        target_post_id integer,
        target_user_id integer,
        acting_user_id integer,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        queued_post_id integer
    );
    
    
    --
    -- Name: user_actions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_actions_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_actions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_actions_id_seq OWNED BY public.user_actions.id;
    
    
    --
    -- Name: user_api_keys; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_api_keys (
        id integer NOT NULL,
        user_id integer NOT NULL,
        client_id character varying NOT NULL,
        key character varying NOT NULL,
        application_name character varying NOT NULL,
        push_url character varying,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        revoked_at timestamp without time zone,
        scopes text[] DEFAULT '{}'::text[] NOT NULL,
        last_used_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
    );
    
    
    --
    -- Name: user_api_keys_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_api_keys_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_api_keys_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_api_keys_id_seq OWNED BY public.user_api_keys.id;
    
    
    --
    -- Name: user_archived_messages; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_archived_messages (
        id integer NOT NULL,
        user_id integer NOT NULL,
        topic_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: user_archived_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_archived_messages_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_archived_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_archived_messages_id_seq OWNED BY public.user_archived_messages.id;
    
    
    --
    -- Name: user_auth_token_logs; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_auth_token_logs (
        id integer NOT NULL,
        action character varying NOT NULL,
        user_auth_token_id integer,
        user_id integer,
        client_ip inet,
        user_agent character varying,
        auth_token character varying,
        created_at timestamp without time zone,
        path character varying
    );
    
    
    --
    -- Name: user_auth_token_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_auth_token_logs_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_auth_token_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_auth_token_logs_id_seq OWNED BY public.user_auth_token_logs.id;
    
    
    --
    -- Name: user_auth_tokens; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_auth_tokens (
        id integer NOT NULL,
        user_id integer NOT NULL,
        auth_token character varying NOT NULL,
        prev_auth_token character varying NOT NULL,
        user_agent character varying,
        auth_token_seen boolean DEFAULT false NOT NULL,
        client_ip inet,
        rotated_at timestamp without time zone NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        seen_at timestamp without time zone
    );
    
    
    --
    -- Name: user_auth_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_auth_tokens_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_auth_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_auth_tokens_id_seq OWNED BY public.user_auth_tokens.id;
    
    
    --
    -- Name: user_avatars; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_avatars (
        id integer NOT NULL,
        user_id integer NOT NULL,
        custom_upload_id integer,
        gravatar_upload_id integer,
        last_gravatar_download_attempt timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: user_avatars_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_avatars_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_avatars_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_avatars_id_seq OWNED BY public.user_avatars.id;
    
    
    --
    -- Name: user_badges; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_badges (
        id integer NOT NULL,
        badge_id integer NOT NULL,
        user_id integer NOT NULL,
        granted_at timestamp without time zone NOT NULL,
        granted_by_id integer NOT NULL,
        post_id integer,
        notification_id integer,
        seq integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: user_badges_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_badges_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_badges_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_badges_id_seq OWNED BY public.user_badges.id;
    
    
    --
    -- Name: user_custom_fields; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_custom_fields (
        id integer NOT NULL,
        user_id integer NOT NULL,
        name character varying(256) NOT NULL,
        value text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: user_custom_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_custom_fields_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_custom_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_custom_fields_id_seq OWNED BY public.user_custom_fields.id;
    
    
    --
    -- Name: user_emails; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_emails (
        id integer NOT NULL,
        user_id integer NOT NULL,
        email character varying(513) NOT NULL,
        "primary" boolean DEFAULT false NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: user_emails_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_emails_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_emails_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_emails_id_seq OWNED BY public.user_emails.id;
    
    
    --
    -- Name: user_exports; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_exports (
        id integer NOT NULL,
        file_name character varying NOT NULL,
        user_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        upload_id integer
    );
    
    
    --
    -- Name: user_exports_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_exports_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_exports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_exports_id_seq OWNED BY public.user_exports.id;
    
    
    --
    -- Name: user_field_options; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_field_options (
        id integer NOT NULL,
        user_field_id integer NOT NULL,
        value character varying NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: user_field_options_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_field_options_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_field_options_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_field_options_id_seq OWNED BY public.user_field_options.id;
    
    
    --
    -- Name: user_fields; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_fields (
        id integer NOT NULL,
        name character varying NOT NULL,
        field_type character varying NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone,
        editable boolean DEFAULT false NOT NULL,
        description character varying NOT NULL,
        required boolean DEFAULT true NOT NULL,
        show_on_profile boolean DEFAULT false NOT NULL,
        "position" integer DEFAULT 0,
        show_on_user_card boolean DEFAULT false NOT NULL,
        external_name character varying,
        external_type character varying
    );
    
    
    --
    -- Name: user_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_fields_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_fields_id_seq OWNED BY public.user_fields.id;
    
    
    --
    -- Name: user_histories; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_histories (
        id integer NOT NULL,
        action integer NOT NULL,
        acting_user_id integer,
        target_user_id integer,
        details text,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        context character varying,
        ip_address character varying,
        email character varying,
        subject text,
        previous_value text,
        new_value text,
        topic_id integer,
        admin_only boolean DEFAULT false,
        post_id integer,
        custom_type character varying,
        category_id integer
    );
    
    
    --
    -- Name: user_histories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_histories_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_histories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_histories_id_seq OWNED BY public.user_histories.id;
    
    
    --
    -- Name: user_open_ids; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_open_ids (
        id integer NOT NULL,
        user_id integer NOT NULL,
        email character varying NOT NULL,
        url character varying NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        active boolean NOT NULL
    );
    
    
    --
    -- Name: user_open_ids_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_open_ids_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_open_ids_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_open_ids_id_seq OWNED BY public.user_open_ids.id;
    
    
    --
    -- Name: user_options; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_options (
        user_id integer NOT NULL,
        email_always boolean DEFAULT false NOT NULL,
        mailing_list_mode boolean DEFAULT false NOT NULL,
        email_digests boolean,
        email_direct boolean DEFAULT true NOT NULL,
        email_private_messages boolean DEFAULT true NOT NULL,
        external_links_in_new_tab boolean DEFAULT false NOT NULL,
        enable_quoting boolean DEFAULT true NOT NULL,
        dynamic_favicon boolean DEFAULT false NOT NULL,
        disable_jump_reply boolean DEFAULT false NOT NULL,
        automatically_unpin_topics boolean DEFAULT true NOT NULL,
        digest_after_minutes integer,
        auto_track_topics_after_msecs integer,
        new_topic_duration_minutes integer,
        last_redirected_to_top_at timestamp without time zone,
        email_previous_replies integer DEFAULT 2 NOT NULL,
        email_in_reply_to boolean DEFAULT true NOT NULL,
        like_notification_frequency integer DEFAULT 1 NOT NULL,
        mailing_list_mode_frequency integer DEFAULT 1 NOT NULL,
        include_tl0_in_digests boolean DEFAULT false,
        notification_level_when_replying integer,
        theme_key_seq integer DEFAULT 0 NOT NULL,
        allow_private_messages boolean DEFAULT true NOT NULL,
        homepage_id integer,
        theme_ids integer[] DEFAULT '{}'::integer[] NOT NULL,
        hide_profile_and_presence boolean DEFAULT false NOT NULL
    );
    
    
    --
    -- Name: user_profile_views; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_profile_views (
        id integer NOT NULL,
        user_profile_id integer NOT NULL,
        viewed_at timestamp without time zone NOT NULL,
        ip_address inet,
        user_id integer
    );
    
    
    --
    -- Name: user_profile_views_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_profile_views_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_profile_views_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_profile_views_id_seq OWNED BY public.user_profile_views.id;
    
    
    --
    -- Name: user_profiles; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_profiles (
        user_id integer NOT NULL,
        location character varying,
        website character varying,
        bio_raw text,
        bio_cooked text,
        profile_background character varying(255),
        dismissed_banner_key integer,
        bio_cooked_version integer,
        badge_granted_title boolean DEFAULT false,
        card_background character varying(255),
        views integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: user_search_data; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_search_data (
        user_id integer NOT NULL,
        search_data tsvector,
        raw_data text,
        locale text,
        version integer DEFAULT 0
    );
    
    
    --
    -- Name: user_second_factors; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_second_factors (
        id bigint NOT NULL,
        user_id integer NOT NULL,
        method integer NOT NULL,
        data character varying NOT NULL,
        enabled boolean DEFAULT false NOT NULL,
        last_used timestamp without time zone,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: user_second_factors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_second_factors_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_second_factors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_second_factors_id_seq OWNED BY public.user_second_factors.id;
    
    
    --
    -- Name: user_stats; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_stats (
        user_id integer NOT NULL,
        topics_entered integer DEFAULT 0 NOT NULL,
        time_read integer DEFAULT 0 NOT NULL,
        days_visited integer DEFAULT 0 NOT NULL,
        posts_read_count integer DEFAULT 0 NOT NULL,
        likes_given integer DEFAULT 0 NOT NULL,
        likes_received integer DEFAULT 0 NOT NULL,
        topic_reply_count integer DEFAULT 0 NOT NULL,
        new_since timestamp without time zone NOT NULL,
        read_faq timestamp without time zone,
        first_post_created_at timestamp without time zone,
        post_count integer DEFAULT 0 NOT NULL,
        topic_count integer DEFAULT 0 NOT NULL,
        bounce_score double precision DEFAULT 0 NOT NULL,
        reset_bounce_score_after timestamp without time zone
    );
    
    
    --
    -- Name: user_uploads; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_uploads (
        id bigint NOT NULL,
        upload_id integer NOT NULL,
        user_id integer NOT NULL,
        created_at timestamp without time zone NOT NULL
    );
    
    
    --
    -- Name: user_uploads_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_uploads_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_uploads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_uploads_id_seq OWNED BY public.user_uploads.id;
    
    
    --
    -- Name: user_visits; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_visits (
        id integer NOT NULL,
        user_id integer NOT NULL,
        visited_at date NOT NULL,
        posts_read integer DEFAULT 0,
        mobile boolean DEFAULT false,
        time_read integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: user_visits_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_visits_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_visits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_visits_id_seq OWNED BY public.user_visits.id;
    
    
    --
    -- Name: user_warnings; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.user_warnings (
        id integer NOT NULL,
        topic_id integer NOT NULL,
        user_id integer NOT NULL,
        created_by_id integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: user_warnings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.user_warnings_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: user_warnings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.user_warnings_id_seq OWNED BY public.user_warnings.id;
    
    
    --
    -- Name: users; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.users (
        id integer NOT NULL,
        username character varying(60) NOT NULL,
        created_at timestamp without time zone NOT NULL,
        updated_at timestamp without time zone NOT NULL,
        name character varying,
        seen_notification_id integer DEFAULT 0 NOT NULL,
        last_posted_at timestamp without time zone,
        password_hash character varying(64),
        salt character varying(32),
        active boolean DEFAULT false NOT NULL,
        username_lower character varying(60) NOT NULL,
        last_seen_at timestamp without time zone,
        admin boolean DEFAULT false NOT NULL,
        last_emailed_at timestamp without time zone,
        trust_level integer NOT NULL,
        approved boolean DEFAULT false NOT NULL,
        approved_by_id integer,
        approved_at timestamp without time zone,
        previous_visit_at timestamp without time zone,
        suspended_at timestamp without time zone,
        suspended_till timestamp without time zone,
        date_of_birth date,
        views integer DEFAULT 0 NOT NULL,
        flag_level integer DEFAULT 0 NOT NULL,
        ip_address inet,
        moderator boolean DEFAULT false,
        title character varying,
        uploaded_avatar_id integer,
        locale character varying(10),
        primary_group_id integer,
        registration_ip_address inet,
        staged boolean DEFAULT false NOT NULL,
        first_seen_at timestamp without time zone,
        silenced_till timestamp without time zone,
        group_locked_trust_level integer,
        manual_locked_trust_level integer
    );
    
    
    --
    -- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.users_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;
    
    
    --
    -- Name: watched_words; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.watched_words (
        id integer NOT NULL,
        word character varying NOT NULL,
        action integer NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: watched_words_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.watched_words_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: watched_words_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.watched_words_id_seq OWNED BY public.watched_words.id;
    
    
    --
    -- Name: web_crawler_requests; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.web_crawler_requests (
        id bigint NOT NULL,
        date date NOT NULL,
        user_agent character varying NOT NULL,
        count integer DEFAULT 0 NOT NULL
    );
    
    
    --
    -- Name: web_crawler_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.web_crawler_requests_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: web_crawler_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.web_crawler_requests_id_seq OWNED BY public.web_crawler_requests.id;
    
    
    --
    -- Name: web_hook_event_types; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.web_hook_event_types (
        id integer NOT NULL,
        name character varying NOT NULL
    );
    
    
    --
    -- Name: web_hook_event_types_hooks; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.web_hook_event_types_hooks (
        web_hook_id integer NOT NULL,
        web_hook_event_type_id integer NOT NULL
    );
    
    
    --
    -- Name: web_hook_event_types_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.web_hook_event_types_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: web_hook_event_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.web_hook_event_types_id_seq OWNED BY public.web_hook_event_types.id;
    
    
    --
    -- Name: web_hook_events; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.web_hook_events (
        id integer NOT NULL,
        web_hook_id integer NOT NULL,
        headers character varying,
        payload text,
        status integer DEFAULT 0,
        response_headers character varying,
        response_body text,
        duration integer DEFAULT 0,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: web_hook_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.web_hook_events_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: web_hook_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.web_hook_events_id_seq OWNED BY public.web_hook_events.id;
    
    
    --
    -- Name: web_hooks; Type: TABLE; Schema: public; Owner: -
    --
    
    CREATE TABLE public.web_hooks (
        id integer NOT NULL,
        payload_url character varying NOT NULL,
        content_type integer DEFAULT 1 NOT NULL,
        last_delivery_status integer DEFAULT 1 NOT NULL,
        status integer DEFAULT 1 NOT NULL,
        secret character varying DEFAULT ''::character varying,
        wildcard_web_hook boolean DEFAULT false NOT NULL,
        verify_certificate boolean DEFAULT true NOT NULL,
        active boolean DEFAULT false NOT NULL,
        created_at timestamp without time zone,
        updated_at timestamp without time zone
    );
    
    
    --
    -- Name: web_hooks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
    --
    
    CREATE SEQUENCE public.web_hooks_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    
    
    --
    -- Name: web_hooks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
    --
    
    ALTER SEQUENCE public.web_hooks_id_seq OWNED BY public.web_hooks.id;
    
    
    --
    -- Name: api_keys id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.api_keys ALTER COLUMN id SET DEFAULT nextval('public.api_keys_id_seq'::regclass);
    
    
    --
    -- Name: application_requests id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.application_requests ALTER COLUMN id SET DEFAULT nextval('public.application_requests_id_seq'::regclass);
    
    
    --
    -- Name: badge_groupings id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.badge_groupings ALTER COLUMN id SET DEFAULT nextval('public.badge_groupings_id_seq'::regclass);
    
    
    --
    -- Name: badge_types id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.badge_types ALTER COLUMN id SET DEFAULT nextval('public.badge_types_id_seq'::regclass);
    
    
    --
    -- Name: badges id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.badges ALTER COLUMN id SET DEFAULT nextval('public.badges_id_seq'::regclass);
    
    
    --
    -- Name: categories id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);
    
    
    --
    -- Name: category_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.category_custom_fields_id_seq'::regclass);
    
    
    --
    -- Name: category_featured_topics id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_featured_topics ALTER COLUMN id SET DEFAULT nextval('public.category_featured_topics_id_seq'::regclass);
    
    
    --
    -- Name: category_groups id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_groups ALTER COLUMN id SET DEFAULT nextval('public.category_groups_id_seq'::regclass);
    
    
    --
    -- Name: category_tag_groups id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_tag_groups ALTER COLUMN id SET DEFAULT nextval('public.category_tag_groups_id_seq'::regclass);
    
    
    --
    -- Name: category_tag_stats id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_tag_stats ALTER COLUMN id SET DEFAULT nextval('public.category_tag_stats_id_seq'::regclass);
    
    
    --
    -- Name: category_tags id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_tags ALTER COLUMN id SET DEFAULT nextval('public.category_tags_id_seq'::regclass);
    
    
    --
    -- Name: category_users id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_users ALTER COLUMN id SET DEFAULT nextval('public.category_users_id_seq'::regclass);
    
    
    --
    -- Name: child_themes id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.child_themes ALTER COLUMN id SET DEFAULT nextval('public.child_themes_id_seq'::regclass);
    
    
    --
    -- Name: color_scheme_colors id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.color_scheme_colors ALTER COLUMN id SET DEFAULT nextval('public.color_scheme_colors_id_seq'::regclass);
    
    
    --
    -- Name: color_schemes id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.color_schemes ALTER COLUMN id SET DEFAULT nextval('public.color_schemes_id_seq'::regclass);
    
    
    --
    -- Name: custom_emojis id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.custom_emojis ALTER COLUMN id SET DEFAULT nextval('public.custom_emojis_id_seq'::regclass);
    
    
    --
    -- Name: developers id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.developers ALTER COLUMN id SET DEFAULT nextval('public.developers_id_seq'::regclass);
    
    
    --
    -- Name: directory_items id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.directory_items ALTER COLUMN id SET DEFAULT nextval('public.directory_items_id_seq'::regclass);
    
    
    --
    -- Name: draft_sequences id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.draft_sequences ALTER COLUMN id SET DEFAULT nextval('public.draft_sequences_id_seq'::regclass);
    
    
    --
    -- Name: drafts id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.drafts ALTER COLUMN id SET DEFAULT nextval('public.drafts_id_seq'::regclass);
    
    
    --
    -- Name: email_change_requests id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.email_change_requests ALTER COLUMN id SET DEFAULT nextval('public.email_change_requests_id_seq'::regclass);
    
    
    --
    -- Name: email_logs id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.email_logs ALTER COLUMN id SET DEFAULT nextval('public.email_logs_id_seq'::regclass);
    
    
    --
    -- Name: email_tokens id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.email_tokens ALTER COLUMN id SET DEFAULT nextval('public.email_tokens_id_seq'::regclass);
    
    
    --
    -- Name: embeddable_hosts id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.embeddable_hosts ALTER COLUMN id SET DEFAULT nextval('public.embeddable_hosts_id_seq'::regclass);
    
    
    --
    -- Name: facebook_user_infos id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.facebook_user_infos ALTER COLUMN id SET DEFAULT nextval('public.facebook_user_infos_id_seq'::regclass);
    
    
    --
    -- Name: github_user_infos id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.github_user_infos ALTER COLUMN id SET DEFAULT nextval('public.github_user_infos_id_seq'::regclass);
    
    
    --
    -- Name: google_user_infos id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.google_user_infos ALTER COLUMN id SET DEFAULT nextval('public.google_user_infos_id_seq'::regclass);
    
    
    --
    -- Name: group_archived_messages id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_archived_messages ALTER COLUMN id SET DEFAULT nextval('public.group_archived_messages_id_seq'::regclass);
    
    
    --
    -- Name: group_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.group_custom_fields_id_seq'::regclass);
    
    
    --
    -- Name: group_histories id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_histories ALTER COLUMN id SET DEFAULT nextval('public.group_histories_id_seq'::regclass);
    
    
    --
    -- Name: group_mentions id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_mentions ALTER COLUMN id SET DEFAULT nextval('public.group_mentions_id_seq'::regclass);
    
    
    --
    -- Name: group_users id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_users ALTER COLUMN id SET DEFAULT nextval('public.group_users_id_seq'::regclass);
    
    
    --
    -- Name: groups id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.groups ALTER COLUMN id SET DEFAULT nextval('public.groups_id_seq'::regclass);
    
    
    --
    -- Name: incoming_domains id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_domains ALTER COLUMN id SET DEFAULT nextval('public.incoming_domains_id_seq'::regclass);
    
    
    --
    -- Name: incoming_emails id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_emails ALTER COLUMN id SET DEFAULT nextval('public.incoming_emails_id_seq'::regclass);
    
    
    --
    -- Name: incoming_links id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_links ALTER COLUMN id SET DEFAULT nextval('public.incoming_links_id_seq'::regclass);
    
    
    --
    -- Name: incoming_referers id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_referers ALTER COLUMN id SET DEFAULT nextval('public.incoming_referers_id_seq'::regclass);
    
    
    --
    -- Name: instagram_user_infos id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.instagram_user_infos ALTER COLUMN id SET DEFAULT nextval('public.instagram_user_infos_id_seq'::regclass);
    
    
    --
    -- Name: invited_groups id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.invited_groups ALTER COLUMN id SET DEFAULT nextval('public.invited_groups_id_seq'::regclass);
    
    
    --
    -- Name: invites id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.invites ALTER COLUMN id SET DEFAULT nextval('public.invites_id_seq'::regclass);
    
    
    --
    -- Name: javascript_caches id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.javascript_caches ALTER COLUMN id SET DEFAULT nextval('public.javascript_caches_id_seq'::regclass);
    
    
    --
    -- Name: message_bus id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.message_bus ALTER COLUMN id SET DEFAULT nextval('public.message_bus_id_seq'::regclass);
    
    
    --
    -- Name: muted_users id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.muted_users ALTER COLUMN id SET DEFAULT nextval('public.muted_users_id_seq'::regclass);
    
    
    --
    -- Name: notifications id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);
    
    
    --
    -- Name: oauth2_user_infos id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.oauth2_user_infos ALTER COLUMN id SET DEFAULT nextval('public.oauth2_user_infos_id_seq'::regclass);
    
    
    --
    -- Name: onceoff_logs id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.onceoff_logs ALTER COLUMN id SET DEFAULT nextval('public.onceoff_logs_id_seq'::regclass);
    
    
    --
    -- Name: optimized_images id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.optimized_images ALTER COLUMN id SET DEFAULT nextval('public.optimized_images_id_seq'::regclass);
    
    
    --
    -- Name: permalinks id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.permalinks ALTER COLUMN id SET DEFAULT nextval('public.permalinks_id_seq'::regclass);
    
    
    --
    -- Name: plugin_store_rows id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.plugin_store_rows ALTER COLUMN id SET DEFAULT nextval('public.plugin_store_rows_id_seq'::regclass);
    
    
    --
    -- Name: post_action_types id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_action_types ALTER COLUMN id SET DEFAULT nextval('public.post_action_types_id_seq'::regclass);
    
    
    --
    -- Name: post_actions id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_actions ALTER COLUMN id SET DEFAULT nextval('public.post_actions_id_seq'::regclass);
    
    
    --
    -- Name: post_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.post_custom_fields_id_seq'::regclass);
    
    
    --
    -- Name: post_details id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_details ALTER COLUMN id SET DEFAULT nextval('public.post_details_id_seq'::regclass);
    
    
    --
    -- Name: post_reply_keys id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_reply_keys ALTER COLUMN id SET DEFAULT nextval('public.post_reply_keys_id_seq'::regclass);
    
    
    --
    -- Name: post_revisions id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_revisions ALTER COLUMN id SET DEFAULT nextval('public.post_revisions_id_seq'::regclass);
    
    
    --
    -- Name: post_stats id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_stats ALTER COLUMN id SET DEFAULT nextval('public.post_stats_id_seq'::regclass);
    
    
    --
    -- Name: post_uploads id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_uploads ALTER COLUMN id SET DEFAULT nextval('public.post_uploads_id_seq'::regclass);
    
    
    --
    -- Name: posts id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.posts ALTER COLUMN id SET DEFAULT nextval('public.posts_id_seq'::regclass);
    
    
    --
    -- Name: push_subscriptions id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.push_subscriptions ALTER COLUMN id SET DEFAULT nextval('public.push_subscriptions_id_seq'::regclass);
    
    
    --
    -- Name: queued_posts id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.queued_posts ALTER COLUMN id SET DEFAULT nextval('public.queued_posts_id_seq'::regclass);
    
    
    --
    -- Name: quoted_posts id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.quoted_posts ALTER COLUMN id SET DEFAULT nextval('public.quoted_posts_id_seq'::regclass);
    
    
    --
    -- Name: remote_themes id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.remote_themes ALTER COLUMN id SET DEFAULT nextval('public.remote_themes_id_seq'::regclass);
    
    
    --
    -- Name: scheduler_stats id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.scheduler_stats ALTER COLUMN id SET DEFAULT nextval('public.scheduler_stats_id_seq'::regclass);
    
    
    --
    -- Name: schema_migration_details id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.schema_migration_details ALTER COLUMN id SET DEFAULT nextval('public.schema_migration_details_id_seq'::regclass);
    
    
    --
    -- Name: screened_emails id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.screened_emails ALTER COLUMN id SET DEFAULT nextval('public.screened_emails_id_seq'::regclass);
    
    
    --
    -- Name: screened_ip_addresses id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.screened_ip_addresses ALTER COLUMN id SET DEFAULT nextval('public.screened_ip_addresses_id_seq'::regclass);
    
    
    --
    -- Name: screened_urls id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.screened_urls ALTER COLUMN id SET DEFAULT nextval('public.screened_urls_id_seq'::regclass);
    
    
    --
    -- Name: search_logs id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.search_logs ALTER COLUMN id SET DEFAULT nextval('public.search_logs_id_seq'::regclass);
    
    
    --
    -- Name: shared_drafts id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.shared_drafts ALTER COLUMN id SET DEFAULT nextval('public.shared_drafts_id_seq'::regclass);
    
    
    --
    -- Name: single_sign_on_records id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.single_sign_on_records ALTER COLUMN id SET DEFAULT nextval('public.single_sign_on_records_id_seq'::regclass);
    
    
    --
    -- Name: site_settings id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.site_settings ALTER COLUMN id SET DEFAULT nextval('public.site_settings_id_seq'::regclass);
    
    
    --
    -- Name: skipped_email_logs id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.skipped_email_logs ALTER COLUMN id SET DEFAULT nextval('public.skipped_email_logs_id_seq'::regclass);
    
    
    --
    -- Name: stylesheet_cache id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.stylesheet_cache ALTER COLUMN id SET DEFAULT nextval('public.stylesheet_cache_id_seq'::regclass);
    
    
    --
    -- Name: tag_group_memberships id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_group_memberships ALTER COLUMN id SET DEFAULT nextval('public.tag_group_memberships_id_seq'::regclass);
    
    
    --
    -- Name: tag_group_permissions id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_group_permissions ALTER COLUMN id SET DEFAULT nextval('public.tag_group_permissions_id_seq'::regclass);
    
    
    --
    -- Name: tag_groups id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_groups ALTER COLUMN id SET DEFAULT nextval('public.tag_groups_id_seq'::regclass);
    
    
    --
    -- Name: tag_search_data tag_id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_search_data ALTER COLUMN tag_id SET DEFAULT nextval('public.tag_search_data_tag_id_seq'::regclass);
    
    
    --
    -- Name: tag_users id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_users ALTER COLUMN id SET DEFAULT nextval('public.tag_users_id_seq'::regclass);
    
    
    --
    -- Name: tags id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tags ALTER COLUMN id SET DEFAULT nextval('public.tags_id_seq'::regclass);
    
    
    --
    -- Name: theme_fields id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.theme_fields ALTER COLUMN id SET DEFAULT nextval('public.theme_fields_id_seq'::regclass);
    
    
    --
    -- Name: theme_settings id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.theme_settings ALTER COLUMN id SET DEFAULT nextval('public.theme_settings_id_seq'::regclass);
    
    
    --
    -- Name: themes id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.themes ALTER COLUMN id SET DEFAULT nextval('public.themes_id_seq'::regclass);
    
    
    --
    -- Name: top_topics id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.top_topics ALTER COLUMN id SET DEFAULT nextval('public.top_topics_id_seq'::regclass);
    
    
    --
    -- Name: topic_allowed_groups id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_allowed_groups ALTER COLUMN id SET DEFAULT nextval('public.topic_allowed_groups_id_seq'::regclass);
    
    
    --
    -- Name: topic_allowed_users id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_allowed_users ALTER COLUMN id SET DEFAULT nextval('public.topic_allowed_users_id_seq'::regclass);
    
    
    --
    -- Name: topic_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.topic_custom_fields_id_seq'::regclass);
    
    
    --
    -- Name: topic_embeds id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_embeds ALTER COLUMN id SET DEFAULT nextval('public.topic_embeds_id_seq'::regclass);
    
    
    --
    -- Name: topic_invites id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_invites ALTER COLUMN id SET DEFAULT nextval('public.topic_invites_id_seq'::regclass);
    
    
    --
    -- Name: topic_link_clicks id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_link_clicks ALTER COLUMN id SET DEFAULT nextval('public.topic_link_clicks_id_seq'::regclass);
    
    
    --
    -- Name: topic_links id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_links ALTER COLUMN id SET DEFAULT nextval('public.topic_links_id_seq'::regclass);
    
    
    --
    -- Name: topic_tags id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_tags ALTER COLUMN id SET DEFAULT nextval('public.topic_tags_id_seq'::regclass);
    
    
    --
    -- Name: topic_timers id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_timers ALTER COLUMN id SET DEFAULT nextval('public.topic_timers_id_seq'::regclass);
    
    
    --
    -- Name: topic_users id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_users ALTER COLUMN id SET DEFAULT nextval('public.topic_users_id_seq'::regclass);
    
    
    --
    -- Name: topics id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topics ALTER COLUMN id SET DEFAULT nextval('public.topics_id_seq'::regclass);
    
    
    --
    -- Name: translation_overrides id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.translation_overrides ALTER COLUMN id SET DEFAULT nextval('public.translation_overrides_id_seq'::regclass);
    
    
    --
    -- Name: twitter_user_infos id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.twitter_user_infos ALTER COLUMN id SET DEFAULT nextval('public.twitter_user_infos_id_seq'::regclass);
    
    
    --
    -- Name: uploads id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.uploads ALTER COLUMN id SET DEFAULT nextval('public.uploads_id_seq'::regclass);
    
    
    --
    -- Name: user_actions id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_actions ALTER COLUMN id SET DEFAULT nextval('public.user_actions_id_seq'::regclass);
    
    
    --
    -- Name: user_api_keys id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_api_keys ALTER COLUMN id SET DEFAULT nextval('public.user_api_keys_id_seq'::regclass);
    
    
    --
    -- Name: user_archived_messages id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_archived_messages ALTER COLUMN id SET DEFAULT nextval('public.user_archived_messages_id_seq'::regclass);
    
    
    --
    -- Name: user_auth_token_logs id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_auth_token_logs ALTER COLUMN id SET DEFAULT nextval('public.user_auth_token_logs_id_seq'::regclass);
    
    
    --
    -- Name: user_auth_tokens id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_auth_tokens ALTER COLUMN id SET DEFAULT nextval('public.user_auth_tokens_id_seq'::regclass);
    
    
    --
    -- Name: user_avatars id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_avatars ALTER COLUMN id SET DEFAULT nextval('public.user_avatars_id_seq'::regclass);
    
    
    --
    -- Name: user_badges id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_badges ALTER COLUMN id SET DEFAULT nextval('public.user_badges_id_seq'::regclass);
    
    
    --
    -- Name: user_custom_fields id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_custom_fields ALTER COLUMN id SET DEFAULT nextval('public.user_custom_fields_id_seq'::regclass);
    
    
    --
    -- Name: user_emails id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_emails ALTER COLUMN id SET DEFAULT nextval('public.user_emails_id_seq'::regclass);
    
    
    --
    -- Name: user_exports id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_exports ALTER COLUMN id SET DEFAULT nextval('public.user_exports_id_seq'::regclass);
    
    
    --
    -- Name: user_field_options id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_field_options ALTER COLUMN id SET DEFAULT nextval('public.user_field_options_id_seq'::regclass);
    
    
    --
    -- Name: user_fields id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_fields ALTER COLUMN id SET DEFAULT nextval('public.user_fields_id_seq'::regclass);
    
    
    --
    -- Name: user_histories id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_histories ALTER COLUMN id SET DEFAULT nextval('public.user_histories_id_seq'::regclass);
    
    
    --
    -- Name: user_open_ids id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_open_ids ALTER COLUMN id SET DEFAULT nextval('public.user_open_ids_id_seq'::regclass);
    
    
    --
    -- Name: user_profile_views id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_profile_views ALTER COLUMN id SET DEFAULT nextval('public.user_profile_views_id_seq'::regclass);
    
    
    --
    -- Name: user_second_factors id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_second_factors ALTER COLUMN id SET DEFAULT nextval('public.user_second_factors_id_seq'::regclass);
    
    
    --
    -- Name: user_uploads id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_uploads ALTER COLUMN id SET DEFAULT nextval('public.user_uploads_id_seq'::regclass);
    
    
    --
    -- Name: user_visits id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_visits ALTER COLUMN id SET DEFAULT nextval('public.user_visits_id_seq'::regclass);
    
    
    --
    -- Name: user_warnings id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_warnings ALTER COLUMN id SET DEFAULT nextval('public.user_warnings_id_seq'::regclass);
    
    
    --
    -- Name: users id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);
    
    
    --
    -- Name: watched_words id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.watched_words ALTER COLUMN id SET DEFAULT nextval('public.watched_words_id_seq'::regclass);
    
    
    --
    -- Name: web_crawler_requests id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_crawler_requests ALTER COLUMN id SET DEFAULT nextval('public.web_crawler_requests_id_seq'::regclass);
    
    
    --
    -- Name: web_hook_event_types id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_hook_event_types ALTER COLUMN id SET DEFAULT nextval('public.web_hook_event_types_id_seq'::regclass);
    
    
    --
    -- Name: web_hook_events id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_hook_events ALTER COLUMN id SET DEFAULT nextval('public.web_hook_events_id_seq'::regclass);
    
    
    --
    -- Name: web_hooks id; Type: DEFAULT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_hooks ALTER COLUMN id SET DEFAULT nextval('public.web_hooks_id_seq'::regclass);
    
    
    --
    -- Name: api_keys api_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.api_keys
        ADD CONSTRAINT api_keys_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: application_requests application_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.application_requests
        ADD CONSTRAINT application_requests_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: ar_internal_metadata ar_internal_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.ar_internal_metadata
        ADD CONSTRAINT ar_internal_metadata_pkey PRIMARY KEY (key);
    
    
    --
    -- Name: badge_groupings badge_groupings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.badge_groupings
        ADD CONSTRAINT badge_groupings_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: badge_types badge_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.badge_types
        ADD CONSTRAINT badge_types_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: badges badges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.badges
        ADD CONSTRAINT badges_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.categories
        ADD CONSTRAINT categories_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: category_search_data categories_search_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_search_data
        ADD CONSTRAINT categories_search_pkey PRIMARY KEY (category_id);
    
    
    --
    -- Name: category_custom_fields category_custom_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_custom_fields
        ADD CONSTRAINT category_custom_fields_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: category_featured_topics category_featured_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_featured_topics
        ADD CONSTRAINT category_featured_topics_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: category_groups category_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_groups
        ADD CONSTRAINT category_groups_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: category_tag_groups category_tag_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_tag_groups
        ADD CONSTRAINT category_tag_groups_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: category_tag_stats category_tag_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_tag_stats
        ADD CONSTRAINT category_tag_stats_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: category_tags category_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_tags
        ADD CONSTRAINT category_tags_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: category_users category_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.category_users
        ADD CONSTRAINT category_users_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: child_themes child_themes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.child_themes
        ADD CONSTRAINT child_themes_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: color_scheme_colors color_scheme_colors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.color_scheme_colors
        ADD CONSTRAINT color_scheme_colors_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: color_schemes color_schemes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.color_schemes
        ADD CONSTRAINT color_schemes_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: custom_emojis custom_emojis_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.custom_emojis
        ADD CONSTRAINT custom_emojis_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: developers developers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.developers
        ADD CONSTRAINT developers_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: unsubscribe_keys digest_unsubscribe_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.unsubscribe_keys
        ADD CONSTRAINT digest_unsubscribe_keys_pkey PRIMARY KEY (key);
    
    
    --
    -- Name: directory_items directory_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.directory_items
        ADD CONSTRAINT directory_items_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: draft_sequences draft_sequences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.draft_sequences
        ADD CONSTRAINT draft_sequences_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: drafts drafts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.drafts
        ADD CONSTRAINT drafts_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: email_change_requests email_change_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.email_change_requests
        ADD CONSTRAINT email_change_requests_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: email_logs email_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.email_logs
        ADD CONSTRAINT email_logs_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: email_tokens email_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.email_tokens
        ADD CONSTRAINT email_tokens_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: embeddable_hosts embeddable_hosts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.embeddable_hosts
        ADD CONSTRAINT embeddable_hosts_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: facebook_user_infos facebook_user_infos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.facebook_user_infos
        ADD CONSTRAINT facebook_user_infos_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: github_user_infos github_user_infos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.github_user_infos
        ADD CONSTRAINT github_user_infos_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: google_user_infos google_user_infos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.google_user_infos
        ADD CONSTRAINT google_user_infos_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: group_archived_messages group_archived_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_archived_messages
        ADD CONSTRAINT group_archived_messages_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: group_custom_fields group_custom_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_custom_fields
        ADD CONSTRAINT group_custom_fields_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: group_histories group_histories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_histories
        ADD CONSTRAINT group_histories_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: group_mentions group_mentions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_mentions
        ADD CONSTRAINT group_mentions_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: group_users group_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.group_users
        ADD CONSTRAINT group_users_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: groups groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.groups
        ADD CONSTRAINT groups_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: incoming_domains incoming_domains_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_domains
        ADD CONSTRAINT incoming_domains_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: incoming_emails incoming_emails_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_emails
        ADD CONSTRAINT incoming_emails_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: incoming_links incoming_links_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_links
        ADD CONSTRAINT incoming_links_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: incoming_referers incoming_referers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.incoming_referers
        ADD CONSTRAINT incoming_referers_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: instagram_user_infos instagram_user_infos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.instagram_user_infos
        ADD CONSTRAINT instagram_user_infos_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: invited_groups invited_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.invited_groups
        ADD CONSTRAINT invited_groups_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: invites invites_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.invites
        ADD CONSTRAINT invites_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: javascript_caches javascript_caches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.javascript_caches
        ADD CONSTRAINT javascript_caches_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: message_bus message_bus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.message_bus
        ADD CONSTRAINT message_bus_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: muted_users muted_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.muted_users
        ADD CONSTRAINT muted_users_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.notifications
        ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: oauth2_user_infos oauth2_user_infos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.oauth2_user_infos
        ADD CONSTRAINT oauth2_user_infos_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: onceoff_logs onceoff_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.onceoff_logs
        ADD CONSTRAINT onceoff_logs_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: optimized_images optimized_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.optimized_images
        ADD CONSTRAINT optimized_images_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: permalinks permalinks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.permalinks
        ADD CONSTRAINT permalinks_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: plugin_store_rows plugin_store_rows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.plugin_store_rows
        ADD CONSTRAINT plugin_store_rows_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_action_types post_action_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_action_types
        ADD CONSTRAINT post_action_types_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_actions post_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_actions
        ADD CONSTRAINT post_actions_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_custom_fields post_custom_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_custom_fields
        ADD CONSTRAINT post_custom_fields_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_details post_details_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_details
        ADD CONSTRAINT post_details_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_reply_keys post_reply_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_reply_keys
        ADD CONSTRAINT post_reply_keys_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_revisions post_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_revisions
        ADD CONSTRAINT post_revisions_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_stats post_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_stats
        ADD CONSTRAINT post_stats_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_uploads post_uploads_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_uploads
        ADD CONSTRAINT post_uploads_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.posts
        ADD CONSTRAINT posts_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: post_search_data posts_search_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.post_search_data
        ADD CONSTRAINT posts_search_pkey PRIMARY KEY (post_id);
    
    
    --
    -- Name: push_subscriptions push_subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.push_subscriptions
        ADD CONSTRAINT push_subscriptions_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: queued_posts queued_posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.queued_posts
        ADD CONSTRAINT queued_posts_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: quoted_posts quoted_posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.quoted_posts
        ADD CONSTRAINT quoted_posts_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: remote_themes remote_themes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.remote_themes
        ADD CONSTRAINT remote_themes_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: scheduler_stats scheduler_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.scheduler_stats
        ADD CONSTRAINT scheduler_stats_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: schema_migration_details schema_migration_details_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.schema_migration_details
        ADD CONSTRAINT schema_migration_details_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: screened_emails screened_emails_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.screened_emails
        ADD CONSTRAINT screened_emails_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: screened_ip_addresses screened_ip_addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.screened_ip_addresses
        ADD CONSTRAINT screened_ip_addresses_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: screened_urls screened_urls_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.screened_urls
        ADD CONSTRAINT screened_urls_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: search_logs search_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.search_logs
        ADD CONSTRAINT search_logs_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: shared_drafts shared_drafts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.shared_drafts
        ADD CONSTRAINT shared_drafts_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: single_sign_on_records single_sign_on_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.single_sign_on_records
        ADD CONSTRAINT single_sign_on_records_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: site_settings site_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.site_settings
        ADD CONSTRAINT site_settings_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: skipped_email_logs skipped_email_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.skipped_email_logs
        ADD CONSTRAINT skipped_email_logs_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: stylesheet_cache stylesheet_cache_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.stylesheet_cache
        ADD CONSTRAINT stylesheet_cache_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: tag_group_memberships tag_group_memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_group_memberships
        ADD CONSTRAINT tag_group_memberships_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: tag_group_permissions tag_group_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_group_permissions
        ADD CONSTRAINT tag_group_permissions_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: tag_groups tag_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_groups
        ADD CONSTRAINT tag_groups_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: tag_search_data tag_search_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_search_data
        ADD CONSTRAINT tag_search_data_pkey PRIMARY KEY (tag_id);
    
    
    --
    -- Name: tag_users tag_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tag_users
        ADD CONSTRAINT tag_users_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: tags tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.tags
        ADD CONSTRAINT tags_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: theme_fields theme_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.theme_fields
        ADD CONSTRAINT theme_fields_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: theme_settings theme_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.theme_settings
        ADD CONSTRAINT theme_settings_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: themes themes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.themes
        ADD CONSTRAINT themes_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: top_topics top_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.top_topics
        ADD CONSTRAINT top_topics_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_allowed_groups topic_allowed_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_allowed_groups
        ADD CONSTRAINT topic_allowed_groups_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_allowed_users topic_allowed_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_allowed_users
        ADD CONSTRAINT topic_allowed_users_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_custom_fields topic_custom_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_custom_fields
        ADD CONSTRAINT topic_custom_fields_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_embeds topic_embeds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_embeds
        ADD CONSTRAINT topic_embeds_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_invites topic_invites_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_invites
        ADD CONSTRAINT topic_invites_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_link_clicks topic_link_clicks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_link_clicks
        ADD CONSTRAINT topic_link_clicks_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_links topic_links_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_links
        ADD CONSTRAINT topic_links_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_search_data topic_search_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_search_data
        ADD CONSTRAINT topic_search_data_pkey PRIMARY KEY (topic_id);
    
    
    --
    -- Name: topic_tags topic_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_tags
        ADD CONSTRAINT topic_tags_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_timers topic_timers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_timers
        ADD CONSTRAINT topic_timers_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topic_users topic_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topic_users
        ADD CONSTRAINT topic_users_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: topics topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.topics
        ADD CONSTRAINT topics_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: translation_overrides translation_overrides_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.translation_overrides
        ADD CONSTRAINT translation_overrides_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: twitter_user_infos twitter_user_infos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.twitter_user_infos
        ADD CONSTRAINT twitter_user_infos_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: uploads uploads_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.uploads
        ADD CONSTRAINT uploads_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_actions user_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_actions
        ADD CONSTRAINT user_actions_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_api_keys user_api_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_api_keys
        ADD CONSTRAINT user_api_keys_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_archived_messages user_archived_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_archived_messages
        ADD CONSTRAINT user_archived_messages_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_auth_token_logs user_auth_token_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_auth_token_logs
        ADD CONSTRAINT user_auth_token_logs_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_auth_tokens user_auth_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_auth_tokens
        ADD CONSTRAINT user_auth_tokens_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_avatars user_avatars_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_avatars
        ADD CONSTRAINT user_avatars_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_badges user_badges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_badges
        ADD CONSTRAINT user_badges_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_custom_fields user_custom_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_custom_fields
        ADD CONSTRAINT user_custom_fields_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_emails user_emails_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_emails
        ADD CONSTRAINT user_emails_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_exports user_exports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_exports
        ADD CONSTRAINT user_exports_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_field_options user_field_options_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_field_options
        ADD CONSTRAINT user_field_options_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_fields user_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_fields
        ADD CONSTRAINT user_fields_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_histories user_histories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_histories
        ADD CONSTRAINT user_histories_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_open_ids user_open_ids_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_open_ids
        ADD CONSTRAINT user_open_ids_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_profile_views user_profile_views_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_profile_views
        ADD CONSTRAINT user_profile_views_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_profiles
        ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (user_id);
    
    
    --
    -- Name: user_second_factors user_second_factors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_second_factors
        ADD CONSTRAINT user_second_factors_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_stats user_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_stats
        ADD CONSTRAINT user_stats_pkey PRIMARY KEY (user_id);
    
    
    --
    -- Name: user_uploads user_uploads_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_uploads
        ADD CONSTRAINT user_uploads_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_visits user_visits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_visits
        ADD CONSTRAINT user_visits_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_warnings user_warnings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_warnings
        ADD CONSTRAINT user_warnings_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.users
        ADD CONSTRAINT users_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: user_search_data users_search_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.user_search_data
        ADD CONSTRAINT users_search_pkey PRIMARY KEY (user_id);
    
    
    --
    -- Name: watched_words watched_words_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.watched_words
        ADD CONSTRAINT watched_words_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: web_crawler_requests web_crawler_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_crawler_requests
        ADD CONSTRAINT web_crawler_requests_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: web_hook_event_types web_hook_event_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_hook_event_types
        ADD CONSTRAINT web_hook_event_types_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: web_hook_events web_hook_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_hook_events
        ADD CONSTRAINT web_hook_events_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: web_hooks web_hooks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
    --
    
    ALTER TABLE ONLY public.web_hooks
        ADD CONSTRAINT web_hooks_pkey PRIMARY KEY (id);
    
    
    --
    -- Name: by_link; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX by_link ON public.topic_link_clicks USING btree (topic_link_id);
    
    
    --
    -- Name: by_queue_status; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX by_queue_status ON public.queued_posts USING btree (queue, state, created_at);
    
    
    --
    -- Name: by_queue_status_topic; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX by_queue_status_topic ON public.queued_posts USING btree (topic_id, queue, state, created_at);
    
    
    --
    -- Name: cat_featured_threads; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX cat_featured_threads ON public.category_featured_topics USING btree (category_id, topic_id);
    
    
    --
    -- Name: idx_category_tag_groups_ix1; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_category_tag_groups_ix1 ON public.category_tag_groups USING btree (category_id, tag_group_id);
    
    
    --
    -- Name: idx_category_tags_ix1; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_category_tags_ix1 ON public.category_tags USING btree (category_id, tag_id);
    
    
    --
    -- Name: idx_category_tags_ix2; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_category_tags_ix2 ON public.category_tags USING btree (tag_id, category_id);
    
    
    --
    -- Name: idx_category_users_u1; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_category_users_u1 ON public.category_users USING btree (user_id, category_id, notification_level);
    
    
    --
    -- Name: idx_category_users_u2; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_category_users_u2 ON public.category_users USING btree (category_id, user_id, notification_level);
    
    
    --
    -- Name: idx_notifications_speedup_unread_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_notifications_speedup_unread_count ON public.notifications USING btree (user_id, notification_type) WHERE (NOT read);
    
    
    --
    -- Name: idx_post_custom_fields_akismet; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_post_custom_fields_akismet ON public.post_custom_fields USING btree (post_id) WHERE (((name)::text = 'AKISMET_STATE'::text) AND (value = 'needs_review'::text));
    
    
    --
    -- Name: idx_posts_created_at_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_posts_created_at_topic_id ON public.posts USING btree (created_at, topic_id) WHERE (deleted_at IS NULL);
    
    
    --
    -- Name: idx_posts_deleted_posts; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_posts_deleted_posts ON public.posts USING btree (topic_id, post_number) WHERE (deleted_at IS NOT NULL);
    
    
    --
    -- Name: idx_posts_user_id_deleted_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_posts_user_id_deleted_at ON public.posts USING btree (user_id) WHERE (deleted_at IS NULL);
    
    
    --
    -- Name: idx_search_category; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_search_category ON public.category_search_data USING gin (search_data);
    
    
    --
    -- Name: idx_search_post; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_search_post ON public.post_search_data USING gin (search_data);
    
    
    --
    -- Name: idx_search_tag; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_search_tag ON public.tag_search_data USING gin (search_data);
    
    
    --
    -- Name: idx_search_topic; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_search_topic ON public.topic_search_data USING gin (search_data);
    
    
    --
    -- Name: idx_search_user; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_search_user ON public.user_search_data USING gin (search_data);
    
    
    --
    -- Name: idx_tag_users_ix1; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_tag_users_ix1 ON public.tag_users USING btree (user_id, tag_id, notification_level);
    
    
    --
    -- Name: idx_tag_users_ix2; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_tag_users_ix2 ON public.tag_users USING btree (tag_id, user_id, notification_level);
    
    
    --
    -- Name: idx_topic_id_public_type_deleted_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_topic_id_public_type_deleted_at ON public.topic_timers USING btree (topic_id) WHERE ((public_type = true) AND (deleted_at IS NULL));
    
    
    --
    -- Name: idx_topics_front_page; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_topics_front_page ON public.topics USING btree (deleted_at, visible, archetype, category_id, id);
    
    
    --
    -- Name: idx_topics_user_id_deleted_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_topics_user_id_deleted_at ON public.topics USING btree (user_id) WHERE (deleted_at IS NULL);
    
    
    --
    -- Name: idx_unique_actions; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_unique_actions ON public.post_actions USING btree (user_id, post_action_type_id, post_id, targets_topic) WHERE ((deleted_at IS NULL) AND (disagreed_at IS NULL) AND (deferred_at IS NULL));
    
    
    --
    -- Name: idx_unique_flags; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_unique_flags ON public.post_actions USING btree (user_id, post_id, targets_topic) WHERE ((deleted_at IS NULL) AND (disagreed_at IS NULL) AND (deferred_at IS NULL) AND (post_action_type_id = ANY (ARRAY[3, 4, 7, 8])));
    
    
    --
    -- Name: idx_unique_post_uploads; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_unique_post_uploads ON public.post_uploads USING btree (post_id, upload_id);
    
    
    --
    -- Name: idx_unique_rows; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_unique_rows ON public.user_actions USING btree (action_type, user_id, target_topic_id, target_post_id, acting_user_id);
    
    
    --
    -- Name: idx_user_actions_speed_up_user_all; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_user_actions_speed_up_user_all ON public.user_actions USING btree (user_id, created_at, action_type);
    
    
    --
    -- Name: idx_users_admin; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_users_admin ON public.users USING btree (id) WHERE admin;
    
    
    --
    -- Name: idx_users_moderator; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idx_users_moderator ON public.users USING btree (id) WHERE moderator;
    
    
    --
    -- Name: idx_web_hook_event_types_hooks_on_ids; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX idx_web_hook_event_types_hooks_on_ids ON public.web_hook_event_types_hooks USING btree (web_hook_event_type_id, web_hook_id);
    
    
    --
    -- Name: idxtopicslug; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX idxtopicslug ON public.topics USING btree (slug) WHERE ((deleted_at IS NULL) AND (slug IS NOT NULL));
    
    
    --
    -- Name: index_api_keys_on_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_api_keys_on_key ON public.api_keys USING btree (key);
    
    
    --
    -- Name: index_api_keys_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_api_keys_on_user_id ON public.api_keys USING btree (user_id);
    
    
    --
    -- Name: index_application_requests_on_date_and_req_type; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_application_requests_on_date_and_req_type ON public.application_requests USING btree (date, req_type);
    
    
    --
    -- Name: index_badge_types_on_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_badge_types_on_name ON public.badge_types USING btree (name);
    
    
    --
    -- Name: index_badges_on_badge_type_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_badges_on_badge_type_id ON public.badges USING btree (badge_type_id);
    
    
    --
    -- Name: index_badges_on_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_badges_on_name ON public.badges USING btree (name);
    
    
    --
    -- Name: index_categories_on_email_in; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_categories_on_email_in ON public.categories USING btree (email_in);
    
    
    --
    -- Name: index_categories_on_topic_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_categories_on_topic_count ON public.categories USING btree (topic_count);
    
    
    --
    -- Name: index_categories_web_hooks_on_web_hook_id_and_category_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_categories_web_hooks_on_web_hook_id_and_category_id ON public.categories_web_hooks USING btree (web_hook_id, category_id);
    
    
    --
    -- Name: index_category_custom_fields_on_category_id_and_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_category_custom_fields_on_category_id_and_name ON public.category_custom_fields USING btree (category_id, name);
    
    
    --
    -- Name: index_category_featured_topics_on_category_id_and_rank; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_category_featured_topics_on_category_id_and_rank ON public.category_featured_topics USING btree (category_id, rank);
    
    
    --
    -- Name: index_category_tag_stats_on_category_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_category_tag_stats_on_category_id ON public.category_tag_stats USING btree (category_id);
    
    
    --
    -- Name: index_category_tag_stats_on_category_id_and_tag_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_category_tag_stats_on_category_id_and_tag_id ON public.category_tag_stats USING btree (category_id, tag_id);
    
    
    --
    -- Name: index_category_tag_stats_on_category_id_and_topic_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_category_tag_stats_on_category_id_and_topic_count ON public.category_tag_stats USING btree (category_id, topic_count);
    
    
    --
    -- Name: index_category_tag_stats_on_tag_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_category_tag_stats_on_tag_id ON public.category_tag_stats USING btree (tag_id);
    
    
    --
    -- Name: index_child_themes_on_child_theme_id_and_parent_theme_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_child_themes_on_child_theme_id_and_parent_theme_id ON public.child_themes USING btree (child_theme_id, parent_theme_id);
    
    
    --
    -- Name: index_child_themes_on_parent_theme_id_and_child_theme_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_child_themes_on_parent_theme_id_and_child_theme_id ON public.child_themes USING btree (parent_theme_id, child_theme_id);
    
    
    --
    -- Name: index_color_scheme_colors_on_color_scheme_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_color_scheme_colors_on_color_scheme_id ON public.color_scheme_colors USING btree (color_scheme_id);
    
    
    --
    -- Name: index_custom_emojis_on_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_custom_emojis_on_name ON public.custom_emojis USING btree (name);
    
    
    --
    -- Name: index_directory_items_on_days_visited; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_directory_items_on_days_visited ON public.directory_items USING btree (days_visited);
    
    
    --
    -- Name: index_directory_items_on_likes_given; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_directory_items_on_likes_given ON public.directory_items USING btree (likes_given);
    
    
    --
    -- Name: index_directory_items_on_likes_received; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_directory_items_on_likes_received ON public.directory_items USING btree (likes_received);
    
    
    --
    -- Name: index_directory_items_on_period_type_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_directory_items_on_period_type_and_user_id ON public.directory_items USING btree (period_type, user_id);
    
    
    --
    -- Name: index_directory_items_on_post_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_directory_items_on_post_count ON public.directory_items USING btree (post_count);
    
    
    --
    -- Name: index_directory_items_on_posts_read; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_directory_items_on_posts_read ON public.directory_items USING btree (posts_read);
    
    
    --
    -- Name: index_directory_items_on_topic_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_directory_items_on_topic_count ON public.directory_items USING btree (topic_count);
    
    
    --
    -- Name: index_directory_items_on_topics_entered; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_directory_items_on_topics_entered ON public.directory_items USING btree (topics_entered);
    
    
    --
    -- Name: index_draft_sequences_on_user_id_and_draft_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_draft_sequences_on_user_id_and_draft_key ON public.draft_sequences USING btree (user_id, draft_key);
    
    
    --
    -- Name: index_drafts_on_user_id_and_draft_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_drafts_on_user_id_and_draft_key ON public.drafts USING btree (user_id, draft_key);
    
    
    --
    -- Name: index_email_change_requests_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_email_change_requests_on_user_id ON public.email_change_requests USING btree (user_id);
    
    
    --
    -- Name: index_email_logs_on_bounced; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_email_logs_on_bounced ON public.email_logs USING btree (bounced);
    
    
    --
    -- Name: index_email_logs_on_created_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_email_logs_on_created_at ON public.email_logs USING btree (created_at DESC);
    
    
    --
    -- Name: index_email_logs_on_message_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_email_logs_on_message_id ON public.email_logs USING btree (message_id);
    
    
    --
    -- Name: index_email_logs_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_email_logs_on_post_id ON public.email_logs USING btree (post_id);
    
    
    --
    -- Name: index_email_logs_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_email_logs_on_user_id ON public.email_logs USING btree (user_id);
    
    
    --
    -- Name: index_email_tokens_on_token; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_email_tokens_on_token ON public.email_tokens USING btree (token);
    
    
    --
    -- Name: index_email_tokens_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_email_tokens_on_user_id ON public.email_tokens USING btree (user_id);
    
    
    --
    -- Name: index_facebook_user_infos_on_facebook_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_facebook_user_infos_on_facebook_user_id ON public.facebook_user_infos USING btree (facebook_user_id);
    
    
    --
    -- Name: index_facebook_user_infos_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_facebook_user_infos_on_user_id ON public.facebook_user_infos USING btree (user_id);
    
    
    --
    -- Name: index_github_user_infos_on_github_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_github_user_infos_on_github_user_id ON public.github_user_infos USING btree (github_user_id);
    
    
    --
    -- Name: index_github_user_infos_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_github_user_infos_on_user_id ON public.github_user_infos USING btree (user_id);
    
    
    --
    -- Name: index_given_daily_likes_on_limit_reached_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_given_daily_likes_on_limit_reached_and_user_id ON public.given_daily_likes USING btree (limit_reached, user_id);
    
    
    --
    -- Name: index_given_daily_likes_on_user_id_and_given_date; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_given_daily_likes_on_user_id_and_given_date ON public.given_daily_likes USING btree (user_id, given_date);
    
    
    --
    -- Name: index_google_user_infos_on_google_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_google_user_infos_on_google_user_id ON public.google_user_infos USING btree (google_user_id);
    
    
    --
    -- Name: index_google_user_infos_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_google_user_infos_on_user_id ON public.google_user_infos USING btree (user_id);
    
    
    --
    -- Name: index_group_archived_messages_on_group_id_and_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_group_archived_messages_on_group_id_and_topic_id ON public.group_archived_messages USING btree (group_id, topic_id);
    
    
    --
    -- Name: index_group_custom_fields_on_group_id_and_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_group_custom_fields_on_group_id_and_name ON public.group_custom_fields USING btree (group_id, name);
    
    
    --
    -- Name: index_group_histories_on_acting_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_group_histories_on_acting_user_id ON public.group_histories USING btree (acting_user_id);
    
    
    --
    -- Name: index_group_histories_on_action; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_group_histories_on_action ON public.group_histories USING btree (action);
    
    
    --
    -- Name: index_group_histories_on_group_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_group_histories_on_group_id ON public.group_histories USING btree (group_id);
    
    
    --
    -- Name: index_group_histories_on_target_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_group_histories_on_target_user_id ON public.group_histories USING btree (target_user_id);
    
    
    --
    -- Name: index_group_mentions_on_group_id_and_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_group_mentions_on_group_id_and_post_id ON public.group_mentions USING btree (group_id, post_id);
    
    
    --
    -- Name: index_group_mentions_on_post_id_and_group_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_group_mentions_on_post_id_and_group_id ON public.group_mentions USING btree (post_id, group_id);
    
    
    --
    -- Name: index_group_users_on_group_id_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_group_users_on_group_id_and_user_id ON public.group_users USING btree (group_id, user_id);
    
    
    --
    -- Name: index_group_users_on_user_id_and_group_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_group_users_on_user_id_and_group_id ON public.group_users USING btree (user_id, group_id);
    
    
    --
    -- Name: index_groups_on_incoming_email; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_groups_on_incoming_email ON public.groups USING btree (incoming_email);
    
    
    --
    -- Name: index_groups_on_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_groups_on_name ON public.groups USING btree (name);
    
    
    --
    -- Name: index_groups_web_hooks_on_web_hook_id_and_group_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_groups_web_hooks_on_web_hook_id_and_group_id ON public.groups_web_hooks USING btree (web_hook_id, group_id);
    
    
    --
    -- Name: index_incoming_domains_on_name_and_https_and_port; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_incoming_domains_on_name_and_https_and_port ON public.incoming_domains USING btree (name, https, port);
    
    
    --
    -- Name: index_incoming_emails_on_created_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_incoming_emails_on_created_at ON public.incoming_emails USING btree (created_at);
    
    
    --
    -- Name: index_incoming_emails_on_error; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_incoming_emails_on_error ON public.incoming_emails USING btree (error);
    
    
    --
    -- Name: index_incoming_emails_on_message_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_incoming_emails_on_message_id ON public.incoming_emails USING btree (message_id);
    
    
    --
    -- Name: index_incoming_emails_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_incoming_emails_on_post_id ON public.incoming_emails USING btree (post_id);
    
    
    --
    -- Name: index_incoming_links_on_created_at_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_incoming_links_on_created_at_and_user_id ON public.incoming_links USING btree (created_at, user_id);
    
    
    --
    -- Name: index_incoming_links_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_incoming_links_on_post_id ON public.incoming_links USING btree (post_id);
    
    
    --
    -- Name: index_incoming_referers_on_path_and_incoming_domain_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_incoming_referers_on_path_and_incoming_domain_id ON public.incoming_referers USING btree (path, incoming_domain_id);
    
    
    --
    -- Name: index_invites_on_email_and_invited_by_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_invites_on_email_and_invited_by_id ON public.invites USING btree (email, invited_by_id);
    
    
    --
    -- Name: index_invites_on_invite_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_invites_on_invite_key ON public.invites USING btree (invite_key);
    
    
    --
    -- Name: index_javascript_caches_on_digest; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_javascript_caches_on_digest ON public.javascript_caches USING btree (digest);
    
    
    --
    -- Name: index_javascript_caches_on_theme_field_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_javascript_caches_on_theme_field_id ON public.javascript_caches USING btree (theme_field_id);
    
    
    --
    -- Name: index_message_bus_on_created_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_message_bus_on_created_at ON public.message_bus USING btree (created_at);
    
    
    --
    -- Name: index_muted_users_on_muted_user_id_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_muted_users_on_muted_user_id_and_user_id ON public.muted_users USING btree (muted_user_id, user_id);
    
    
    --
    -- Name: index_muted_users_on_user_id_and_muted_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_muted_users_on_user_id_and_muted_user_id ON public.muted_users USING btree (user_id, muted_user_id);
    
    
    --
    -- Name: index_notifications_on_post_action_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_notifications_on_post_action_id ON public.notifications USING btree (post_action_id);
    
    
    --
    -- Name: index_notifications_on_user_id_and_created_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_notifications_on_user_id_and_created_at ON public.notifications USING btree (user_id, created_at);
    
    
    --
    -- Name: index_notifications_on_user_id_and_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_notifications_on_user_id_and_id ON public.notifications USING btree (user_id, id) WHERE ((notification_type = 6) AND (NOT read));
    
    
    --
    -- Name: index_notifications_on_user_id_and_topic_id_and_post_number; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_notifications_on_user_id_and_topic_id_and_post_number ON public.notifications USING btree (user_id, topic_id, post_number);
    
    
    --
    -- Name: index_oauth2_user_infos_on_uid_and_provider; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_oauth2_user_infos_on_uid_and_provider ON public.oauth2_user_infos USING btree (uid, provider);
    
    
    --
    -- Name: index_onceoff_logs_on_job_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_onceoff_logs_on_job_name ON public.onceoff_logs USING btree (job_name);
    
    
    --
    -- Name: index_optimized_images_on_upload_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_optimized_images_on_upload_id ON public.optimized_images USING btree (upload_id);
    
    
    --
    -- Name: index_optimized_images_on_upload_id_and_width_and_height; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_optimized_images_on_upload_id_and_width_and_height ON public.optimized_images USING btree (upload_id, width, height);
    
    
    --
    -- Name: index_permalinks_on_url; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_permalinks_on_url ON public.permalinks USING btree (url);
    
    
    --
    -- Name: index_plugin_store_rows_on_plugin_name_and_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_plugin_store_rows_on_plugin_name_and_key ON public.plugin_store_rows USING btree (plugin_name, key);
    
    
    --
    -- Name: index_post_actions_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_actions_on_post_id ON public.post_actions USING btree (post_id);
    
    
    --
    -- Name: index_post_actions_on_user_id_and_post_action_type_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_actions_on_user_id_and_post_action_type_id ON public.post_actions USING btree (user_id, post_action_type_id) WHERE (deleted_at IS NULL);
    
    
    --
    -- Name: index_post_custom_fields_on_name_and_value; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_custom_fields_on_name_and_value ON public.post_custom_fields USING btree (name, "left"(value, 200));
    
    
    --
    -- Name: index_post_custom_fields_on_post_id_and_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_custom_fields_on_post_id_and_name ON public.post_custom_fields USING btree (post_id, name);
    
    
    --
    -- Name: index_post_details_on_post_id_and_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_post_details_on_post_id_and_key ON public.post_details USING btree (post_id, key);
    
    
    --
    -- Name: index_post_replies_on_post_id_and_reply_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_post_replies_on_post_id_and_reply_id ON public.post_replies USING btree (post_id, reply_id);
    
    
    --
    -- Name: index_post_reply_keys_on_reply_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_post_reply_keys_on_reply_key ON public.post_reply_keys USING btree (reply_key);
    
    
    --
    -- Name: index_post_reply_keys_on_user_id_and_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_post_reply_keys_on_user_id_and_post_id ON public.post_reply_keys USING btree (user_id, post_id);
    
    
    --
    -- Name: index_post_revisions_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_revisions_on_post_id ON public.post_revisions USING btree (post_id);
    
    
    --
    -- Name: index_post_revisions_on_post_id_and_number; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_revisions_on_post_id_and_number ON public.post_revisions USING btree (post_id, number);
    
    
    --
    -- Name: index_post_stats_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_stats_on_post_id ON public.post_stats USING btree (post_id);
    
    
    --
    -- Name: index_post_timings_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_timings_on_user_id ON public.post_timings USING btree (user_id);
    
    
    --
    -- Name: index_post_uploads_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_uploads_on_post_id ON public.post_uploads USING btree (post_id);
    
    
    --
    -- Name: index_post_uploads_on_upload_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_post_uploads_on_upload_id ON public.post_uploads USING btree (upload_id);
    
    
    --
    -- Name: index_posts_on_reply_to_post_number; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_posts_on_reply_to_post_number ON public.posts USING btree (reply_to_post_number);
    
    
    --
    -- Name: index_posts_on_topic_id_and_percent_rank; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_posts_on_topic_id_and_percent_rank ON public.posts USING btree (topic_id, percent_rank);
    
    
    --
    -- Name: index_posts_on_topic_id_and_post_number; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_posts_on_topic_id_and_post_number ON public.posts USING btree (topic_id, post_number);
    
    
    --
    -- Name: index_posts_on_topic_id_and_sort_order; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_posts_on_topic_id_and_sort_order ON public.posts USING btree (topic_id, sort_order);
    
    
    --
    -- Name: index_posts_on_user_id_and_created_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_posts_on_user_id_and_created_at ON public.posts USING btree (user_id, created_at);
    
    
    --
    -- Name: index_quoted_posts_on_post_id_and_quoted_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_quoted_posts_on_post_id_and_quoted_post_id ON public.quoted_posts USING btree (post_id, quoted_post_id);
    
    
    --
    -- Name: index_quoted_posts_on_quoted_post_id_and_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_quoted_posts_on_quoted_post_id_and_post_id ON public.quoted_posts USING btree (quoted_post_id, post_id);
    
    
    --
    -- Name: index_schema_migration_details_on_version; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_schema_migration_details_on_version ON public.schema_migration_details USING btree (version);
    
    
    --
    -- Name: index_screened_emails_on_email; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_screened_emails_on_email ON public.screened_emails USING btree (email);
    
    
    --
    -- Name: index_screened_emails_on_last_match_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_screened_emails_on_last_match_at ON public.screened_emails USING btree (last_match_at);
    
    
    --
    -- Name: index_screened_ip_addresses_on_ip_address; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_screened_ip_addresses_on_ip_address ON public.screened_ip_addresses USING btree (ip_address);
    
    
    --
    -- Name: index_screened_ip_addresses_on_last_match_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_screened_ip_addresses_on_last_match_at ON public.screened_ip_addresses USING btree (last_match_at);
    
    
    --
    -- Name: index_screened_urls_on_last_match_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_screened_urls_on_last_match_at ON public.screened_urls USING btree (last_match_at);
    
    
    --
    -- Name: index_screened_urls_on_url; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_screened_urls_on_url ON public.screened_urls USING btree (url);
    
    
    --
    -- Name: index_shared_drafts_on_category_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_shared_drafts_on_category_id ON public.shared_drafts USING btree (category_id);
    
    
    --
    -- Name: index_shared_drafts_on_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_shared_drafts_on_topic_id ON public.shared_drafts USING btree (topic_id);
    
    
    --
    -- Name: index_single_sign_on_records_on_external_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_single_sign_on_records_on_external_id ON public.single_sign_on_records USING btree (external_id);
    
    
    --
    -- Name: index_skipped_email_logs_on_created_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_skipped_email_logs_on_created_at ON public.skipped_email_logs USING btree (created_at);
    
    
    --
    -- Name: index_skipped_email_logs_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_skipped_email_logs_on_post_id ON public.skipped_email_logs USING btree (post_id);
    
    
    --
    -- Name: index_skipped_email_logs_on_reason_type; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_skipped_email_logs_on_reason_type ON public.skipped_email_logs USING btree (reason_type);
    
    
    --
    -- Name: index_skipped_email_logs_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_skipped_email_logs_on_user_id ON public.skipped_email_logs USING btree (user_id);
    
    
    --
    -- Name: index_stylesheet_cache_on_target_and_digest; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_stylesheet_cache_on_target_and_digest ON public.stylesheet_cache USING btree (target, digest);
    
    
    --
    -- Name: index_tag_group_memberships_on_tag_group_id_and_tag_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_tag_group_memberships_on_tag_group_id_and_tag_id ON public.tag_group_memberships USING btree (tag_group_id, tag_id);
    
    
    --
    -- Name: index_tag_group_permissions_on_group_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_tag_group_permissions_on_group_id ON public.tag_group_permissions USING btree (group_id);
    
    
    --
    -- Name: index_tag_group_permissions_on_tag_group_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_tag_group_permissions_on_tag_group_id ON public.tag_group_permissions USING btree (tag_group_id);
    
    
    --
    -- Name: index_tags_on_lower_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_tags_on_lower_name ON public.tags USING btree (lower((name)::text));
    
    
    --
    -- Name: index_tags_on_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_tags_on_name ON public.tags USING btree (name);
    
    
    --
    -- Name: index_themes_on_remote_theme_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_themes_on_remote_theme_id ON public.themes USING btree (remote_theme_id);
    
    
    --
    -- Name: index_top_topics_on_all_score; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_all_score ON public.top_topics USING btree (all_score);
    
    
    --
    -- Name: index_top_topics_on_daily_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_daily_likes_count ON public.top_topics USING btree (daily_likes_count DESC);
    
    
    --
    -- Name: index_top_topics_on_daily_op_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_daily_op_likes_count ON public.top_topics USING btree (daily_op_likes_count);
    
    
    --
    -- Name: index_top_topics_on_daily_posts_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_daily_posts_count ON public.top_topics USING btree (daily_posts_count DESC);
    
    
    --
    -- Name: index_top_topics_on_daily_score; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_daily_score ON public.top_topics USING btree (daily_score);
    
    
    --
    -- Name: index_top_topics_on_daily_views_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_daily_views_count ON public.top_topics USING btree (daily_views_count DESC);
    
    
    --
    -- Name: index_top_topics_on_monthly_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_monthly_likes_count ON public.top_topics USING btree (monthly_likes_count DESC);
    
    
    --
    -- Name: index_top_topics_on_monthly_op_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_monthly_op_likes_count ON public.top_topics USING btree (monthly_op_likes_count);
    
    
    --
    -- Name: index_top_topics_on_monthly_posts_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_monthly_posts_count ON public.top_topics USING btree (monthly_posts_count DESC);
    
    
    --
    -- Name: index_top_topics_on_monthly_score; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_monthly_score ON public.top_topics USING btree (monthly_score);
    
    
    --
    -- Name: index_top_topics_on_monthly_views_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_monthly_views_count ON public.top_topics USING btree (monthly_views_count DESC);
    
    
    --
    -- Name: index_top_topics_on_quarterly_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_quarterly_likes_count ON public.top_topics USING btree (quarterly_likes_count);
    
    
    --
    -- Name: index_top_topics_on_quarterly_op_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_quarterly_op_likes_count ON public.top_topics USING btree (quarterly_op_likes_count);
    
    
    --
    -- Name: index_top_topics_on_quarterly_posts_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_quarterly_posts_count ON public.top_topics USING btree (quarterly_posts_count);
    
    
    --
    -- Name: index_top_topics_on_quarterly_views_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_quarterly_views_count ON public.top_topics USING btree (quarterly_views_count);
    
    
    --
    -- Name: index_top_topics_on_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_top_topics_on_topic_id ON public.top_topics USING btree (topic_id);
    
    
    --
    -- Name: index_top_topics_on_weekly_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_weekly_likes_count ON public.top_topics USING btree (weekly_likes_count DESC);
    
    
    --
    -- Name: index_top_topics_on_weekly_op_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_weekly_op_likes_count ON public.top_topics USING btree (weekly_op_likes_count);
    
    
    --
    -- Name: index_top_topics_on_weekly_posts_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_weekly_posts_count ON public.top_topics USING btree (weekly_posts_count DESC);
    
    
    --
    -- Name: index_top_topics_on_weekly_score; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_weekly_score ON public.top_topics USING btree (weekly_score);
    
    
    --
    -- Name: index_top_topics_on_weekly_views_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_weekly_views_count ON public.top_topics USING btree (weekly_views_count DESC);
    
    
    --
    -- Name: index_top_topics_on_yearly_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_yearly_likes_count ON public.top_topics USING btree (yearly_likes_count DESC);
    
    
    --
    -- Name: index_top_topics_on_yearly_op_likes_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_yearly_op_likes_count ON public.top_topics USING btree (yearly_op_likes_count);
    
    
    --
    -- Name: index_top_topics_on_yearly_posts_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_yearly_posts_count ON public.top_topics USING btree (yearly_posts_count DESC);
    
    
    --
    -- Name: index_top_topics_on_yearly_score; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_yearly_score ON public.top_topics USING btree (yearly_score);
    
    
    --
    -- Name: index_top_topics_on_yearly_views_count; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_top_topics_on_yearly_views_count ON public.top_topics USING btree (yearly_views_count DESC);
    
    
    --
    -- Name: index_topic_allowed_groups_on_group_id_and_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_allowed_groups_on_group_id_and_topic_id ON public.topic_allowed_groups USING btree (group_id, topic_id);
    
    
    --
    -- Name: index_topic_allowed_groups_on_topic_id_and_group_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_allowed_groups_on_topic_id_and_group_id ON public.topic_allowed_groups USING btree (topic_id, group_id);
    
    
    --
    -- Name: index_topic_allowed_users_on_topic_id_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_allowed_users_on_topic_id_and_user_id ON public.topic_allowed_users USING btree (topic_id, user_id);
    
    
    --
    -- Name: index_topic_allowed_users_on_user_id_and_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_allowed_users_on_user_id_and_topic_id ON public.topic_allowed_users USING btree (user_id, topic_id);
    
    
    --
    -- Name: index_topic_custom_fields_on_topic_id_and_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_custom_fields_on_topic_id_and_name ON public.topic_custom_fields USING btree (topic_id, name);
    
    
    --
    -- Name: index_topic_embeds_on_embed_url; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_embeds_on_embed_url ON public.topic_embeds USING btree (embed_url);
    
    
    --
    -- Name: index_topic_invites_on_invite_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_invites_on_invite_id ON public.topic_invites USING btree (invite_id);
    
    
    --
    -- Name: index_topic_invites_on_topic_id_and_invite_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_invites_on_topic_id_and_invite_id ON public.topic_invites USING btree (topic_id, invite_id);
    
    
    --
    -- Name: index_topic_links_on_extension; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_links_on_extension ON public.topic_links USING btree (extension);
    
    
    --
    -- Name: index_topic_links_on_link_post_id_and_reflection; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_links_on_link_post_id_and_reflection ON public.topic_links USING btree (link_post_id, reflection);
    
    
    --
    -- Name: index_topic_links_on_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_links_on_post_id ON public.topic_links USING btree (post_id);
    
    
    --
    -- Name: index_topic_links_on_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_links_on_topic_id ON public.topic_links USING btree (topic_id);
    
    
    --
    -- Name: index_topic_tags_on_topic_id_and_tag_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_tags_on_topic_id_and_tag_id ON public.topic_tags USING btree (topic_id, tag_id);
    
    
    --
    -- Name: index_topic_timers_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_timers_on_user_id ON public.topic_timers USING btree (user_id);
    
    
    --
    -- Name: index_topic_users_on_topic_id_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_users_on_topic_id_and_user_id ON public.topic_users USING btree (topic_id, user_id);
    
    
    --
    -- Name: index_topic_users_on_user_id_and_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_topic_users_on_user_id_and_topic_id ON public.topic_users USING btree (user_id, topic_id);
    
    
    --
    -- Name: index_topic_views_on_topic_id_and_viewed_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_views_on_topic_id_and_viewed_at ON public.topic_views USING btree (topic_id, viewed_at);
    
    
    --
    -- Name: index_topic_views_on_user_id_and_viewed_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_views_on_user_id_and_viewed_at ON public.topic_views USING btree (user_id, viewed_at);
    
    
    --
    -- Name: index_topic_views_on_viewed_at_and_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topic_views_on_viewed_at_and_topic_id ON public.topic_views USING btree (viewed_at, topic_id);
    
    
    --
    -- Name: index_topics_on_bumped_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topics_on_bumped_at ON public.topics USING btree (bumped_at DESC);
    
    
    --
    -- Name: index_topics_on_created_at_and_visible; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topics_on_created_at_and_visible ON public.topics USING btree (created_at, visible) WHERE ((deleted_at IS NULL) AND ((archetype)::text <> 'private_message'::text));
    
    
    --
    -- Name: index_topics_on_id_and_deleted_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topics_on_id_and_deleted_at ON public.topics USING btree (id, deleted_at);
    
    
    --
    -- Name: index_topics_on_lower_title; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topics_on_lower_title ON public.topics USING btree (lower((title)::text));
    
    
    --
    -- Name: index_topics_on_pinned_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topics_on_pinned_at ON public.topics USING btree (pinned_at) WHERE (pinned_at IS NOT NULL);
    
    
    --
    -- Name: index_topics_on_pinned_globally; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_topics_on_pinned_globally ON public.topics USING btree (pinned_globally) WHERE pinned_globally;
    
    
    --
    -- Name: index_translation_overrides_on_locale_and_translation_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_translation_overrides_on_locale_and_translation_key ON public.translation_overrides USING btree (locale, translation_key);
    
    
    --
    -- Name: index_twitter_user_infos_on_twitter_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_twitter_user_infos_on_twitter_user_id ON public.twitter_user_infos USING btree (twitter_user_id);
    
    
    --
    -- Name: index_twitter_user_infos_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_twitter_user_infos_on_user_id ON public.twitter_user_infos USING btree (user_id);
    
    
    --
    -- Name: index_unsubscribe_keys_on_created_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_unsubscribe_keys_on_created_at ON public.unsubscribe_keys USING btree (created_at);
    
    
    --
    -- Name: index_uploads_on_extension; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_uploads_on_extension ON public.uploads USING btree (lower((extension)::text));
    
    
    --
    -- Name: index_uploads_on_id_and_url; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_uploads_on_id_and_url ON public.uploads USING btree (id, url);
    
    
    --
    -- Name: index_uploads_on_sha1; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_uploads_on_sha1 ON public.uploads USING btree (sha1);
    
    
    --
    -- Name: index_uploads_on_url; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_uploads_on_url ON public.uploads USING btree (url);
    
    
    --
    -- Name: index_uploads_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_uploads_on_user_id ON public.uploads USING btree (user_id);
    
    
    --
    -- Name: index_user_actions_on_acting_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_actions_on_acting_user_id ON public.user_actions USING btree (acting_user_id);
    
    
    --
    -- Name: index_user_actions_on_target_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_actions_on_target_post_id ON public.user_actions USING btree (target_post_id);
    
    
    --
    -- Name: index_user_actions_on_user_id_and_action_type; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_actions_on_user_id_and_action_type ON public.user_actions USING btree (user_id, action_type);
    
    
    --
    -- Name: index_user_api_keys_on_client_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_api_keys_on_client_id ON public.user_api_keys USING btree (client_id);
    
    
    --
    -- Name: index_user_api_keys_on_key; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_api_keys_on_key ON public.user_api_keys USING btree (key);
    
    
    --
    -- Name: index_user_api_keys_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_api_keys_on_user_id ON public.user_api_keys USING btree (user_id);
    
    
    --
    -- Name: index_user_archived_messages_on_user_id_and_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_archived_messages_on_user_id_and_topic_id ON public.user_archived_messages USING btree (user_id, topic_id);
    
    
    --
    -- Name: index_user_auth_tokens_on_auth_token; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_auth_tokens_on_auth_token ON public.user_auth_tokens USING btree (auth_token);
    
    
    --
    -- Name: index_user_auth_tokens_on_prev_auth_token; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_auth_tokens_on_prev_auth_token ON public.user_auth_tokens USING btree (prev_auth_token);
    
    
    --
    -- Name: index_user_avatars_on_custom_upload_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_avatars_on_custom_upload_id ON public.user_avatars USING btree (custom_upload_id);
    
    
    --
    -- Name: index_user_avatars_on_gravatar_upload_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_avatars_on_gravatar_upload_id ON public.user_avatars USING btree (gravatar_upload_id);
    
    
    --
    -- Name: index_user_avatars_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_avatars_on_user_id ON public.user_avatars USING btree (user_id);
    
    
    --
    -- Name: index_user_badges_on_badge_id_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_badges_on_badge_id_and_user_id ON public.user_badges USING btree (badge_id, user_id);
    
    
    --
    -- Name: index_user_badges_on_badge_id_and_user_id_and_post_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_badges_on_badge_id_and_user_id_and_post_id ON public.user_badges USING btree (badge_id, user_id, post_id) WHERE (post_id IS NOT NULL);
    
    
    --
    -- Name: index_user_badges_on_badge_id_and_user_id_and_seq; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_badges_on_badge_id_and_user_id_and_seq ON public.user_badges USING btree (badge_id, user_id, seq) WHERE (post_id IS NULL);
    
    
    --
    -- Name: index_user_badges_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_badges_on_user_id ON public.user_badges USING btree (user_id);
    
    
    --
    -- Name: index_user_custom_fields_on_user_id_and_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_custom_fields_on_user_id_and_name ON public.user_custom_fields USING btree (user_id, name);
    
    
    --
    -- Name: index_user_emails_on_email; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_emails_on_email ON public.user_emails USING btree (lower((email)::text));
    
    
    --
    -- Name: index_user_emails_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_emails_on_user_id ON public.user_emails USING btree (user_id);
    
    
    --
    -- Name: index_user_emails_on_user_id_and_primary; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_emails_on_user_id_and_primary ON public.user_emails USING btree (user_id, "primary") WHERE "primary";
    
    
    --
    -- Name: index_user_histories_on_acting_user_id_and_action_and_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_histories_on_acting_user_id_and_action_and_id ON public.user_histories USING btree (acting_user_id, action, id);
    
    
    --
    -- Name: index_user_histories_on_action_and_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_histories_on_action_and_id ON public.user_histories USING btree (action, id);
    
    
    --
    -- Name: index_user_histories_on_category_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_histories_on_category_id ON public.user_histories USING btree (category_id);
    
    
    --
    -- Name: index_user_histories_on_subject_and_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_histories_on_subject_and_id ON public.user_histories USING btree (subject, id);
    
    
    --
    -- Name: index_user_histories_on_target_user_id_and_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_histories_on_target_user_id_and_id ON public.user_histories USING btree (target_user_id, id);
    
    
    --
    -- Name: index_user_open_ids_on_url; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_open_ids_on_url ON public.user_open_ids USING btree (url);
    
    
    --
    -- Name: index_user_options_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_options_on_user_id ON public.user_options USING btree (user_id);
    
    
    --
    -- Name: index_user_profile_views_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_profile_views_on_user_id ON public.user_profile_views USING btree (user_id);
    
    
    --
    -- Name: index_user_profile_views_on_user_profile_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_profile_views_on_user_profile_id ON public.user_profile_views USING btree (user_profile_id);
    
    
    --
    -- Name: index_user_profiles_on_bio_cooked_version; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_profiles_on_bio_cooked_version ON public.user_profiles USING btree (bio_cooked_version);
    
    
    --
    -- Name: index_user_profiles_on_card_background; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_profiles_on_card_background ON public.user_profiles USING btree (card_background);
    
    
    --
    -- Name: index_user_profiles_on_profile_background; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_profiles_on_profile_background ON public.user_profiles USING btree (profile_background);
    
    
    --
    -- Name: index_user_second_factors_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_second_factors_on_user_id ON public.user_second_factors USING btree (user_id);
    
    
    --
    -- Name: index_user_uploads_on_upload_id_and_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_uploads_on_upload_id_and_user_id ON public.user_uploads USING btree (upload_id, user_id);
    
    
    --
    -- Name: index_user_visits_on_user_id_and_visited_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_visits_on_user_id_and_visited_at ON public.user_visits USING btree (user_id, visited_at);
    
    
    --
    -- Name: index_user_visits_on_user_id_and_visited_at_and_time_read; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_visits_on_user_id_and_visited_at_and_time_read ON public.user_visits USING btree (user_id, visited_at, time_read);
    
    
    --
    -- Name: index_user_visits_on_visited_at_and_mobile; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_visits_on_visited_at_and_mobile ON public.user_visits USING btree (visited_at, mobile);
    
    
    --
    -- Name: index_user_warnings_on_topic_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_user_warnings_on_topic_id ON public.user_warnings USING btree (topic_id);
    
    
    --
    -- Name: index_user_warnings_on_user_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_user_warnings_on_user_id ON public.user_warnings USING btree (user_id);
    
    
    --
    -- Name: index_users_on_last_posted_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_users_on_last_posted_at ON public.users USING btree (last_posted_at);
    
    
    --
    -- Name: index_users_on_last_seen_at; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_users_on_last_seen_at ON public.users USING btree (last_seen_at);
    
    
    --
    -- Name: index_users_on_uploaded_avatar_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_users_on_uploaded_avatar_id ON public.users USING btree (uploaded_avatar_id);
    
    
    --
    -- Name: index_users_on_username; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_users_on_username ON public.users USING btree (username);
    
    
    --
    -- Name: index_users_on_username_lower; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_users_on_username_lower ON public.users USING btree (username_lower);
    
    
    --
    -- Name: index_watched_words_on_action_and_word; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_watched_words_on_action_and_word ON public.watched_words USING btree (action, word);
    
    
    --
    -- Name: index_web_crawler_requests_on_date_and_user_agent; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX index_web_crawler_requests_on_date_and_user_agent ON public.web_crawler_requests USING btree (date, user_agent);
    
    
    --
    -- Name: index_web_hook_events_on_web_hook_id; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX index_web_hook_events_on_web_hook_id ON public.web_hook_events USING btree (web_hook_id);
    
    
    --
    -- Name: post_timings_summary; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX post_timings_summary ON public.post_timings USING btree (topic_id, post_number);
    
    
    --
    -- Name: post_timings_unique; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX post_timings_unique ON public.post_timings USING btree (topic_id, post_number, user_id);
    
    
    --
    -- Name: theme_field_unique_index; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX theme_field_unique_index ON public.theme_fields USING btree (theme_id, target_id, type_id, name);
    
    
    --
    -- Name: topic_custom_fields_value_key_idx; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE INDEX topic_custom_fields_value_key_idx ON public.topic_custom_fields USING btree (value, name) WHERE ((value IS NOT NULL) AND (char_length(value) < 400));
    
    
    --
    -- Name: uniq_ip_or_user_id_topic_views; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX uniq_ip_or_user_id_topic_views ON public.topic_views USING btree (user_id, ip_address, topic_id);
    
    
    --
    -- Name: unique_index_categories_on_name; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX unique_index_categories_on_name ON public.categories USING btree (COALESCE(parent_category_id, '-1'::integer), name);
    
    
    --
    -- Name: unique_post_links; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX unique_post_links ON public.topic_links USING btree (topic_id, post_id, url);
    
    
    --
    -- Name: unique_profile_view_user_or_ip; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX unique_profile_view_user_or_ip ON public.user_profile_views USING btree (viewed_at, user_id, ip_address, user_profile_id);
    
    
    --
    -- Name: unique_schema_migrations; Type: INDEX; Schema: public; Owner: -
    --
    
    CREATE UNIQUE INDEX unique_schema_migrations ON public.schema_migrations USING btree (version);
        
    
    --
    -- PostgreSQL database dump complete
    --
    
    `, `public`)
  }, 20000)
})
