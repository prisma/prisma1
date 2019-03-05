import testSchema from '../common'

describe('Introspector', () => {
  test('CMS/sequences', async () => {
    await testSchema(
      `create schema cms;
    
    create sequence cms.service_provider_performance_summary_type_id_seq
    ;
    
    create sequence cms.provider_performance_prisma_id_seq
    ;
    
    create sequence cms.service_performance_prisma_id_seq
    ;
    
    create sequence cms.service_provider_performance_prisma_id_seq
    ;
    
    create sequence cms.service_provider_performance_summary_prisma_id_seq
    ;
    
    create table cms.provider_performance
    (
        npi integer not null,
        mcare_participation_indicator text,
        place_of_service text not null,
        hcpcs_code text not null,
        n_of_svcs numeric,
        n_of_mcare_beneficiaries integer,
        n_of_distinct_mcare_beneficiary_per_day_svcs integer,
        avg_mcare_allowed_amt numeric,
        avg_submitted_charge_amt numeric,
        avg_mcare_pay_amt numeric,
        avg_mcare_standardized_amt numeric,
        prisma_id serial not null
            constraint provider_performance_prisma_id_pk
                primary key,
        constraint provider_performance_npi_hcpcs_code_place_of_service_pk
            unique (npi, hcpcs_code, place_of_service)
    )
    ;
    
    create unique index provider_performance_prisma_id_uindex
        on cms.provider_performance (prisma_id)
    ;
    
    create index provider_performance_mcare_participation_indicator_index
        on cms.provider_performance (mcare_participation_indicator)
    ;
    
    create index provider_performance_place_of_service_index
        on cms.provider_performance (place_of_service)
    ;
    
    create index provider_performance_hcpcs_code_index
        on cms.provider_performance (hcpcs_code)
    ;
    
    create index provider_performance_n_of_svcs_index
        on cms.provider_performance (n_of_svcs)
    ;
    
    create index provider_performance_n_of_mcare_beneficiaries_index
        on cms.provider_performance (n_of_mcare_beneficiaries)
    ;
    
    create index provider_performance_n_of_distinct_mcare_beneficiary_pe
        on cms.provider_performance (n_of_distinct_mcare_beneficiary_per_day_svcs)
    ;
    
    create index provider_performance_avg_mcare_allowed_amt_index
        on cms.provider_performance (avg_mcare_allowed_amt)
    ;
    
    create index provider_performance_avg_submitted_charge_amt_index
        on cms.provider_performance (avg_submitted_charge_amt)
    ;
    
    create index provider_performance_avg_mcare_pay_amt_index
        on cms.provider_performance (avg_mcare_pay_amt)
    ;
    
    create index provider_performance_avg_mcare_standardized_amt_index
        on cms.provider_performance (avg_mcare_standardized_amt)
    ;
    
    create table cms.providers
    (
        npi integer not null
            constraint providers_npi_pk
                primary key,
        entity_type text,
        provider_type text,
        address_street_01 text,
        address_street_02 text,
        address_city text,
        address_zip_code text,
        address_state text,
        address_country text,
        address_latitude numeric,
        address_longitude numeric
    )
    ;
    
    create index providers_entity_type_index
        on cms.providers (entity_type)
    ;
    
    create index providers_provider_type_index
        on cms.providers (provider_type)
    ;
    
    alter table cms.provider_performance
        add constraint provider_performance_providers_npi_fk
            foreign key (npi) references cms.providers
    ;
    
    create table cms.service_provider_performance
    (
        hcpcs_code text not null,
        npi integer not null
            constraint service_provider_performance_providers_npi_fk
                references cms.providers,
        entity_type text,
        n_of_svcs numeric,
        n_of_distinct_mcare_beneficiary_per_day_svcs integer,
        n_of_mcare_beneficiaries integer,
        avg_mcare_pay_amt numeric,
        avg_submitted_charge_amt numeric,
        avg_mcare_allowed_amt numeric,
        avg_mcare_standardized_amt numeric,
        est_ttl_mcare_pay_amt numeric,
        est_ttl_submitted_charge_amt numeric,
        rank_n_of_svcs bigint,
        rank_n_of_distinct_mcare_beneficiary_per_day_svcs bigint,
        rank_n_of_mcare_beneficiaries bigint,
        rank_avg_mcare_standardized_amt bigint,
        rank_avg_mcare_allowed_amt bigint,
        rank_avg_submitted_charge_amt bigint,
        rank_avg_mcare_pay_amt bigint,
        rank_est_ttl_mcare_pay_amt bigint,
        rank_est_ttl_submitted_charge_amt bigint,
        mcare_participation_indicator text,
        place_of_service text not null,
        var_avg_mcare_submitted_charge_pay_amt numeric,
        rank_var_avg_mcare_submitted_charge_pay_amt bigint,
        prisma_id serial not null
            constraint service_provider_performance_prisma_id_pk
                primary key,
        constraint service_provider_performance_hcpcs_code_npi_place_of_service_pk
            unique (hcpcs_code, npi, place_of_service)
    )
    ;
    
    create unique index service_provider_performance_prisma_id_uindex
        on cms.service_provider_performance (prisma_id)
    ;
    
    create index service_provider_performance_entity_type_index
        on cms.service_provider_performance (entity_type)
    ;
    
    create index service_provider_performance_rank_n_of_svcs_index
        on cms.service_provider_performance (rank_n_of_svcs)
    ;
    
    create index service_provider_performance_rank_n_of_distinct_mcare_b
        on cms.service_provider_performance (rank_n_of_distinct_mcare_beneficiary_per_day_svcs)
    ;
    
    create index service_provider_performance_rank_n_of_mcare_beneficiar
        on cms.service_provider_performance (rank_n_of_mcare_beneficiaries)
    ;
    
    create index service_provider_performance_rank_avg_mcare_standardized
        on cms.service_provider_performance (rank_avg_mcare_standardized_amt)
    ;
    
    create index service_provider_performance_rank_avg_mcare_allowed_amou
        on cms.service_provider_performance (rank_avg_mcare_allowed_amt)
    ;
    
    create index service_provider_performance_rank_avg_submitted_charge_amou
        on cms.service_provider_performance (rank_avg_submitted_charge_amt)
    ;
    
    create index service_provider_performance_rank_avg_mcare_pay_amou
        on cms.service_provider_performance (rank_avg_mcare_pay_amt)
    ;
    
    create index service_provider_performance_rank_est_ttl_mcare_pay_am
        on cms.service_provider_performance (rank_est_ttl_mcare_pay_amt)
    ;
    
    create index service_provider_performance_rank_est_ttl_submitted_charge_am
        on cms.service_provider_performance (rank_est_ttl_submitted_charge_amt)
    ;
    
    create index service_provider_performance_rank_var_avg_mcare_sub
        on cms.service_provider_performance (rank_var_avg_mcare_submitted_charge_pay_amt)
    ;
    
    create table cms.services
    (
        hcpcs_code text not null
            constraint svcs_hcpcs_code_pk
                primary key,
        hcpcs_description text,
        hcpcs_drug_indicator text
    )
    ;
    
    create index svcs_hcpcs_drug_indicator_index
        on cms.services (hcpcs_drug_indicator)
    ;
    
    alter table cms.provider_performance
        add constraint provider_performance_cms_svcs_hcpcs_code_fk
            foreign key (hcpcs_code) references cms.services
    ;
    
    alter table cms.service_provider_performance
        add constraint service_provider_performance_svcs_hcpcs_code_fk
            foreign key (hcpcs_code) references cms.services
    ;
    
    create table cms.providers_individuals
    (
        npi integer not null
            constraint providers_individuals_npi_pk
                primary key
            constraint providers_individuals_providers_npi_fk
                references cms.providers,
        name_last text,
        name_first text,
        name_middle text,
        credentials text,
        gender text
    )
    ;
    
    create index providers_individuals_gender_index
        on cms.providers_individuals (gender)
    ;
    
    create table cms.providers_organizations
    (
        npi integer not null
            constraint providers_organizations_npi_pk
                primary key
            constraint providers_organizations_providers_npi_fk
                references cms.providers,
        name text
    )
    ;
    
    create table cms.service_performance
    (
        hcpcs_code text not null
            constraint service_performance_svcs_hcpcs_code_fk
                references cms.services,
        entity_type text not null,
        providers bigint,
        n_of_svcs numeric,
        n_of_distinct_mcare_beneficiary_per_day_svcs bigint,
        n_of_mcare_beneficiaries bigint,
        avg_avg_mcare_pay_amt numeric,
        avg_avg_submitted_charge_amt numeric,
        avg_avg_mcare_allowed_amt numeric,
        avg_avg_mcare_standardized_amt numeric,
        min_avg_mcare_pay_amt numeric,
        max_avg_mcare_pay_amt numeric,
        var_avg_mcare_pay_amt numeric,
        min_avg_mcare_allowed_amt numeric,
        max_avg_mcare_allowed_amt numeric,
        var_avg_mcare_allowed_amt numeric,
        min_avg_submitted_charge_amt numeric,
        max_avg_submitted_charge_amt numeric,
        var_avg_submitted_charge_amt numeric,
        min_avg_mcare_standardized_amt numeric,
        max_avg_mcare_standardized_amt numeric,
        var_avg_mcare_standardized_amt numeric,
        est_ttl_mcare_pay_amt numeric,
        est_ttl_submitted_charge_amt numeric,
        est_ttl_mcare_allowed_amt numeric,
        est_ttl_mcare_standardized_amt numeric,
        rank_providers bigint,
        rank_n_of_svcs bigint,
        rank_n_of_distinct_mcare_beneficiary_per_day_svcs bigint,
        rank_n_of_mcare_beneficiaries bigint,
        rank_avg_avg_mcare_pay_amt bigint,
        rank_avg_avg_submitted_charge_amt bigint,
        rank_avg_avg_mcare_allowed_amt bigint,
        rank_avg_avg_mcare_standardized_amt bigint,
        rank_min_avg_mcare_pay_amt bigint,
        rank_max_avg_mcare_pay_amt bigint,
        rank_var_avg_mcare_pay_amt bigint,
        rank_min_avg_mcare_allowed_amt bigint,
        rank_max_avg_mcare_allowed_amt bigint,
        rank_var_avg_mcare_allowed_amt bigint,
        rank_min_avg_submitted_charge_amt bigint,
        rank_max_avg_submitted_charge_amt bigint,
        rank_var_avg_submitted_charge_amt bigint,
        rank_min_avg_mcare_standardized_amt bigint,
        rank_max_avg_mcare_standardized_amt bigint,
        rank_var_avg_mcare_standardized_amt bigint,
        rank_est_ttl_mcare_pay_amt bigint,
        rank_est_ttl_submitted_charge_amt bigint,
        rank_est_ttl_mcare_allowed_amt bigint,
        rank_est_ttl_mcare_standardized_amt bigint,
        var_avg_mcare_submitted_charge_pay_amt numeric,
        rank_var_avg_mcare_submitted_charge_pay_amt bigint,
        prisma_id serial not null
            constraint service_performance_prisma_id_pk
                primary key,
        constraint service_performance_hcpcs_code_entity_type_pk
            unique (hcpcs_code, entity_type)
    )
    ;
    
    create unique index service_performance_prisma_id_uindex
        on cms.service_performance (prisma_id)
    ;
    
    create index service_performance_entity_type_index
        on cms.service_performance (entity_type)
    ;
    
    create index service_performance_rank_providers_index
        on cms.service_performance (rank_providers)
    ;
    
    create index service_performance_rank_n_of_svcs_index
        on cms.service_performance (rank_n_of_svcs)
    ;
    
    create index service_performance_rank_n_of_distinct_mcare_beneficiar
        on cms.service_performance (rank_n_of_distinct_mcare_beneficiary_per_day_svcs)
    ;
    
    create index service_performance_rank_n_of_mcare_beneficiaries_index
        on cms.service_performance (rank_n_of_mcare_beneficiaries)
    ;
    
    create index service_performance_rank_avg_avg_mcare_pay_amt_in
        on cms.service_performance (rank_avg_avg_mcare_pay_amt)
    ;
    
    create index service_performance_rank_avg_avg_submitted_charge_amt_in
        on cms.service_performance (rank_avg_avg_submitted_charge_amt)
    ;
    
    create index service_performance_rank_avg_avg_mcare_allowed_amt_in
        on cms.service_performance (rank_avg_avg_mcare_allowed_amt)
    ;
    
    create index service_performance_rank_avg_avg_mcare_standardized_amou
        on cms.service_performance (rank_avg_avg_mcare_standardized_amt)
    ;
    
    create index service_performance_rank_min_avg_mcare_pay_amt_in
        on cms.service_performance (rank_min_avg_mcare_pay_amt)
    ;
    
    create index service_performance_rank_max_avg_mcare_pay_amt_in
        on cms.service_performance (rank_max_avg_mcare_pay_amt)
    ;
    
    create index service_performance_rank_var_avg_mcare_pay_amou
        on cms.service_performance (rank_var_avg_mcare_pay_amt)
    ;
    
    create index service_performance_rank_min_avg_mcare_allowed_amt_in
        on cms.service_performance (rank_min_avg_mcare_allowed_amt)
    ;
    
    create index service_performance_rank_max_avg_mcare_allowed_amt_in
        on cms.service_performance (rank_max_avg_mcare_allowed_amt)
    ;
    
    create index service_performance_rank_var_avg_mcare_allowed_amou
        on cms.service_performance (rank_var_avg_mcare_allowed_amt)
    ;
    
    create index service_performance_rank_min_avg_submitted_charge_amt_in
        on cms.service_performance (rank_min_avg_submitted_charge_amt)
    ;
    
    create index service_performance_rank_max_avg_submitted_charge_amt_in
        on cms.service_performance (rank_max_avg_submitted_charge_amt)
    ;
    
    create index service_performance_rank_var_avg_submitted_charge_amou
        on cms.service_performance (rank_var_avg_submitted_charge_amt)
    ;
    
    create index service_performance_rank_min_avg_mcare_standardized_amou
        on cms.service_performance (rank_min_avg_mcare_standardized_amt)
    ;
    
    create index service_performance_rank_max_avg_mcare_standardized_amou
        on cms.service_performance (rank_max_avg_mcare_standardized_amt)
    ;
    
    create index service_performance_rank_var_avg_mcare_standardized
        on cms.service_performance (rank_var_avg_mcare_standardized_amt)
    ;
    
    create index service_performance_rank_est_ttl_mcare_pay_amt_inde
        on cms.service_performance (rank_est_ttl_mcare_pay_amt)
    ;
    
    create index service_performance_rank_est_ttl_submitted_charge_amt_inde
        on cms.service_performance (rank_est_ttl_submitted_charge_amt)
    ;
    
    create index service_performance_rank_est_ttl_mcare_allowed_amt_inde
        on cms.service_performance (rank_est_ttl_mcare_allowed_amt)
    ;
    
    create index service_performance_rank_est_ttl_mcare_standardized_amt
        on cms.service_performance (rank_est_ttl_mcare_standardized_amt)
    ;
    
    create index service_performance_rank_var_avg_mcare_submitted_ch
        on cms.service_performance (rank_var_avg_mcare_submitted_charge_pay_amt)
    ;
    
    create table cms.service_provider_performance_summary
    (
        npi integer not null
            constraint service_provider_performance_summary_providers_npi_fk
                references cms.providers,
        entity_type text,
        ttl_hcpcs_code text,
        ttl_n_of_svcs bigint,
        est_ttl_submitted_charge_amt numeric,
        est_ttl_mcare_pay_amt numeric,
        var_est_ttl_mcare_submitted_charge_pay_amt numeric,
        est_ttl_mcare_pay_amt_by_ttl_hcpcs_code numeric,
        est_ttl_mcare_pay_amt_by_ttl_n_of_svcs numeric,
        rank_ttl_hcpcs_code bigint,
        rank_ttl_n_of_svcs bigint,
        rank_est_ttl_submitted_charge_amt bigint,
        rank_est_ttl_mcare_pay_amt bigint,
        rank_var_est_ttl_mcare_submitted_charge_pay_amoun bigint,
        rank_est_ttl_mcare_pay_amt_by_ttl_hcpcs_code bigint,
        rank_est_ttl_mcare_pay_amt_by_ttl_n_of_servi bigint,
        summary_type integer not null,
        prisma_id serial not null
            constraint service_provider_performance_summary_prisma_id_pk
                primary key,
        constraint service_provider_performance_summary_npi_summary_type_pk
            unique (npi, summary_type)
    )
    ;
    
    create unique index service_provider_performance_summary_prisma_id_uindex
        on cms.service_provider_performance_summary (prisma_id)
    ;
    
    create index service_provider_performance_summary_entity_type_index
        on cms.service_provider_performance_summary (entity_type)
    ;
    
    create index prvdr_smry_rank_ttl_hcpcs_code_index
        on cms.service_provider_performance_summary (rank_ttl_hcpcs_code)
    ;
    
    create index prvdr_smry_rank_ttl_n_of_svcs_index
        on cms.service_provider_performance_summary (rank_ttl_n_of_svcs)
    ;
    
    create index prvdr_smry_rank_est_ttl_submitted_charge_amt_index
        on cms.service_provider_performance_summary (rank_est_ttl_submitted_charge_amt)
    ;
    
    create index prvdr_smry_rank_est_ttl_mcare_pay_amt_index
        on cms.service_provider_performance_summary (rank_est_ttl_mcare_pay_amt)
    ;
    
    create index prvdr_smry_rank_var_est_ttl_mcare_submitted_charge_pa
        on cms.service_provider_performance_summary (rank_var_est_ttl_mcare_submitted_charge_pay_amoun)
    ;
    
    create index prvdr_smry_rank_est_ttl_mcare_pay_amt_by_ttl_hcpc
        on cms.service_provider_performance_summary (rank_est_ttl_mcare_pay_amt_by_ttl_hcpcs_code)
    ;
    
    create index prvdr_smry_rank_est_ttl_mcare_pay_amt_by_ttl_numb
        on cms.service_provider_performance_summary (rank_est_ttl_mcare_pay_amt_by_ttl_n_of_servi)
    ;
    
    create table cms.service_provider_performance_summary_type
    (
        id serial not null
            constraint service_provider_performance_summary_type_pkey
                primary key,
        slug varchar(20),
        description text not null,
        group_membership boolean default true not null
    )
    ;
    
    create unique index service_provider_performance_summary_type_slug_uindex
        on cms.service_provider_performance_summary_type (slug)
    ;
    
    alter table cms.service_provider_performance_summary
        add constraint service_provider_performance_summary_service_provider_performan
            foreign key (summary_type) references cms.service_provider_performance_summary_type
    ;
    
    CREATE VIEW cms.live_service_performance AS SELECT performance.hcpcs_code,
        providers.entity_type,
        count(performance.npi) AS providers,
        sum(performance.n_of_svcs) AS n_of_svcs,
        sum(performance.n_of_distinct_mcare_beneficiary_per_day_svcs) AS n_of_distinct_mcare_beneficiary_per_day_svcs,
        sum(performance.n_of_mcare_beneficiaries) AS n_of_mcare_beneficiaries,
        avg(performance.avg_mcare_pay_amt) AS avg_avg_mcare_pay_amt,
        avg(performance.avg_submitted_charge_amt) AS avg_avg_submitted_charge_amt,
        (avg(performance.avg_submitted_charge_amt) - avg(performance.avg_mcare_pay_amt)) AS var_avg_mcare_submitted_charge_pay_amt,
        avg(performance.avg_mcare_allowed_amt) AS avg_avg_mcare_allowed_amt,
        avg(performance.avg_mcare_standardized_amt) AS avg_avg_mcare_standardized_amt,
        min(performance.avg_mcare_pay_amt) AS min_avg_mcare_pay_amt,
        max(performance.avg_mcare_pay_amt) AS max_avg_mcare_pay_amt,
        (max(performance.avg_mcare_pay_amt) - min(performance.avg_mcare_pay_amt)) AS var_avg_mcare_pay_amt,
        min(performance.avg_mcare_allowed_amt) AS min_avg_mcare_allowed_amt,
        max(performance.avg_mcare_allowed_amt) AS max_avg_mcare_allowed_amt,
        (max(performance.avg_mcare_allowed_amt) - min(performance.avg_mcare_allowed_amt)) AS var_avg_mcare_allowed_amt,
        min(performance.avg_submitted_charge_amt) AS min_avg_submitted_charge_amt,
        max(performance.avg_submitted_charge_amt) AS max_avg_submitted_charge_amt,
        (max(performance.avg_submitted_charge_amt) - min(performance.avg_submitted_charge_amt)) AS var_avg_submitted_charge_amt,
        min(performance.avg_mcare_standardized_amt) AS min_avg_mcare_standardized_amt,
        max(performance.avg_mcare_standardized_amt) AS max_avg_mcare_standardized_amt,
        (max(performance.avg_mcare_standardized_amt) - min(performance.avg_mcare_standardized_amt)) AS var_avg_mcare_standardized_amt,
        (avg(performance.avg_mcare_pay_amt) * sum(performance.n_of_svcs)) AS est_ttl_mcare_pay_amt,
        (avg(performance.avg_submitted_charge_amt) * sum(performance.n_of_svcs)) AS est_ttl_submitted_charge_amt,
        (avg(performance.avg_mcare_allowed_amt) * sum(performance.n_of_svcs)) AS est_ttl_mcare_allowed_amt,
        (avg(performance.avg_mcare_standardized_amt) * sum(performance.n_of_svcs)) AS est_ttl_mcare_standardized_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (count(performance.npi)) DESC) AS rank_providers,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (sum(performance.n_of_svcs)) DESC) AS rank_n_of_svcs,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (sum(performance.n_of_distinct_mcare_beneficiary_per_day_svcs)) DESC) AS rank_n_of_distinct_mcare_beneficiary_per_day_svcs,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (sum(performance.n_of_mcare_beneficiaries)) DESC) AS rank_n_of_mcare_beneficiaries,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_mcare_pay_amt)) DESC) AS rank_avg_avg_mcare_pay_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_submitted_charge_amt)) DESC) AS rank_avg_avg_submitted_charge_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_submitted_charge_amt) - avg(performance.avg_mcare_pay_amt)) DESC) AS rank_var_avg_mcare_submitted_charge_pay_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_mcare_allowed_amt)) DESC) AS rank_avg_avg_mcare_allowed_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_mcare_standardized_amt)) DESC) AS rank_avg_avg_mcare_standardized_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (min(performance.avg_mcare_pay_amt)) DESC) AS rank_min_avg_mcare_pay_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_mcare_pay_amt)) DESC) AS rank_max_avg_mcare_pay_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_mcare_pay_amt) - min(performance.avg_mcare_pay_amt)) DESC) AS rank_var_avg_mcare_pay_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (min(performance.avg_mcare_allowed_amt)) DESC) AS rank_min_avg_mcare_allowed_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_mcare_allowed_amt)) DESC) AS rank_max_avg_mcare_allowed_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_mcare_allowed_amt) - min(performance.avg_mcare_allowed_amt)) DESC) AS rank_var_avg_mcare_allowed_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (min(performance.avg_submitted_charge_amt)) DESC) AS rank_min_avg_submitted_charge_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_submitted_charge_amt)) DESC) AS rank_max_avg_submitted_charge_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_submitted_charge_amt) - min(performance.avg_submitted_charge_amt)) DESC) AS rank_var_avg_submitted_charge_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (min(performance.avg_mcare_standardized_amt)) DESC) AS rank_min_avg_mcare_standardized_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_mcare_standardized_amt)) DESC) AS rank_max_avg_mcare_standardized_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (max(performance.avg_mcare_standardized_amt) - min(performance.avg_mcare_standardized_amt)) DESC) AS rank_var_avg_mcare_standardized_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_mcare_pay_amt) * sum(performance.n_of_svcs)) DESC) AS rank_est_ttl_mcare_pay_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_submitted_charge_amt) * sum(performance.n_of_svcs)) DESC) AS rank_est_ttl_submitted_charge_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_mcare_allowed_amt) * sum(performance.n_of_svcs)) DESC) AS rank_est_ttl_mcare_allowed_amt,
        rank() OVER (PARTITION BY providers.entity_type ORDER BY (avg(performance.avg_mcare_standardized_amt) * sum(performance.n_of_svcs)) DESC) AS rank_est_ttl_mcare_standardized_amt
       FROM (cms.provider_performance performance
         JOIN cms.providers providers ON ((providers.npi = performance.npi)))
      GROUP BY performance.hcpcs_code, providers.entity_type
    ;
    
    CREATE VIEW cms.live_service_provider_performance AS SELECT performance.hcpcs_code,
        performance.npi,
        providers.entity_type,
        performance.mcare_participation_indicator,
        performance.place_of_service,
        performance.n_of_svcs,
        performance.n_of_distinct_mcare_beneficiary_per_day_svcs,
        performance.n_of_mcare_beneficiaries,
        performance.avg_mcare_pay_amt,
        performance.avg_submitted_charge_amt,
        (performance.avg_submitted_charge_amt - performance.avg_mcare_pay_amt) AS var_avg_mcare_submitted_charge_pay_amt,
        performance.avg_mcare_allowed_amt,
        performance.avg_mcare_standardized_amt,
        (performance.avg_mcare_pay_amt * performance.n_of_svcs) AS est_ttl_mcare_pay_amt,
        (performance.avg_submitted_charge_amt * performance.n_of_svcs) AS est_ttl_submitted_charge_amt,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, performance.n_of_svcs DESC) AS rank_n_of_svcs,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, performance.n_of_distinct_mcare_beneficiary_per_day_svcs DESC) AS rank_n_of_distinct_mcare_beneficiary_per_day_svcs,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, performance.n_of_mcare_beneficiaries DESC) AS rank_n_of_mcare_beneficiaries,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, performance.avg_mcare_standardized_amt DESC) AS rank_avg_mcare_standardized_amt,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, (performance.avg_submitted_charge_amt - performance.avg_mcare_pay_amt) DESC) AS rank_var_avg_mcare_submitted_charge_pay_amt,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, performance.avg_mcare_allowed_amt DESC) AS rank_avg_mcare_allowed_amt,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, performance.avg_submitted_charge_amt DESC) AS rank_avg_submitted_charge_amt,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, performance.avg_mcare_pay_amt DESC) AS rank_avg_mcare_pay_amt,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, (performance.avg_mcare_pay_amt * performance.n_of_svcs) DESC) AS rank_est_ttl_mcare_pay_amt,
        rank() OVER (PARTITION BY performance.hcpcs_code, providers.entity_type ORDER BY performance.hcpcs_code, (performance.avg_submitted_charge_amt * performance.n_of_svcs) DESC) AS rank_est_ttl_submitted_charge_amt
       FROM (cms.provider_performance performance
         JOIN cms.providers providers ON ((providers.npi = performance.npi)))
      GROUP BY performance.hcpcs_code, performance.npi, providers.entity_type, performance.mcare_participation_indicator, performance.place_of_service, performance.n_of_svcs, performance.n_of_distinct_mcare_beneficiary_per_day_svcs, performance.n_of_mcare_beneficiaries, performance.avg_mcare_pay_amt, performance.avg_submitted_charge_amt, performance.avg_mcare_allowed_amt, performance.avg_mcare_standardized_amt
      ORDER BY performance.hcpcs_code, providers.entity_type, performance.n_of_svcs DESC
    ;
    
    CREATE VIEW cms.live_service_provider_performance_summary_drug_no AS SELECT 3 AS summary_type,
        performance.npi,
        performance.entity_type,
        count(performance.hcpcs_code) AS ttl_hcpcs_code,
        sum(performance.n_of_svcs) AS ttl_n_of_svcs,
        sum(performance.est_ttl_submitted_charge_amt) AS est_ttl_submitted_charge_amt,
        sum(performance.est_ttl_mcare_pay_amt) AS est_ttl_mcare_pay_amt,
        (sum(performance.est_ttl_submitted_charge_amt) - sum(performance.est_ttl_mcare_pay_amt)) AS var_est_ttl_mcare_submitted_charge_pay_amt,
        (sum(performance.est_ttl_mcare_pay_amt) / (count(performance.hcpcs_code))::numeric) AS est_ttl_mcare_pay_amt_by_ttl_hcpcs_code,
        (sum(performance.est_ttl_mcare_pay_amt) / sum(performance.n_of_svcs)) AS est_ttl_mcare_pay_amt_by_ttl_n_of_svcs,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (count(performance.hcpcs_code)) DESC) AS rank_ttl_hcpcs_code,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.n_of_svcs)) DESC) AS rank_ttl_n_of_svcs,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_submitted_charge_amt)) DESC) AS rank_est_ttl_submitted_charge_amt,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt)) DESC) AS rank_est_ttl_mcare_pay_amt,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_submitted_charge_amt) - sum(performance.est_ttl_mcare_pay_amt)) DESC) AS rank_var_est_ttl_mcare_submitted_charge_pay_amoun,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt) / (count(performance.hcpcs_code))::numeric) DESC) AS rank_est_ttl_mcare_pay_amt_by_ttl_hcpcs_code,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt) / sum(performance.n_of_svcs)) DESC) AS rank_est_ttl_mcare_pay_amt_by_ttl_n_of_servi
       FROM (cms.service_provider_performance performance
         JOIN cms.services svcs ON ((svcs.hcpcs_code = performance.hcpcs_code)))
      WHERE (svcs.hcpcs_drug_indicator = 'N'::text)
      GROUP BY performance.npi, performance.entity_type
    ;
    
    CREATE VIEW cms.live_service_provider_performance_summary_drug_yes AS SELECT 2 AS summary_type,
        performance.npi,
        performance.entity_type,
        count(performance.hcpcs_code) AS ttl_hcpcs_code,
        sum(performance.n_of_svcs) AS ttl_n_of_svcs,
        sum(performance.est_ttl_submitted_charge_amt) AS est_ttl_submitted_charge_amt,
        sum(performance.est_ttl_mcare_pay_amt) AS est_ttl_mcare_pay_amt,
        (sum(performance.est_ttl_submitted_charge_amt) - sum(performance.est_ttl_mcare_pay_amt)) AS var_est_ttl_mcare_submitted_charge_pay_amt,
        (sum(performance.est_ttl_mcare_pay_amt) / (count(performance.hcpcs_code))::numeric) AS est_ttl_mcare_pay_amt_by_ttl_hcpcs_code,
        (sum(performance.est_ttl_mcare_pay_amt) / sum(performance.n_of_svcs)) AS est_ttl_mcare_pay_amt_by_ttl_n_of_svcs,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (count(performance.hcpcs_code)) DESC) AS rank_ttl_hcpcs_code,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.n_of_svcs)) DESC) AS rank_ttl_n_of_svcs,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_submitted_charge_amt)) DESC) AS rank_est_ttl_submitted_charge_amt,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt)) DESC) AS rank_est_ttl_mcare_pay_amt,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_submitted_charge_amt) - sum(performance.est_ttl_mcare_pay_amt)) DESC) AS rank_var_est_ttl_mcare_submitted_charge_pay_amoun,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt) / (count(performance.hcpcs_code))::numeric) DESC) AS rank_est_ttl_mcare_pay_amt_by_ttl_hcpcs_code,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt) / sum(performance.n_of_svcs)) DESC) AS rank_est_ttl_mcare_pay_amt_by_ttl_n_of_servi
       FROM (cms.service_provider_performance performance
         JOIN cms.services svcs ON ((svcs.hcpcs_code = performance.hcpcs_code)))
      WHERE (svcs.hcpcs_drug_indicator = 'Y'::text)
      GROUP BY performance.npi, performance.entity_type
    ;
    
    CREATE VIEW cms.live_service_provider_performance_summary_overall AS SELECT 1 AS summary_type,
        performance.npi,
        performance.entity_type,
        count(performance.hcpcs_code) AS ttl_hcpcs_code,
        sum(performance.n_of_svcs) AS ttl_n_of_svcs,
        sum(performance.est_ttl_submitted_charge_amt) AS est_ttl_submitted_charge_amt,
        sum(performance.est_ttl_mcare_pay_amt) AS est_ttl_mcare_pay_amt,
        (sum(performance.est_ttl_submitted_charge_amt) - sum(performance.est_ttl_mcare_pay_amt)) AS var_est_ttl_mcare_submitted_charge_pay_amt,
        (sum(performance.est_ttl_mcare_pay_amt) / (count(performance.hcpcs_code))::numeric) AS est_ttl_mcare_pay_amt_by_ttl_hcpcs_code,
        (sum(performance.est_ttl_mcare_pay_amt) / sum(performance.n_of_svcs)) AS est_ttl_mcare_pay_amt_by_ttl_n_of_svcs,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (count(performance.hcpcs_code)) DESC) AS rank_ttl_hcpcs_code,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.n_of_svcs)) DESC) AS rank_ttl_n_of_svcs,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_submitted_charge_amt)) DESC) AS rank_est_ttl_submitted_charge_amt,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt)) DESC) AS rank_est_ttl_mcare_pay_amt,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_submitted_charge_amt) - sum(performance.est_ttl_mcare_pay_amt)) DESC) AS rank_var_est_ttl_mcare_submitted_charge_pay_amoun,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt) / (count(performance.hcpcs_code))::numeric) DESC) AS rank_est_ttl_mcare_pay_amt_by_ttl_hcpcs_code,
        rank() OVER (PARTITION BY performance.entity_type ORDER BY (sum(performance.est_ttl_mcare_pay_amt) / sum(performance.n_of_svcs)) DESC) AS rank_est_ttl_mcare_pay_amt_by_ttl_n_of_servi
       FROM cms.service_provider_performance performance
      GROUP BY performance.npi, performance.entity_type
    ;  
    `,
      'cms',
      false,
    )
  })
})
