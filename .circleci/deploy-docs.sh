#!/bin/bash

set -e
set -o pipefail

if [[ $CIRCLE_COMPARE_URL ]]; then
  export lastCommits=`echo $CIRCLE_COMPARE_URL | sed -n 's/.*compare\/\(.*\)/\1/p' | sed 's/\.\.\./ /'`
else
  export lastCommits="HEAD"
fi

export changedFiles=$(git diff-tree --no-commit-id --name-only -r $lastCommits)

if [[ "$changedFiles" = *"docs/1.0/"* ]] || \
   [[ "$changedFiles" = *"docs/1.1/"* ]] || \
   [[ "$changedFiles" = *"docs/1.2/"* ]] || \
   [[ "$changedFiles" = *"docs/1.3/"* ]] || \
   [[ "$changedFiles" = *"docs/1.4/"* ]] || \
   [[ "$changedFiles" = *"docs/1.5/"* ]] || \
   [[ "$changedFiles" = *"docs/1.6/"* ]] || \
   [[ "$changedFiles" = *"docs/1.7/"* ]] || \
   [[ "$changedFiles" = *"docs/1.8/"* ]] || \
   [[ "$changedFiles" = *"docs/1.9/"* ]] || \
   [[ "$changedFiles" = *"docs/1.10/"* ]] || \
   [[ "$changedFiles" = *"docs/1.11/"* ]] || \
   [[ "$changedFiles" = *"docs/1.12/"* ]] || \
   [[ "$changedFiles" = *"docs/1.13/"* ]] || \
   [[ "$changedFiles" = *"docs/1.14/"* ]];
then
  echo "There were changes in the old docs. Going to deploy old docs"

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
elif [[ "$changedFiles" = *"docs/"* ]]; then
  echo "Deploying new docs"
  curl -X POST -d '' $NETLIFY_HOOK_DOCS_V2
else
  echo "No Changes. Exiting"
  exit 0
fi

