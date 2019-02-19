export const connectionDetails = {
  database: process.env.TEST_MYSQL_DB,
  user: process.env.TEST_MYSQL_USER,
  password: process.env.TEST_MYSQL_PASSWORD,
  host: process.env.TEST_MYSQL_HOST,
  port: process.env.TEST_MYSQL_PORT
    ? parseInt(process.env.TEST_MYSQL_PORT, 10)
    : undefined,
  multipleStatements: true,
}
