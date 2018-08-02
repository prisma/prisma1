#! /bin/bash

if [ -z "$BUILDKITE_TAG" ]; then
    # Regular commit, not a tag
    git diff --exit-code --name-only ${BUILDKITE_COMMIT} ${BUILDKITE_COMMIT}~1 | grep "server/"
    if [ $? -ne 0 ]; then
        echo "Nothing to do"
        exit 0
    fi
fi

declare -a connectors=(mysql postgres postgres-passive)

# Projects with a locked connector
static=$(printf "    - label: \":mysql: MySql 5.7 API connector\"
      command: cd server && ./.buildkite/scripts/test.sh api-connector-mysql mysql

    - label: \":mysql: MySql 5.7 deploy connector\"
      command: cd server && ./.buildkite/scripts/test.sh deploy-connector-mysql mysql

    - label: \":scala: integration-tests-mysql\"
      command: cd server && ./.buildkite/scripts/test.sh integration-tests-mysql mysql

    - label: \":mysql: MySql 8 API connector\"
      command: cd server && ./.buildkite/scripts/test.sh api-connector-mysql mysql8

    - label: \":mysql: MySql 8 deploy connector\"
      command: cd server && ./.buildkite/scripts/test.sh deploy-connector-mysql mysql8

    - label: \":postgres: Postgres API connector\"
      command: cd server && ./.buildkite/scripts/test.sh api-connector-postgres postgres

    - label: \":postgres: Postgres deploy connector\"
      command: cd server && ./.buildkite/scripts/test.sh deploy-connector-postgres postgres

    - label: \":scala: integration-tests-postgres\"
      command: cd server && ./.buildkite/scripts/test.sh integration-tests-mysql postgres

    # Libs are not specific to a connector, simply run with mysql
    - label: \":scala: libs\"
      command: cd server && ./.buildkite/scripts/test.sh libs mysql

    - label: \":scala: subscriptions\"
      command: cd server && ./.buildkite/scripts/test.sh subscriptions postgres

    - label: \":scala: shared-models\"
      command: cd server && ./.buildkite/scripts/test.sh shared-models mysql

")

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

    - label: \":scala: workers [$connector]\"
      command: cd server && ./.buildkite/scripts/test.sh workers $connector

")
done

docker=$(printf "
    - wait

    - label: \":docker: Build alpha channel\"
      command: ./server/.buildkite/scripts/unstable.sh alpha 2
      branches: alpha

    - label: \":docker: Check & Build beta channel\"
      command: ./server/.buildkite/scripts/unstable.sh beta 1

    - label: \":docker: Check & Build stable channel\"
      command: ./server/.buildkite/scripts/stable.sh
    ")

pipeline=$(printf "
steps:
$static
$dynamic
$docker
")

echo "$pipeline" | buildkite-agent pipeline upload