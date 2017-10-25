#! /bin/bash

# Kill entire script on Ctrl+C
trap "exit" INT

SERVICE="${1:?Provide the service you want to test as a parameter}"
TEST_PACKAGE=${@:2}
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_NAME=${BUILDKITE_JOB_ID:-TEST}


DC_ARGS="--project-name $PROJECT_NAME --file $DIR/docker-compose.test.yml"


echo "Starting dependency services..."
docker-compose $DC_ARGS up -d client-db internal-db redis rabbit

until docker-compose $DC_ARGS run ping-db mysqladmin ping -h client-db -u root --protocol=TCP > /dev/null; do
    echo "$(date) - waiting for mysql (client)"
    sleep 1
done
until docker-compose $DC_ARGS run ping-db mysqladmin ping -h internal-db -u root --protocol=TCP > /dev/null; do
    echo "$(date) - waiting for mysql (internal)"
    sleep 1
done

# script is invoked with a service parameter
echo "Starting tests for $SERVICE..."
docker-compose $DC_ARGS run app sbt -mem 3072 "$SERVICE/testOnly $TEST_PACKAGE"

EXIT_CODE=$?

echo "Stopping dependency services..."
docker-compose $DC_ARGS kill

exit $EXIT_CODE
