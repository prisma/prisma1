import Connectors from '../../connectors'
import { Client } from 'pg'
import { connectionDetails } from './connectionDetails'
import { PostgresConnector } from '../../databases/relational/postgres/postgresConnector'
import { DatabaseType } from 'prisma-datamodel'
import { connect } from 'tls'

async function introspect(client: Client) {
  return (await Connectors.create(DatabaseType.postgres, client).introspect('databaseintrospector')).renderToNormalizedDatamodelString()
}

async function testSchema(sql: string) {
  const client = new Client(connectionDetails)

  await client.connect()
  await client.query('DROP SCHEMA IF EXISTS DatabaseIntrospector cascade;')
  await client.query('CREATE SCHEMA DatabaseIntrospector;')
  await client.query('SET search_path TO DatabaseIntrospector;')
  await client.query(sql)

  expect(await introspect(client)).toMatchSnapshot()

  await client.end()
}

describe('Introspector', () => {
  // Schema from https://github.com/prismagraphql/prisma/issues/2504
  test('test schema - 2504', async () => {
    await testSchema(sql)
  })
})

const sql = `
/*******************************************************************************
 Create Tables
********************************************************************************/

CREATE TABLE "Employee"
(
  "EmployeeId" INT NOT NULL,
  "LastName" VARCHAR(20) NOT NULL,
  "FirstName" VARCHAR(20) NOT NULL,
  "Title" VARCHAR(30),
  "ReportsTo" INT,
  "BirthDate" TIMESTAMP,
  "HireDate" TIMESTAMP,
  "Address" VARCHAR(70),
  "City" VARCHAR(40),
  "State" VARCHAR(40),
  "Country" VARCHAR(40),
  "PostalCode" VARCHAR(10),
  "Phone" VARCHAR(24),
  "Fax" VARCHAR(24),
  "Email" VARCHAR(60),
  CONSTRAINT "PK_Employee" PRIMARY KEY  ("EmployeeId")
);

/*******************************************************************************
 Create Primary Key Unique Indexes
********************************************************************************/

/*******************************************************************************
 Create Foreign Keys
********************************************************************************/

ALTER TABLE "Employee" ADD CONSTRAINT "FK_EmployeeReportsTo"
  FOREIGN KEY ("ReportsTo") REFERENCES "Employee" ("EmployeeId") ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX "IFK_EmployeeReportsTo" ON "Employee" ("ReportsTo");
`
