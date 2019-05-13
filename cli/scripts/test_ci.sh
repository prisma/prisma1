#!/bin/bash

set -ex

#
# Detect change
#
if [[ $CIRCLE_COMPARE_URL ]]; then
  export lastCommits=`echo $CIRCLE_COMPARE_URL | sed -n 's/.*compare\/\(.*\)/\1/p' | sed 's/\.\.\./ /'`
else
  export lastCommits="HEAD"
fi

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