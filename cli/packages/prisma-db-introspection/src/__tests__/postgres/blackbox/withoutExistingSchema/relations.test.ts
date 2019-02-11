import testSchema from "../common"

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

  test('relation with relation table with extra column', async () => {
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
    , some_other_column text NOT NULL
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

  test('relation with inline relation column NOT NULL', async () => {
    await testSchema(`CREATE TABLE product (
      id           serial PRIMARY KEY  -- implicit primary key constraint
    , description  text NOT NULL
    );
    
    CREATE TABLE bill (
      id         serial PRIMARY KEY
    , bill       text NOT NULL
    , product_id int NOT NULL REFERENCES product (id) ON UPDATE CASCADE
    );`)
  })
})
