#!/bin/bash

set -e

#
# Detect change
#

export changedFiles=$(git diff-tree --no-commit-id --name-only -r $lastCommits)

cliChanged=false

if [[ "$changedFiles" = *"cli/"* ]]; then
  cliChanged=true
fi

if [ $cliChanged == false ]; then
  echo "There are no changes in the CLI."
  exit 0;
fi

./cli/scripts/test.sh