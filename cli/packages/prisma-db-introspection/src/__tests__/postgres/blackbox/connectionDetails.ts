export const connectionDetails = {
  database: process.env.TEST_PG_DB,
  user: process.env.TEST_PG_USER,
  password: process.env.TEST_PG_PASSWORD,
  host: process.env.TEST_PG_HOST,
  port: process.env.TEST_PG_PORT
    ? parseInt(process.env.TEST_PG_PORT, 10)
    : undefined,
}
