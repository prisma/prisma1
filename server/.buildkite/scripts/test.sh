#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/kill-all-docker-containers.sh

# Kill entire script on Ctrl+C
trap "exit" INT

SERVICE="${1:?Provide the service you want to test as a parameter}"
CONNECTOR="${2:?Provide the connector you want to use for this test run}"
PROJECT_NAME=${BUILDKITE_JOB_ID:-TEST}
DC_ARGS="--project-name $PROJECT_NAME --file $DIR/docker-test-setups/docker-compose.test.$CONNECTOR.yml"

echo "Starting dependency services..."
docker-compose $DC_ARGS up -d test-db rabbit

sleep 20

# script is invoked with a service parameter
echo "Starting tests for $SERVICE..."
docker-compose $DC_ARGS run app sbt -mem 3072 "$SERVICE/test"

EXIT_CODE=$?

echo "Stopping dependency services..."
docker-compose $DC_ARGS kill

exit $EXIT_CODE
