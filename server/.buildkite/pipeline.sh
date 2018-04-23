#! /bin/bash

declare -a connectors=(mysql postgres)

# Projects with a locked connector
static=$(printf "    - label: \":mysql: MySql API connector\"
      command: cd server && ./.buildkite/scripts/test.sh api-connector-mysql mysql

    - label: \":mysql: MySql deploy connector\"
      command: cd server && ./.buildkite/scripts/test.sh deploy-connector-mysql mysql

    - label: \":postgres: Postgres API connector\"
      command: cd server && ./.buildkite/scripts/test.sh api-connector-postgresql postgres

    - label: \":postgres: Postgres deploy connector\"
      command: cd server && ./.buildkite/scripts/test.sh deploy-connector-postgresql postgres

    # Libs are not specific to a connector, simply run with mysql
    - label: \":scala: libs\"
      command: cd server && ./.buildkite/scripts/test.sh libs mysql
")

optional=""

if [ -z "$BUILDKITE_TAG" ]; then
    # Regular commit
    git diff --exit-code --name-only ${BUILDKITE_COMMIT} ${BUILDKITE_COMMIT}~1 | grep "server/"
    if [ $? -ne 0 ]; then
      buildkite-agent pipeline upload ./server/.buildkite/empty-pipeline.yml
      exit 0
    else
      optional=$(printf "
    - wait

    - label: \":docker: Build unstable channel\"
      command: ./server/.buildkite/scripts/docker-build.sh unstable
      branches: master
    ")
    fi
else
    # Build was triggered by a tagged commit
    optional=$(printf "    - wait

    - label: \":docker: Build stable channel\"
      command: ./server/.buildkite/scripts/docker-build.sh stable
    ")
fi

dynamic=""

for connector in "${connectors[@]}"
do
   dynamic=$(printf "$dynamic
%s
" "    - label: \":scala: images [$connector]\"
      command: cd server && ./.buildkite/scripts/test.sh images $connector

    - label: \":scala: deploy [$connector]\"
      command: cd server && ./.buildkite/scripts/test.sh deploy $connector

    - label: \":scala: api [$connector]\"
      command: cd server && ./.buildkite/scripts/test.sh api $connector

    - label: \":scala: subscriptions [$connector]\"
      command: cd server && ./.buildkite/scripts/test.sh subscriptions $connector

    - label: \":scala: workers [$connector]\"
      command: cd server && ./.buildkite/scripts/test.sh workers $connector

    - label: \":scala: integration-tests [$connector]\"
      command: cd server && ./.buildkite/scripts/test.sh integrationTests $connector

")

done


pipeline=$(printf "
steps:
$static
$dynamic
$optional
")

echo "$pipeline" | buildkite-agent pipeline upload