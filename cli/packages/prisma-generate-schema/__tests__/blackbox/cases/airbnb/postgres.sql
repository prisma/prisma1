--
-- PostgreSQL database dump
--
-- Dumped from database version 10.1
-- Dumped by pg_dump version 10.5
SET statement_timeout
= 0;
SET lock_timeout
= 0;
SET idle_in_transaction_session_timeout
= 0;
SET client_encoding
= 'UTF8';
SET standard_conforming_strings
= on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies
= false;
SET client_min_messages
= warning;
SET row_security
= off;
--
-- Name: schema-generator$airbnb; Type: SCHEMA; Schema: -; Owner: -
--
CREATE SCHEMA "schema-generator$airbnb";
SET default_tablespace
= '';
SET default_with_oids
= false;
--
-- Name: Amenities; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
--
CREATE TABLE "schema-generator$airbnb"."Amenities"
(
    id character varying(25) NOT NULL,
    elevator boolean NOT NULL,
    "petsAllowed" boolean NOT NULL,
    internet boolean NOT NULL,
    kitchen boolean NOT NULL,
    "wirelessInternet" boolean NOT NULL,
    "familyKidFriendly" boolean NOT NULL,
    "freeParkingOnPremises" boolean NOT NULL,
    "hotTub" boolean NOT NULL,
    pool boolean NOT NULL,
    "smokingAllowed" boolean NOT NULL,
    "wheelchairAccessible" boolean NOT NULL,
    breakfast boolean NOT NULL,
    "cableTv" boolean NOT NULL,
    "suitableForEvents" boolean NOT NULL,
    dryer boolean NOT NULL,
    washer boolean NOT NULL,
    "indoorFireplace" boolean NOT NULL,
    tv boolean NOT NULL,
    heating boolean NOT NULL,
    hangers boolean NOT NULL,
    iron boolean NOT NULL,
    "hairDryer" boolean NOT NULL,
    doorman boolean NOT NULL,
    "paidParkingOffPremises" boolean NOT NULL,
    "freeParkingOnStreet" boolean NOT NULL,
    gym boolean NOT NULL,
    "airConditioning" boolean NOT NULL,
    shampoo boolean NOT NULL,
    essentials boolean NOT NULL,
    "laptopFriendlyWorkspace" boolean NOT NULL,
    "privateEntrance" boolean NOT NULL,
    "buzzerWirelessIntercom" boolean NOT NULL,
    "babyBath" boolean NOT NULL,
    "babyMonitor" boolean NOT NULL,
    "babysitterRecommendations" boolean NOT NULL,
    bathtub boolean NOT NULL,
    "changingTable" boolean NOT NULL,
    "childrensBooksAndToys" boolean NOT NULL,
    "childrensDinnerware" boolean NOT NULL,
    crib boolean NOT NULL,
    "updatedAt" timestamp(3)
    without time zone NOT NULL,
    "createdAt" timestamp
    (3) without time zone NOT NULL
);
    --
    -- Name: Booking; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
    --
    CREATE TABLE "schema-generator$airbnb"."Booking"
    (
        id character varying(25) NOT NULL,
        "createdAt" timestamp(3)
        without time zone NOT NULL,
    "startDate" timestamp
        (3) without time zone NOT NULL,
    "endDate" timestamp
        (3) without time zone NOT NULL,
    "updatedAt" timestamp
        (3) without time zone NOT NULL
);
        --
        -- Name: City; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
        --
        CREATE TABLE "schema-generator$airbnb"."City"
        (
            id character varying(25) NOT NULL,
            name text NOT NULL,
            "updatedAt" timestamp(3)
            without time zone NOT NULL,
    "createdAt" timestamp
            (3) without time zone NOT NULL
);
            --
            -- Name: CreditCardInformation; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
            --
            CREATE TABLE "schema-generator$airbnb"."CreditCardInformation"
            (
                id character varying(25) NOT NULL,
                "createdAt" timestamp(3)
                without time zone NOT NULL,
    "cardNumber" text NOT NULL,
    "expiresOnMonth" integer NOT NULL,
    "expiresOnYear" integer NOT NULL,
    "securityCode" text NOT NULL,
    "firstName" text NOT NULL,
    "lastName" text NOT NULL,
    "postalCode" text NOT NULL,
    country text NOT NULL,
    "updatedAt" timestamp
                (3) without time zone NOT NULL
);
                --
                -- Name: Experience; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                --
                CREATE TABLE "schema-generator$airbnb"."Experience"
                (
                    id character varying(25) NOT NULL,
                    title text NOT NULL,
                    "pricePerPerson" integer NOT NULL,
                    popularity integer NOT NULL,
                    "updatedAt" timestamp(3)
                    without time zone NOT NULL,
    "createdAt" timestamp
                    (3) without time zone NOT NULL
);
                    --
                    -- Name: ExperienceCategory; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                    --
                    CREATE TABLE "schema-generator$airbnb"."ExperienceCategory"
                    (
                        id character varying(25) NOT NULL,
                        "mainColor" text NOT NULL,
                        name text NOT NULL,
                        "updatedAt" timestamp(3)
                        without time zone NOT NULL,
    "createdAt" timestamp
                        (3) without time zone NOT NULL
);
                        --
                        -- Name: GuestRequirements; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                        --
                        CREATE TABLE "schema-generator$airbnb"."GuestRequirements"
                        (
                            id character varying(25) NOT NULL,
                            "govIssuedId" boolean NOT NULL,
                            "recommendationsFromOtherHosts" boolean NOT NULL,
                            "guestTripInformation" boolean NOT NULL,
                            "updatedAt" timestamp(3)
                            without time zone NOT NULL,
    "createdAt" timestamp
                            (3) without time zone NOT NULL
);
                            --
                            -- Name: HouseRules; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                            --
                            CREATE TABLE "schema-generator$airbnb"."HouseRules"
                            (
                                id character varying(25) NOT NULL,
                                "createdAt" timestamp(3)
                                without time zone NOT NULL,
    "updatedAt" timestamp
                                (3) without time zone NOT NULL,
    "suitableForChildren" boolean,
    "suitableForInfants" boolean,
    "petsAllowed" boolean,
    "smokingAllowed" boolean,
    "partiesAndEventsAllowed" boolean,
    "additionalRules" text
);
                                --
                                -- Name: Location; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                --
                                CREATE TABLE "schema-generator$airbnb"."Location"
                                (
                                    id character varying(25) NOT NULL,
                                    lat numeric(65,30) NOT NULL,
                                    lng numeric(65,30) NOT NULL,
                                    address text,
                                    directions text,
                                    "updatedAt" timestamp(3)
                                    without time zone NOT NULL,
    "createdAt" timestamp
                                    (3) without time zone NOT NULL
);
                                    --
                                    -- Name: Message; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                    --
                                    CREATE TABLE "schema-generator$airbnb"."Message"
                                    (
                                        id character varying(25) NOT NULL,
                                        "createdAt" timestamp(3)
                                        without time zone NOT NULL,
    "deliveredAt" timestamp
                                        (3) without time zone NOT NULL,
    "readAt" timestamp
                                        (3) without time zone NOT NULL,
    "updatedAt" timestamp
                                        (3) without time zone NOT NULL
);
                                        --
                                        -- Name: Neighbourhood; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                        --
                                        CREATE TABLE "schema-generator$airbnb"."Neighbourhood"
                                        (
                                            id character varying(25) NOT NULL,
                                            name text NOT NULL,
                                            slug text NOT NULL,
                                            featured boolean NOT NULL,
                                            popularity integer NOT NULL,
                                            "updatedAt" timestamp(3)
                                            without time zone NOT NULL,
    "createdAt" timestamp
                                            (3) without time zone NOT NULL
);
                                            --
                                            -- Name: Notification; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                            --
                                            CREATE TABLE "schema-generator$airbnb"."Notification"
                                            (
                                                id character varying(25) NOT NULL,
                                                "createdAt" timestamp(3)
                                                without time zone NOT NULL,
    type text,
    link text NOT NULL,
    "readDate" timestamp
                                                (3) without time zone NOT NULL,
    "updatedAt" timestamp
                                                (3) without time zone NOT NULL
);
                                                --
                                                -- Name: Payment; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                --
                                                CREATE TABLE "schema-generator$airbnb"."Payment"
                                                (
                                                    id character varying(25) NOT NULL,
                                                    "createdAt" timestamp(3)
                                                    without time zone NOT NULL,
    "serviceFee" numeric
                                                    (65,30) NOT NULL,
    "placePrice" numeric
                                                    (65,30) NOT NULL,
    "totalPrice" numeric
                                                    (65,30) NOT NULL,
    "updatedAt" timestamp
                                                    (3) without time zone NOT NULL
);
                                                    --
                                                    -- Name: PaymentAccount; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                    --
                                                    CREATE TABLE "schema-generator$airbnb"."PaymentAccount"
                                                    (
                                                        id character varying(25) NOT NULL,
                                                        "createdAt" timestamp(3)
                                                        without time zone NOT NULL,
    type text,
    "updatedAt" timestamp
                                                        (3) without time zone NOT NULL
);
                                                        --
                                                        -- Name: PaypalInformation; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                        --
                                                        CREATE TABLE "schema-generator$airbnb"."PaypalInformation"
                                                        (
                                                            id character varying(25) NOT NULL,
                                                            "createdAt" timestamp(3)
                                                            without time zone NOT NULL,
    email text NOT NULL,
    "updatedAt" timestamp
                                                            (3) without time zone NOT NULL
);
                                                            --
                                                            -- Name: Picture; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                            --
                                                            CREATE TABLE "schema-generator$airbnb"."Picture"
                                                            (
                                                                id character varying(25) NOT NULL,
                                                                url text NOT NULL,
                                                                "updatedAt" timestamp(3)
                                                                without time zone NOT NULL,
    "createdAt" timestamp
                                                                (3) without time zone NOT NULL
);
                                                                --
                                                                -- Name: Place; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                --
                                                                CREATE TABLE "schema-generator$airbnb"."Place"
                                                                (
                                                                    id character varying(25) NOT NULL,
                                                                    name text,
                                                                    size text,
                                                                    "shortDescription" text NOT NULL,
                                                                    description text NOT NULL,
                                                                    slug text NOT NULL,
                                                                    "maxGuests" integer NOT NULL,
                                                                    "numBedrooms" integer NOT NULL,
                                                                    "numBeds" integer NOT NULL,
                                                                    "numBaths" integer NOT NULL,
                                                                    popularity integer NOT NULL,
                                                                    "updatedAt" timestamp(3)
                                                                    without time zone NOT NULL,
    "createdAt" timestamp
                                                                    (3) without time zone NOT NULL
);
                                                                    --
                                                                    -- Name: Policies; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                    --
                                                                    CREATE TABLE "schema-generator$airbnb"."Policies"
                                                                    (
                                                                        id character varying(25) NOT NULL,
                                                                        "createdAt" timestamp(3)
                                                                        without time zone NOT NULL,
    "updatedAt" timestamp
                                                                        (3) without time zone NOT NULL,
    "checkInStartTime" numeric
                                                                        (65,30) NOT NULL,
    "checkInEndTime" numeric
                                                                        (65,30) NOT NULL,
    "checkoutTime" numeric
                                                                        (65,30) NOT NULL
);
                                                                        --
                                                                        -- Name: Pricing; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                        --
                                                                        CREATE TABLE "schema-generator$airbnb"."Pricing"
                                                                        (
                                                                            id character varying(25) NOT NULL,
                                                                            "monthlyDiscount" integer,
                                                                            "weeklyDiscount" integer,
                                                                            "perNight" integer NOT NULL,
                                                                            "smartPricing" boolean NOT NULL,
                                                                            "basePrice" integer NOT NULL,
                                                                            "averageWeekly" integer NOT NULL,
                                                                            "averageMonthly" integer NOT NULL,
                                                                            "cleaningFee" integer,
                                                                            "securityDeposit" integer,
                                                                            "extraGuests" integer,
                                                                            "weekendPricing" integer,
                                                                            currency text,
                                                                            "updatedAt" timestamp(3)
                                                                            without time zone NOT NULL,
    "createdAt" timestamp
                                                                            (3) without time zone NOT NULL
);
                                                                            --
                                                                            -- Name: Restaurant; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                            --
                                                                            CREATE TABLE "schema-generator$airbnb"."Restaurant"
                                                                            (
                                                                                id character varying(25) NOT NULL,
                                                                                "createdAt" timestamp(3)
                                                                                without time zone NOT NULL,
    title text NOT NULL,
    "avgPricePerPerson" integer NOT NULL,
    "isCurated" boolean NOT NULL,
    slug text NOT NULL,
    popularity integer NOT NULL,
    "updatedAt" timestamp
                                                                                (3) without time zone NOT NULL
);
                                                                                --
                                                                                -- Name: Review; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                --
                                                                                CREATE TABLE "schema-generator$airbnb"."Review"
                                                                                (
                                                                                    id character varying(25) NOT NULL,
                                                                                    "createdAt" timestamp(3)
                                                                                    without time zone NOT NULL,
    text text NOT NULL,
    stars integer NOT NULL,
    accuracy integer NOT NULL,
    location integer NOT NULL,
    "checkIn" integer NOT NULL,
    value integer NOT NULL,
    cleanliness integer NOT NULL,
    communication integer NOT NULL,
    "updatedAt" timestamp
                                                                                    (3) without time zone NOT NULL
);
                                                                                    --
                                                                                    -- Name: User; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                    --
                                                                                    CREATE TABLE "schema-generator$airbnb"."User"
                                                                                    (
                                                                                        id character varying(25) NOT NULL,
                                                                                        "createdAt" timestamp(3)
                                                                                        without time zone NOT NULL,
    "updatedAt" timestamp
                                                                                        (3) without time zone NOT NULL,
    "firstName" text NOT NULL,
    "lastName" text NOT NULL,
    email text NOT NULL,
    password text NOT NULL,
    phone text NOT NULL,
    "responseRate" numeric
                                                                                        (65,30),
    "responseTime" integer,
    "isSuperHost" boolean NOT NULL
);
                                                                                        --
                                                                                        -- Name: Views; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                        --
                                                                                        CREATE TABLE "schema-generator$airbnb"."Views"
                                                                                        (
                                                                                            id character varying(25) NOT NULL,
                                                                                            "lastWeek" integer NOT NULL,
                                                                                            "updatedAt" timestamp(3)
                                                                                            without time zone NOT NULL,
    "createdAt" timestamp
                                                                                            (3) without time zone NOT NULL
);
                                                                                            --
                                                                                            -- Name: _AmenitiesToPlace; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_AmenitiesToPlace"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _BookingToPayment; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_BookingToPayment"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _BookingToPlace; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_BookingToPlace"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _BookingToUser; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_BookingToUser"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _CityToNeighbourhood; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_CityToNeighbourhood"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _CreditCardInformationToPaymentAccount; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_CreditCardInformationToPaymentAccount"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _ExperienceToExperienceCategory; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_ExperienceToExperienceCategory"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _ExperienceToLocation; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_ExperienceToLocation"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _ExperienceToPicture; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_ExperienceToPicture"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _ExperienceToReview; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_ExperienceToReview"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _ExperienceToUser; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_ExperienceToUser"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _GuestRequirementsToPlace; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_GuestRequirementsToPlace"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _HouseRulesToPlace; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_HouseRulesToPlace"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _LocationToNeighbourhood; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_LocationToNeighbourhood"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _LocationToPlace; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_LocationToPlace"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _LocationToRestaurant; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_LocationToRestaurant"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _LocationToUser; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_LocationToUser"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _NeighbourhoodToPicture; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_NeighbourhoodToPicture"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _NotificationToUser; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_NotificationToUser"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PaymentAccountToPaypalInformation; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PaymentAccountToPaypalInformation"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PaymentAccountToUser; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PaymentAccountToUser"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PaymentToPaymentAccount; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PaymentToPaymentAccount"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PictureToPlace; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PictureToPlace"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PictureToRestaurant; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PictureToRestaurant"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PictureToUser; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PictureToUser"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PlaceToPolicies; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PlaceToPolicies"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PlaceToPricing; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PlaceToPricing"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PlaceToReview; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PlaceToReview"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PlaceToUser; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PlaceToUser"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _PlaceToViews; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_PlaceToViews"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _ReceivedMessages; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_ReceivedMessages"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _RelayId; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_RelayId"
                                                                                            (
                                                                                                id character varying(36) NOT NULL,
                                                                                                "stableModelIdentifier" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: _SentMessages; Type: TABLE; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE TABLE "schema-generator$airbnb"."_SentMessages"
                                                                                            (
                                                                                                id character(25) NOT NULL,
                                                                                                "A" character varying(25) NOT NULL,
                                                                                                "B" character varying(25) NOT NULL
                                                                                            );
                                                                                            --
                                                                                            -- Name: Amenities Amenities_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Amenities"
                                                                                            ADD CONSTRAINT "Amenities_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Booking Booking_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Booking"
                                                                                            ADD CONSTRAINT "Booking_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: City City_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."City"
                                                                                            ADD CONSTRAINT "City_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: CreditCardInformation CreditCardInformation_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."CreditCardInformation"
                                                                                            ADD CONSTRAINT "CreditCardInformation_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: ExperienceCategory ExperienceCategory_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."ExperienceCategory"
                                                                                            ADD CONSTRAINT "ExperienceCategory_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Experience Experience_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Experience"
                                                                                            ADD CONSTRAINT "Experience_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: GuestRequirements GuestRequirements_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."GuestRequirements"
                                                                                            ADD CONSTRAINT "GuestRequirements_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: HouseRules HouseRules_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."HouseRules"
                                                                                            ADD CONSTRAINT "HouseRules_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Location Location_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Location"
                                                                                            ADD CONSTRAINT "Location_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Message Message_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Message"
                                                                                            ADD CONSTRAINT "Message_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Neighbourhood Neighbourhood_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Neighbourhood"
                                                                                            ADD CONSTRAINT "Neighbourhood_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Notification Notification_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Notification"
                                                                                            ADD CONSTRAINT "Notification_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: PaymentAccount PaymentAccount_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."PaymentAccount"
                                                                                            ADD CONSTRAINT "PaymentAccount_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Payment Payment_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Payment"
                                                                                            ADD CONSTRAINT "Payment_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: PaypalInformation PaypalInformation_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."PaypalInformation"
                                                                                            ADD CONSTRAINT "PaypalInformation_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Picture Picture_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Picture"
                                                                                            ADD CONSTRAINT "Picture_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Place Place_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Place"
                                                                                            ADD CONSTRAINT "Place_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Policies Policies_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Policies"
                                                                                            ADD CONSTRAINT "Policies_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Pricing Pricing_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Pricing"
                                                                                            ADD CONSTRAINT "Pricing_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Restaurant Restaurant_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Restaurant"
                                                                                            ADD CONSTRAINT "Restaurant_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Review Review_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Review"
                                                                                            ADD CONSTRAINT "Review_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: User User_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."User"
                                                                                            ADD CONSTRAINT "User_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: Views Views_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."Views"
                                                                                            ADD CONSTRAINT "Views_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _AmenitiesToPlace _AmenitiesToPlace_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_AmenitiesToPlace"
                                                                                            ADD CONSTRAINT "_AmenitiesToPlace_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _BookingToPayment _BookingToPayment_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToPayment"
                                                                                            ADD CONSTRAINT "_BookingToPayment_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _BookingToPlace _BookingToPlace_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToPlace"
                                                                                            ADD CONSTRAINT "_BookingToPlace_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _BookingToUser _BookingToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToUser"
                                                                                            ADD CONSTRAINT "_BookingToUser_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _CityToNeighbourhood _CityToNeighbourhood_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_CityToNeighbourhood"
                                                                                            ADD CONSTRAINT "_CityToNeighbourhood_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _CreditCardInformationToPaymentAccount _CreditCardInformationToPaymentAccount_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_CreditCardInformationToPaymentAccount"
                                                                                            ADD CONSTRAINT "_CreditCardInformationToPaymentAccount_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _ExperienceToExperienceCategory _ExperienceToExperienceCategory_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToExperienceCategory"
                                                                                            ADD CONSTRAINT "_ExperienceToExperienceCategory_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _ExperienceToLocation _ExperienceToLocation_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToLocation"
                                                                                            ADD CONSTRAINT "_ExperienceToLocation_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _ExperienceToPicture _ExperienceToPicture_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToPicture"
                                                                                            ADD CONSTRAINT "_ExperienceToPicture_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _ExperienceToReview _ExperienceToReview_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToReview"
                                                                                            ADD CONSTRAINT "_ExperienceToReview_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _ExperienceToUser _ExperienceToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToUser"
                                                                                            ADD CONSTRAINT "_ExperienceToUser_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _GuestRequirementsToPlace _GuestRequirementsToPlace_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_GuestRequirementsToPlace"
                                                                                            ADD CONSTRAINT "_GuestRequirementsToPlace_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _HouseRulesToPlace _HouseRulesToPlace_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_HouseRulesToPlace"
                                                                                            ADD CONSTRAINT "_HouseRulesToPlace_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _LocationToNeighbourhood _LocationToNeighbourhood_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToNeighbourhood"
                                                                                            ADD CONSTRAINT "_LocationToNeighbourhood_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _LocationToPlace _LocationToPlace_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToPlace"
                                                                                            ADD CONSTRAINT "_LocationToPlace_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _LocationToRestaurant _LocationToRestaurant_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToRestaurant"
                                                                                            ADD CONSTRAINT "_LocationToRestaurant_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _LocationToUser _LocationToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToUser"
                                                                                            ADD CONSTRAINT "_LocationToUser_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _NeighbourhoodToPicture _NeighbourhoodToPicture_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_NeighbourhoodToPicture"
                                                                                            ADD CONSTRAINT "_NeighbourhoodToPicture_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _NotificationToUser _NotificationToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_NotificationToUser"
                                                                                            ADD CONSTRAINT "_NotificationToUser_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PaymentAccountToPaypalInformation _PaymentAccountToPaypalInformation_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentAccountToPaypalInformation"
                                                                                            ADD CONSTRAINT "_PaymentAccountToPaypalInformation_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PaymentAccountToUser _PaymentAccountToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentAccountToUser"
                                                                                            ADD CONSTRAINT "_PaymentAccountToUser_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PaymentToPaymentAccount _PaymentToPaymentAccount_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentToPaymentAccount"
                                                                                            ADD CONSTRAINT "_PaymentToPaymentAccount_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PictureToPlace _PictureToPlace_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToPlace"
                                                                                            ADD CONSTRAINT "_PictureToPlace_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PictureToRestaurant _PictureToRestaurant_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToRestaurant"
                                                                                            ADD CONSTRAINT "_PictureToRestaurant_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PictureToUser _PictureToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToUser"
                                                                                            ADD CONSTRAINT "_PictureToUser_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PlaceToPolicies _PlaceToPolicies_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToPolicies"
                                                                                            ADD CONSTRAINT "_PlaceToPolicies_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PlaceToPricing _PlaceToPricing_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToPricing"
                                                                                            ADD CONSTRAINT "_PlaceToPricing_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PlaceToReview _PlaceToReview_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToReview"
                                                                                            ADD CONSTRAINT "_PlaceToReview_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PlaceToUser _PlaceToUser_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToUser"
                                                                                            ADD CONSTRAINT "_PlaceToUser_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _PlaceToViews _PlaceToViews_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToViews"
                                                                                            ADD CONSTRAINT "_PlaceToViews_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _ReceivedMessages _ReceivedMessages_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ReceivedMessages"
                                                                                            ADD CONSTRAINT "_ReceivedMessages_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _SentMessages _SentMessages_pkey; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_SentMessages"
                                                                                            ADD CONSTRAINT "_SentMessages_pkey" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _RelayId pk_RelayId; Type: CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_RelayId"
                                                                                            ADD CONSTRAINT "pk_RelayId" PRIMARY KEY
                                                                                            (id);
                                                                                            --
                                                                                            -- Name: _AmenitiesToPlace_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_AmenitiesToPlace_A" ON "schema-generator$airbnb"."_AmenitiesToPlace" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _AmenitiesToPlace_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_AmenitiesToPlace_AB_unique" ON "schema-generator$airbnb"."_AmenitiesToPlace" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _AmenitiesToPlace_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_AmenitiesToPlace_B" ON "schema-generator$airbnb"."_AmenitiesToPlace" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _BookingToPayment_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_BookingToPayment_A" ON "schema-generator$airbnb"."_BookingToPayment" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _BookingToPayment_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_BookingToPayment_AB_unique" ON "schema-generator$airbnb"."_BookingToPayment" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _BookingToPayment_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_BookingToPayment_B" ON "schema-generator$airbnb"."_BookingToPayment" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _BookingToPlace_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_BookingToPlace_A" ON "schema-generator$airbnb"."_BookingToPlace" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _BookingToPlace_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_BookingToPlace_AB_unique" ON "schema-generator$airbnb"."_BookingToPlace" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _BookingToPlace_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_BookingToPlace_B" ON "schema-generator$airbnb"."_BookingToPlace" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _BookingToUser_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_BookingToUser_A" ON "schema-generator$airbnb"."_BookingToUser" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _BookingToUser_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_BookingToUser_AB_unique" ON "schema-generator$airbnb"."_BookingToUser" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _BookingToUser_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_BookingToUser_B" ON "schema-generator$airbnb"."_BookingToUser" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _CityToNeighbourhood_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_CityToNeighbourhood_A" ON "schema-generator$airbnb"."_CityToNeighbourhood" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _CityToNeighbourhood_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_CityToNeighbourhood_AB_unique" ON "schema-generator$airbnb"."_CityToNeighbourhood" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _CityToNeighbourhood_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_CityToNeighbourhood_B" ON "schema-generator$airbnb"."_CityToNeighbourhood" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _CreditCardInformationToPaymentAccount_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_CreditCardInformationToPaymentAccount_A" ON "schema-generator$airbnb"."_CreditCardInformationToPaymentAccount" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _CreditCardInformationToPaymentAccount_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_CreditCardInformationToPaymentAccount_AB_unique" ON "schema-generator$airbnb"."_CreditCardInformationToPaymentAccount" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _CreditCardInformationToPaymentAccount_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_CreditCardInformationToPaymentAccount_B" ON "schema-generator$airbnb"."_CreditCardInformationToPaymentAccount" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _ExperienceToExperienceCategory_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToExperienceCategory_A" ON "schema-generator$airbnb"."_ExperienceToExperienceCategory" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _ExperienceToExperienceCategory_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_ExperienceToExperienceCategory_AB_unique" ON "schema-generator$airbnb"."_ExperienceToExperienceCategory" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _ExperienceToExperienceCategory_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToExperienceCategory_B" ON "schema-generator$airbnb"."_ExperienceToExperienceCategory" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _ExperienceToLocation_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToLocation_A" ON "schema-generator$airbnb"."_ExperienceToLocation" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _ExperienceToLocation_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_ExperienceToLocation_AB_unique" ON "schema-generator$airbnb"."_ExperienceToLocation" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _ExperienceToLocation_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToLocation_B" ON "schema-generator$airbnb"."_ExperienceToLocation" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _ExperienceToPicture_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToPicture_A" ON "schema-generator$airbnb"."_ExperienceToPicture" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _ExperienceToPicture_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_ExperienceToPicture_AB_unique" ON "schema-generator$airbnb"."_ExperienceToPicture" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _ExperienceToPicture_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToPicture_B" ON "schema-generator$airbnb"."_ExperienceToPicture" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _ExperienceToReview_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToReview_A" ON "schema-generator$airbnb"."_ExperienceToReview" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _ExperienceToReview_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_ExperienceToReview_AB_unique" ON "schema-generator$airbnb"."_ExperienceToReview" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _ExperienceToReview_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToReview_B" ON "schema-generator$airbnb"."_ExperienceToReview" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _ExperienceToUser_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToUser_A" ON "schema-generator$airbnb"."_ExperienceToUser" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _ExperienceToUser_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_ExperienceToUser_AB_unique" ON "schema-generator$airbnb"."_ExperienceToUser" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _ExperienceToUser_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ExperienceToUser_B" ON "schema-generator$airbnb"."_ExperienceToUser" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _GuestRequirementsToPlace_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_GuestRequirementsToPlace_A" ON "schema-generator$airbnb"."_GuestRequirementsToPlace" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _GuestRequirementsToPlace_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_GuestRequirementsToPlace_AB_unique" ON "schema-generator$airbnb"."_GuestRequirementsToPlace" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _GuestRequirementsToPlace_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_GuestRequirementsToPlace_B" ON "schema-generator$airbnb"."_GuestRequirementsToPlace" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _HouseRulesToPlace_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_HouseRulesToPlace_A" ON "schema-generator$airbnb"."_HouseRulesToPlace" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _HouseRulesToPlace_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_HouseRulesToPlace_AB_unique" ON "schema-generator$airbnb"."_HouseRulesToPlace" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _HouseRulesToPlace_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_HouseRulesToPlace_B" ON "schema-generator$airbnb"."_HouseRulesToPlace" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _LocationToNeighbourhood_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToNeighbourhood_A" ON "schema-generator$airbnb"."_LocationToNeighbourhood" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _LocationToNeighbourhood_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_LocationToNeighbourhood_AB_unique" ON "schema-generator$airbnb"."_LocationToNeighbourhood" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _LocationToNeighbourhood_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToNeighbourhood_B" ON "schema-generator$airbnb"."_LocationToNeighbourhood" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _LocationToPlace_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToPlace_A" ON "schema-generator$airbnb"."_LocationToPlace" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _LocationToPlace_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_LocationToPlace_AB_unique" ON "schema-generator$airbnb"."_LocationToPlace" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _LocationToPlace_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToPlace_B" ON "schema-generator$airbnb"."_LocationToPlace" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _LocationToRestaurant_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToRestaurant_A" ON "schema-generator$airbnb"."_LocationToRestaurant" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _LocationToRestaurant_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_LocationToRestaurant_AB_unique" ON "schema-generator$airbnb"."_LocationToRestaurant" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _LocationToRestaurant_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToRestaurant_B" ON "schema-generator$airbnb"."_LocationToRestaurant" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _LocationToUser_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToUser_A" ON "schema-generator$airbnb"."_LocationToUser" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _LocationToUser_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_LocationToUser_AB_unique" ON "schema-generator$airbnb"."_LocationToUser" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _LocationToUser_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_LocationToUser_B" ON "schema-generator$airbnb"."_LocationToUser" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _NeighbourhoodToPicture_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_NeighbourhoodToPicture_A" ON "schema-generator$airbnb"."_NeighbourhoodToPicture" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _NeighbourhoodToPicture_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_NeighbourhoodToPicture_AB_unique" ON "schema-generator$airbnb"."_NeighbourhoodToPicture" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _NeighbourhoodToPicture_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_NeighbourhoodToPicture_B" ON "schema-generator$airbnb"."_NeighbourhoodToPicture" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _NotificationToUser_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_NotificationToUser_A" ON "schema-generator$airbnb"."_NotificationToUser" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _NotificationToUser_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_NotificationToUser_AB_unique" ON "schema-generator$airbnb"."_NotificationToUser" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _NotificationToUser_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_NotificationToUser_B" ON "schema-generator$airbnb"."_NotificationToUser" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PaymentAccountToPaypalInformation_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PaymentAccountToPaypalInformation_A" ON "schema-generator$airbnb"."_PaymentAccountToPaypalInformation" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PaymentAccountToPaypalInformation_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PaymentAccountToPaypalInformation_AB_unique" ON "schema-generator$airbnb"."_PaymentAccountToPaypalInformation" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PaymentAccountToPaypalInformation_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PaymentAccountToPaypalInformation_B" ON "schema-generator$airbnb"."_PaymentAccountToPaypalInformation" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PaymentAccountToUser_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PaymentAccountToUser_A" ON "schema-generator$airbnb"."_PaymentAccountToUser" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PaymentAccountToUser_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PaymentAccountToUser_AB_unique" ON "schema-generator$airbnb"."_PaymentAccountToUser" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PaymentAccountToUser_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PaymentAccountToUser_B" ON "schema-generator$airbnb"."_PaymentAccountToUser" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PaymentToPaymentAccount_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PaymentToPaymentAccount_A" ON "schema-generator$airbnb"."_PaymentToPaymentAccount" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PaymentToPaymentAccount_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PaymentToPaymentAccount_AB_unique" ON "schema-generator$airbnb"."_PaymentToPaymentAccount" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PaymentToPaymentAccount_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PaymentToPaymentAccount_B" ON "schema-generator$airbnb"."_PaymentToPaymentAccount" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PictureToPlace_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PictureToPlace_A" ON "schema-generator$airbnb"."_PictureToPlace" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PictureToPlace_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PictureToPlace_AB_unique" ON "schema-generator$airbnb"."_PictureToPlace" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PictureToPlace_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PictureToPlace_B" ON "schema-generator$airbnb"."_PictureToPlace" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PictureToRestaurant_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PictureToRestaurant_A" ON "schema-generator$airbnb"."_PictureToRestaurant" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PictureToRestaurant_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PictureToRestaurant_AB_unique" ON "schema-generator$airbnb"."_PictureToRestaurant" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PictureToRestaurant_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PictureToRestaurant_B" ON "schema-generator$airbnb"."_PictureToRestaurant" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PictureToUser_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PictureToUser_A" ON "schema-generator$airbnb"."_PictureToUser" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PictureToUser_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PictureToUser_AB_unique" ON "schema-generator$airbnb"."_PictureToUser" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PictureToUser_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PictureToUser_B" ON "schema-generator$airbnb"."_PictureToUser" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PlaceToPolicies_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToPolicies_A" ON "schema-generator$airbnb"."_PlaceToPolicies" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PlaceToPolicies_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PlaceToPolicies_AB_unique" ON "schema-generator$airbnb"."_PlaceToPolicies" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PlaceToPolicies_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToPolicies_B" ON "schema-generator$airbnb"."_PlaceToPolicies" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PlaceToPricing_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToPricing_A" ON "schema-generator$airbnb"."_PlaceToPricing" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PlaceToPricing_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PlaceToPricing_AB_unique" ON "schema-generator$airbnb"."_PlaceToPricing" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PlaceToPricing_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToPricing_B" ON "schema-generator$airbnb"."_PlaceToPricing" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PlaceToReview_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToReview_A" ON "schema-generator$airbnb"."_PlaceToReview" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PlaceToReview_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PlaceToReview_AB_unique" ON "schema-generator$airbnb"."_PlaceToReview" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PlaceToReview_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToReview_B" ON "schema-generator$airbnb"."_PlaceToReview" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PlaceToUser_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToUser_A" ON "schema-generator$airbnb"."_PlaceToUser" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PlaceToUser_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PlaceToUser_AB_unique" ON "schema-generator$airbnb"."_PlaceToUser" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PlaceToUser_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToUser_B" ON "schema-generator$airbnb"."_PlaceToUser" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _PlaceToViews_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToViews_A" ON "schema-generator$airbnb"."_PlaceToViews" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _PlaceToViews_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_PlaceToViews_AB_unique" ON "schema-generator$airbnb"."_PlaceToViews" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _PlaceToViews_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_PlaceToViews_B" ON "schema-generator$airbnb"."_PlaceToViews" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _ReceivedMessages_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ReceivedMessages_A" ON "schema-generator$airbnb"."_ReceivedMessages" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _ReceivedMessages_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_ReceivedMessages_AB_unique" ON "schema-generator$airbnb"."_ReceivedMessages" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _ReceivedMessages_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_ReceivedMessages_B" ON "schema-generator$airbnb"."_ReceivedMessages" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: _SentMessages_A; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_SentMessages_A" ON "schema-generator$airbnb"."_SentMessages" USING btree
                                                                                            ("A");
                                                                                            --
                                                                                            -- Name: _SentMessages_AB_unique; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "_SentMessages_AB_unique" ON "schema-generator$airbnb"."_SentMessages" USING btree
                                                                                            ("A", "B");
                                                                                            --
                                                                                            -- Name: _SentMessages_B; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE INDEX "_SentMessages_B" ON "schema-generator$airbnb"."_SentMessages" USING btree
                                                                                            ("B");
                                                                                            --
                                                                                            -- Name: schema-generator$airbnb.User.email._UNIQUE; Type: INDEX; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            CREATE UNIQUE INDEX "schema-generator$airbnb.User.email._UNIQUE" ON "schema-generator$airbnb"."User" USING btree
                                                                                            (email);
                                                                                            --
                                                                                            -- Name: _AmenitiesToPlace _AmenitiesToPlace_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_AmenitiesToPlace"
                                                                                            ADD CONSTRAINT "_AmenitiesToPlace_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Amenities"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _AmenitiesToPlace _AmenitiesToPlace_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_AmenitiesToPlace"
                                                                                            ADD CONSTRAINT "_AmenitiesToPlace_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _BookingToPayment _BookingToPayment_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToPayment"
                                                                                            ADD CONSTRAINT "_BookingToPayment_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Booking"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _BookingToPayment _BookingToPayment_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToPayment"
                                                                                            ADD CONSTRAINT "_BookingToPayment_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Payment"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _BookingToPlace _BookingToPlace_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToPlace"
                                                                                            ADD CONSTRAINT "_BookingToPlace_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Booking"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _BookingToPlace _BookingToPlace_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToPlace"
                                                                                            ADD CONSTRAINT "_BookingToPlace_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _BookingToUser _BookingToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToUser"
                                                                                            ADD CONSTRAINT "_BookingToUser_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Booking"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _BookingToUser _BookingToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_BookingToUser"
                                                                                            ADD CONSTRAINT "_BookingToUser_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _CityToNeighbourhood _CityToNeighbourhood_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_CityToNeighbourhood"
                                                                                            ADD CONSTRAINT "_CityToNeighbourhood_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."City"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _CityToNeighbourhood _CityToNeighbourhood_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_CityToNeighbourhood"
                                                                                            ADD CONSTRAINT "_CityToNeighbourhood_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Neighbourhood"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _CreditCardInformationToPaymentAccount _CreditCardInformationToPaymentAccount_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_CreditCardInformationToPaymentAccount"
                                                                                            ADD CONSTRAINT "_CreditCardInformationToPaymentAccount_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."CreditCardInformation"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _CreditCardInformationToPaymentAccount _CreditCardInformationToPaymentAccount_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_CreditCardInformationToPaymentAccount"
                                                                                            ADD CONSTRAINT "_CreditCardInformationToPaymentAccount_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."PaymentAccount"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToExperienceCategory _ExperienceToExperienceCategory_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToExperienceCategory"
                                                                                            ADD CONSTRAINT "_ExperienceToExperienceCategory_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Experience"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToExperienceCategory _ExperienceToExperienceCategory_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToExperienceCategory"
                                                                                            ADD CONSTRAINT "_ExperienceToExperienceCategory_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."ExperienceCategory"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToLocation _ExperienceToLocation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToLocation"
                                                                                            ADD CONSTRAINT "_ExperienceToLocation_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Experience"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToLocation _ExperienceToLocation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToLocation"
                                                                                            ADD CONSTRAINT "_ExperienceToLocation_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Location"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToPicture _ExperienceToPicture_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToPicture"
                                                                                            ADD CONSTRAINT "_ExperienceToPicture_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Experience"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToPicture _ExperienceToPicture_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToPicture"
                                                                                            ADD CONSTRAINT "_ExperienceToPicture_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Picture"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToReview _ExperienceToReview_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToReview"
                                                                                            ADD CONSTRAINT "_ExperienceToReview_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Experience"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToReview _ExperienceToReview_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToReview"
                                                                                            ADD CONSTRAINT "_ExperienceToReview_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Review"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToUser _ExperienceToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToUser"
                                                                                            ADD CONSTRAINT "_ExperienceToUser_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Experience"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ExperienceToUser _ExperienceToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ExperienceToUser"
                                                                                            ADD CONSTRAINT "_ExperienceToUser_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _GuestRequirementsToPlace _GuestRequirementsToPlace_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_GuestRequirementsToPlace"
                                                                                            ADD CONSTRAINT "_GuestRequirementsToPlace_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."GuestRequirements"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _GuestRequirementsToPlace _GuestRequirementsToPlace_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_GuestRequirementsToPlace"
                                                                                            ADD CONSTRAINT "_GuestRequirementsToPlace_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _HouseRulesToPlace _HouseRulesToPlace_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_HouseRulesToPlace"
                                                                                            ADD CONSTRAINT "_HouseRulesToPlace_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."HouseRules"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _HouseRulesToPlace _HouseRulesToPlace_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_HouseRulesToPlace"
                                                                                            ADD CONSTRAINT "_HouseRulesToPlace_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToNeighbourhood _LocationToNeighbourhood_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToNeighbourhood"
                                                                                            ADD CONSTRAINT "_LocationToNeighbourhood_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Location"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToNeighbourhood _LocationToNeighbourhood_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToNeighbourhood"
                                                                                            ADD CONSTRAINT "_LocationToNeighbourhood_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Neighbourhood"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToPlace _LocationToPlace_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToPlace"
                                                                                            ADD CONSTRAINT "_LocationToPlace_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Location"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToPlace _LocationToPlace_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToPlace"
                                                                                            ADD CONSTRAINT "_LocationToPlace_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToRestaurant _LocationToRestaurant_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToRestaurant"
                                                                                            ADD CONSTRAINT "_LocationToRestaurant_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Location"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToRestaurant _LocationToRestaurant_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToRestaurant"
                                                                                            ADD CONSTRAINT "_LocationToRestaurant_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Restaurant"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToUser _LocationToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToUser"
                                                                                            ADD CONSTRAINT "_LocationToUser_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Location"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _LocationToUser _LocationToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_LocationToUser"
                                                                                            ADD CONSTRAINT "_LocationToUser_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _NeighbourhoodToPicture _NeighbourhoodToPicture_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_NeighbourhoodToPicture"
                                                                                            ADD CONSTRAINT "_NeighbourhoodToPicture_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Neighbourhood"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _NeighbourhoodToPicture _NeighbourhoodToPicture_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_NeighbourhoodToPicture"
                                                                                            ADD CONSTRAINT "_NeighbourhoodToPicture_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Picture"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _NotificationToUser _NotificationToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_NotificationToUser"
                                                                                            ADD CONSTRAINT "_NotificationToUser_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Notification"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _NotificationToUser _NotificationToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_NotificationToUser"
                                                                                            ADD CONSTRAINT "_NotificationToUser_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PaymentAccountToPaypalInformation _PaymentAccountToPaypalInformation_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentAccountToPaypalInformation"
                                                                                            ADD CONSTRAINT "_PaymentAccountToPaypalInformation_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."PaymentAccount"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PaymentAccountToPaypalInformation _PaymentAccountToPaypalInformation_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentAccountToPaypalInformation"
                                                                                            ADD CONSTRAINT "_PaymentAccountToPaypalInformation_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."PaypalInformation"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PaymentAccountToUser _PaymentAccountToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentAccountToUser"
                                                                                            ADD CONSTRAINT "_PaymentAccountToUser_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."PaymentAccount"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PaymentAccountToUser _PaymentAccountToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentAccountToUser"
                                                                                            ADD CONSTRAINT "_PaymentAccountToUser_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PaymentToPaymentAccount _PaymentToPaymentAccount_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentToPaymentAccount"
                                                                                            ADD CONSTRAINT "_PaymentToPaymentAccount_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Payment"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PaymentToPaymentAccount _PaymentToPaymentAccount_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PaymentToPaymentAccount"
                                                                                            ADD CONSTRAINT "_PaymentToPaymentAccount_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."PaymentAccount"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PictureToPlace _PictureToPlace_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToPlace"
                                                                                            ADD CONSTRAINT "_PictureToPlace_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Picture"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PictureToPlace _PictureToPlace_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToPlace"
                                                                                            ADD CONSTRAINT "_PictureToPlace_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PictureToRestaurant _PictureToRestaurant_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToRestaurant"
                                                                                            ADD CONSTRAINT "_PictureToRestaurant_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Picture"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PictureToRestaurant _PictureToRestaurant_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToRestaurant"
                                                                                            ADD CONSTRAINT "_PictureToRestaurant_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Restaurant"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PictureToUser _PictureToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToUser"
                                                                                            ADD CONSTRAINT "_PictureToUser_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Picture"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PictureToUser _PictureToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PictureToUser"
                                                                                            ADD CONSTRAINT "_PictureToUser_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToPolicies _PlaceToPolicies_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToPolicies"
                                                                                            ADD CONSTRAINT "_PlaceToPolicies_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToPolicies _PlaceToPolicies_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToPolicies"
                                                                                            ADD CONSTRAINT "_PlaceToPolicies_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Policies"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToPricing _PlaceToPricing_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToPricing"
                                                                                            ADD CONSTRAINT "_PlaceToPricing_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToPricing _PlaceToPricing_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToPricing"
                                                                                            ADD CONSTRAINT "_PlaceToPricing_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Pricing"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToReview _PlaceToReview_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToReview"
                                                                                            ADD CONSTRAINT "_PlaceToReview_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToReview _PlaceToReview_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToReview"
                                                                                            ADD CONSTRAINT "_PlaceToReview_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Review"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToUser _PlaceToUser_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToUser"
                                                                                            ADD CONSTRAINT "_PlaceToUser_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToUser _PlaceToUser_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToUser"
                                                                                            ADD CONSTRAINT "_PlaceToUser_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToViews _PlaceToViews_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToViews"
                                                                                            ADD CONSTRAINT "_PlaceToViews_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Place"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _PlaceToViews _PlaceToViews_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_PlaceToViews"
                                                                                            ADD CONSTRAINT "_PlaceToViews_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."Views"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ReceivedMessages _ReceivedMessages_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ReceivedMessages"
                                                                                            ADD CONSTRAINT "_ReceivedMessages_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Message"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _ReceivedMessages _ReceivedMessages_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_ReceivedMessages"
                                                                                            ADD CONSTRAINT "_ReceivedMessages_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _SentMessages _SentMessages_A_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_SentMessages"
                                                                                            ADD CONSTRAINT "_SentMessages_A_fkey" FOREIGN KEY
                                                                                            ("A") REFERENCES "schema-generator$airbnb"."Message"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;
                                                                                            --
                                                                                            -- Name: _SentMessages _SentMessages_B_fkey; Type: FK CONSTRAINT; Schema: schema-generator$airbnb; Owner: -
                                                                                            --
                                                                                            ALTER TABLE ONLY "schema-generator$airbnb"."_SentMessages"
                                                                                            ADD CONSTRAINT "_SentMessages_B_fkey" FOREIGN KEY
                                                                                            ("B") REFERENCES "schema-generator$airbnb"."User"
                                                                                            (id) ON
                                                                                            DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

