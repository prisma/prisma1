import testSchema from '../common'

describe('Introspector', () => {
  test('issue4095 relations', async () => {
    await testSchema(`-- -------------------------------------------------------------
    -- TablePlus 1.5(190)
    --
    -- https://tableplus.com/
    --
    -- Database: issue4019
    -- Generation Time: 2019-02-08 19:22:20.9670
    -- -------------------------------------------------------------
    
    
    DROP TABLE IF EXISTS "billing"."BillingAccount";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."BillingAccount_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."BillingAccount" (
        "id" int8 NOT NULL DEFAULT nextval('billing."BillingAccount_id_seq"'::regclass),
        "name" varchar(50) NOT NULL,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "customerId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Condition";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Condition_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Condition" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Condition_id_seq"'::regclass),
        "startDate" timestamptz NOT NULL,
        "endDate" timestamptz NOT NULL,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "productId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Contract";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Contract_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Contract" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Contract_id_seq"'::regclass),
        "code" varchar(50) NOT NULL,
        "enabled" bool DEFAULT false,
        "activationDateTime" timestamptz,
        "minimumFee" numeric NOT NULL,
        "lookToBook" numeric NOT NULL,
        "customerId" int8 NOT NULL,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Cost";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Cost_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Cost" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Cost_id_seq"'::regclass),
        "type" varchar(50) NOT NULL,
        "min" int4,
        "max" int4,
        "amount" numeric,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "contractId" int8,
        "itemId" int8,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Customer";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Customer_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Customer" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Customer_id_seq"'::regclass),
        "code" varchar(50) NOT NULL,
        "name" varchar(100) NOT NULL,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."GlobalCondition";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."GlobalCondition_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."GlobalCondition" (
        "id" int8 NOT NULL DEFAULT nextval('billing."GlobalCondition_id_seq"'::regclass),
        "type" varchar(50) NOT NULL,
        "numConnections" int4,
        "millionSearches" int4,
        "amount" numeric,
        "globalStartFreeTrialPeriod" timestamptz,
        "globalEndFreeTrialPeriod" timestamptz,
        "defaultFreeTrialDaysForItem" int4,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "contractId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Invoice";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Invoice_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Invoice" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Invoice_id_seq"'::regclass),
        "startDate" timestamptz NOT NULL,
        "endDate" timestamptz NOT NULL,
        "paymentMethod" varchar(100) NOT NULL,
        "paymentStatus" varchar(100) NOT NULL,
        "amount" numeric NOT NULL,
        "amountPaid" numeric NOT NULL,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "billingAccountId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."InvoiceBreakdown";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."InvoiceBreakdown_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."InvoiceBreakdown" (
        "id" int8 NOT NULL DEFAULT nextval('billing."InvoiceBreakdown_id_seq"'::regclass),
        "startDate" timestamptz NOT NULL,
        "endDate" timestamptz NOT NULL,
        "amount" numeric NOT NULL,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "itemBillingAccountId" int8 NOT NULL,
        "invoiceId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Item";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Item_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Item" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Item_id_seq"'::regclass),
        "enabled" bool DEFAULT false,
        "specificFreeTrialDays" int4,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "contractId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Item_BillingAccount";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Item_BillingAccount_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Item_BillingAccount" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Item_BillingAccount_id_seq"'::regclass),
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "billingAccountId" int8 NOT NULL,
        "itemId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    DROP TABLE IF EXISTS "billing"."Product";
    -- This script only contains the table creation statements and does not fully represent the table in the database. It's still missing: indices, triggers. Do not use it as a backup.
    
    -- Sequence and defined type
    CREATE SEQUENCE IF NOT EXISTS billing."Product_id_seq";
    
    -- Table Definition
    CREATE TABLE "billing"."Product" (
        "id" int8 NOT NULL DEFAULT nextval('billing."Product_id_seq"'::regclass),
        "type" varchar(50) NOT NULL,
        "system" varchar(200) NOT NULL,
        "createdAt" timestamptz NOT NULL,
        "updatedAt" timestamptz NOT NULL,
        "itemId" int8 NOT NULL,
        PRIMARY KEY ("id")
    );
    
    ALTER TABLE "billing"."BillingAccount" ADD FOREIGN KEY ("customerId") REFERENCES "billing"."Customer"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Condition" ADD FOREIGN KEY ("productId") REFERENCES "billing"."Product"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Contract" ADD FOREIGN KEY ("customerId") REFERENCES "billing"."Customer"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Cost" ADD FOREIGN KEY ("contractId") REFERENCES "billing"."Contract"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Cost" ADD FOREIGN KEY ("itemId") REFERENCES "billing"."Item"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."GlobalCondition" ADD FOREIGN KEY ("contractId") REFERENCES "billing"."Contract"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Invoice" ADD FOREIGN KEY ("billingAccountId") REFERENCES "billing"."BillingAccount"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."InvoiceBreakdown" ADD FOREIGN KEY ("invoiceId") REFERENCES "billing"."Invoice"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."InvoiceBreakdown" ADD FOREIGN KEY ("itemBillingAccountId") REFERENCES "billing"."Item_BillingAccount"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Item" ADD FOREIGN KEY ("contractId") REFERENCES "billing"."Contract"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Item_BillingAccount" ADD FOREIGN KEY ("billingAccountId") REFERENCES "billing"."BillingAccount"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Item_BillingAccount" ADD FOREIGN KEY ("itemId") REFERENCES "billing"."Item"("id") ON DELETE CASCADE ON UPDATE CASCADE;
    ALTER TABLE "billing"."Product" ADD FOREIGN KEY ("itemId") REFERENCES "billing"."Item"("id") ON DELETE CASCADE ON UPDATE CASCADE;`,
      'billing')
  })
})
