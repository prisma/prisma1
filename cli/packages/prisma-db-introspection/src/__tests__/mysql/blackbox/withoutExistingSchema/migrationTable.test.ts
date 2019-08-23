import testSchema from '../common'

describe('Reserved Tables', () => {
  test('ignore relation table', async () => {
    await testSchema(`CREATE TABLE \`Test\` (
      \`pk\` varchar(55) NOT NULL,
      \`a\` char(1) DEFAULT NULL UNIQUE
      );
      
      CREATE TABLE \`_Migration\` (
        \`pk\` int NOT NULL PRIMARY KEY
      );`)
  })
})
