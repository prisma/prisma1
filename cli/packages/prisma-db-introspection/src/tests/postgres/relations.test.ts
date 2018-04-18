import { Introspector } from '../../Introspector'
import { Client } from 'pg'

function introspect(): Promise<string> {
  return new Introspector('').introspect('DatabaseIntrospector')
}

async function testSchema(sql: string) {
  const client = new Client({ connectionString: '' })
  await client.connect()
  await client.query('DROP SCHEMA IF EXISTS DatabaseIntrospector cascade;')
  await client.query('CREATE SCHEMA DatabaseIntrospector;')
  await client.query('SET search_path TO DatabaseIntrospector;')
  await client.query(sql)

  expect(await introspect()).toMatchSnapshot()

  await client.end()
}

describe('Introspector', () => {
  test('relation with relation table', async () => {
    await testSchema(`CREATE TABLE product (
      id         serial PRIMARY KEY  -- implicit primary key constraint
    , product    text NOT NULL
    );
    
    CREATE TABLE bill (
      id       serial PRIMARY KEY
    , bill     text NOT NULL
    );
    
    CREATE TABLE bill_product (
      bill_id    int REFERENCES bill (id) ON UPDATE CASCADE ON DELETE CASCADE
    , product_id int REFERENCES product (id) ON UPDATE CASCADE
    );`)
  })

  test('relation with inline relation column', async () => {
    await testSchema(`CREATE TABLE product (
      id           serial PRIMARY KEY  -- implicit primary key constraint
    , description  text NOT NULL
    );
    
    CREATE TABLE bill (
      id         serial PRIMARY KEY
    , bill       text NOT NULL
    , product_id int REFERENCES product (id) ON UPDATE CASCADE
    );`)
  })
})
