import * as path from 'path'
import * as fs from 'fs'
import { DefaultParser, DefaultRenderer, DatabaseType } from 'prisma-datamodel'

import * as mysql from 'mysql'
import { connectionDetails } from '../connectionDetails'
import { MysqlConnector } from '../../../../databases/relational/mysql/mysqlConnector'
import MysqlClient from '../../../../databases/relational/mysql/mysqlDatabaseClient'

// If you have trouble signing in to mysql 8, run
// ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'prisma';
// FLUSH PRIVILEGES;

// Tests are located in different module.
const relativeTestCaseDir = path.join(
  __dirname,
  '../../../../../../prisma-generate-schema/__tests__/blackbox/cases/',
)

export default async function blackBoxTest(name: string) {
  const modelPath = path.join(
    relativeTestCaseDir,
    `${name}/model_relational.graphql`,
  )
  const sqlDumpPath = path.join(relativeTestCaseDir, `${name}/mysql.sql`)

  expect(fs.existsSync(modelPath))
  expect(fs.existsSync(sqlDumpPath))

  const model = fs.readFileSync(modelPath, { encoding: 'UTF-8' })
  const sqlDump = fs.readFileSync(sqlDumpPath, { encoding: 'UTF-8' })

  const parser = DefaultParser.create(DatabaseType.postgres)

  const refModel = parser.parseFromSchemaString(model)

  connectionDetails.database = ``
  connectionDetails.multipleStatements = true
  const dbClient = mysql.createConnection(connectionDetails)
  const wrappedClient = new MysqlClient(dbClient)

  const connector = new MysqlConnector(wrappedClient)
  await dbClient.connect()
  await wrappedClient.query(
    `DROP DATABASE IF EXISTS \`schema-generator@${name}\`;`,
    [],
  )
  await wrappedClient.query(`CREATE DATABASE \`schema-generator@${name}\`;`, [])
  await wrappedClient.query(`USE \`schema-generator@${name}\`;`, [])
  await wrappedClient.query(sqlDump, [])

  const introspectionResult = await connector.introspect(
    `schema-generator@${name}`,
  )

  const unnormalized = introspectionResult.getDatamodel()

  const normalizedWithoutReference = introspectionResult.getNormalizedDatamodel()
  const normalizedWithReference = introspectionResult.getNormalizedDatamodel(
    refModel,
  )

  // Backwards compatible (v1) rendering
  const legacyRenderer = DefaultRenderer.create(DatabaseType.postgres)
  const legacyRenderedWithReference = legacyRenderer.render(
    normalizedWithReference,
    true,
  )

  expect(legacyRenderedWithReference).toMatchSnapshot(
    `${name} - legacy datamodel`,
  )

  // V2 rendering
  const renderer = DefaultRenderer.create(DatabaseType.postgres, true)
  const renderedWithReference = renderer.render(normalizedWithReference, true)

  expect(renderedWithReference).toMatchSnapshot(`${name} - v1.1 datamodel`)

  await dbClient.end()
}

/**
 * The CI Tests are flaky and fail on this, even it works locally
 * https://circleci.com/gh/prisma/prisma/4619
 * Need to debug
 */
const testNames = fs
  .readdirSync(relativeTestCaseDir)
  .filter(
    n =>
      ![
        'selfReferencing',
        'relations',
        'relationNames',
        'meshRelation',
        'airbnb',
      ].includes(n),
  )

for (const testName of testNames) {
  test(
    `Introspects ${testName}/mysql correctly`,
    async () => {
      await blackBoxTest(testName)
    },
    20000,
  )
}
