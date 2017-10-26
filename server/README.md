# Graphcool Server
The Graphcool Server is composed of a GraphQL database, the Realtime Subscriptions API, and an engine enabling asynchronous, event-driven flows using serverless functions.

## Running the tests
1. Make sure to have docker up and running
1. Make sure to make the env variables in `env_example` available, for example with `source env_example`.
1. Start all required dependencies with `docker-compose -f docker-compose/backend-dev.yml up -d --remove-orphans`
1. Run `sbt test`. This will take a few minutes.

If you want to clean up afterwards, make sure to run `docker-compose -f docker-compose/backend-dev.yml down -v --remove-orphans`