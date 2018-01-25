#!/bin/bash

set -e
set -o pipefail

export changedFiles=$(git diff-tree --no-commit-id --name-only -r HEAD)

if [[ "$changedFiles" = *"docs/"* ]]; then
  echo "There were changes in the docs folder. Going to deploy docs"
else
  echo "No Changes. Exiting"
  exit 0
fi


if [ ! -z "$CIRCLE_BRANCH" ]; then
  UPPER_BRANCH="MASTER"
  PAT_GREEN_FROM_BRANCH="PAT_GREEN_${UPPER_BRANCH}"
  PAT_GREEN=${!PAT_GREEN_FROM_BRANCH:?$PAT_GREEN_FROM_BRANCH env var not set}
  PAT_BLUE_FROM_BRANCH="PAT_BLUE_${UPPER_BRANCH}"
  PAT_BLUE=${!PAT_BLUE_FROM_BRANCH:?$PAT_BLUE_FROM_BRANCH env var not set}
fi


BRANCH="${CIRCLE_BRANCH:-dev}"

export PAT_BLUE="${PAT_BLUE:?PAT_BLUE env variable not set}"
export PAT_GREEN="${PAT_GREEN:?PAT_GREEN env variable not set}"
export BLUE_ID="${BLUE_ID:?BLUE_ID env variable not set}"
export GREEN_ID="${GREEN_ID:?GREEN_ID env variable not set}"
export SYSTEM_AUTH_TOKEN="${SYSTEM_AUTH_TOKEN:?SYSTEM_AUTH_TOKEN env variable not set}"

docs-cli -c ./docs

curl -X POST -d '' $NETLIFY_HOOK
