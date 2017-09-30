#!/bin/bash

set -e
set -o pipefail


if [ ! -z "$CIRCLE_BRANCH" ]; then
  UPPER_BRANCH=$(echo $CIRCLE_BRANCH | tr '[a-z]' '[A-Z]')
  PAT_GREEN_FROM_BRANCH="PAT_GREEN_${UPPER_BRANCH}"
  PAT_GREEN=${!PAT_GREEN_FROM_BRANCH:?$PAT_GREEN_FROM_BRANCH env var not set}
  PAT_BLUE_FROM_BRANCH="PAT_BLUE_${UPPER_BRANCH}"
  PAT_BLUE=${!PAT_BLUE_FROM_BRANCH:?$PAT_BLUE_FROM_BRANCH env var not set}
fi


BRANCH="${CIRCLE_BRANCH:-dev}"
CIRCLE_TOKEN="${CIRCLE_TOKEN:?CIRCLE_TOKEN env variable not set}"

export PAT_BLUE="${PAT_BLUE:?PAT_BLUE env variable not set}"
export PAT_GREEN="${PAT_GREEN:?PAT_GREEN env variable not set}"
export BLUE_ID="${BLUE_ID:?BLUE_ID env variable not set}"
export GREEN_ID="${GREEN_ID:?GREEN_ID env variable not set}"
export SYSTEM_AUTH_TOKEN="${SYSTEM_AUTH_TOKEN:?SYSTEM_AUTH_TOKEN env variable not set}"

docs-cli -c .

# trigger rebuild of docs
#curl -X POST https://circleci.com/api/v1.1/project/github/graphcool/homepage/tree/$BRANCH?circle-token=$CIRCLE_TOKEN
