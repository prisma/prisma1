#!/bin/bash

#
# Build Order
# prisma-db-introspection
# prisma-yml
# prisma-cli-engine
# prisma-cli-core
# prisma-cli

set -e
set -x

if [ $CIRCLE_TAG ] && [ $CIRCLE_BRANCH != "master" ]; then
  echo "The Tag ${CIRCLE_TAG} has been set, but the branch is set to ${CIRCLE_BRANCH}. Tags are only allowed on master"
fi

if [ $CIRCLE_TAG ] && [ $CIRCLE_BRANCH == "master" ]; then
  echo "WARNING: CIRCLE_TAG is set to $CIRCLE_TAG. This will publish a new version on the @latest tag."
else
  echo "INFO: This will deploy a new version on the @$CIRCLE_BRANCH tag"
fi

if [[ $CIRCLE_COMPARE_URL ]]; then
  export lastCommits=`echo $CIRCLE_COMPARE_URL | sed -n 's/.*compare\/\(.*\)/\1/p' | sed 's/\.\.\./ /'`
else
  export lastCommits="HEAD"
fi

export changedFiles=$(git diff-tree --no-commit-id --name-only -r $lastCommits)

introspectionChanged=false
ymlChanged=false
coreChanged=false
engineChanged=false

if [[ "$changedFiles" = *"cli/packages/prisma-db-introspection"* ]]; then
  introspectionChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-yml"* ]]; then
  ymlChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-cli-core"* ]]; then
  coreChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-cli-engine"* ]]; then
  engineChanged=true
fi

echo "introspection changed: $introspectionChanged yml changed: $ymlChanged. core changed: $coreChanged. engine changed: $engineChanged"

if [ $introspectionChanged == false ] && [ $ymlChanged == false ] && [ $coreChanged == false ] && [ $engineChanged == false ] && [ -z "$CIRCLE_TAG" ]; then
  echo "There are no changes in the CLI."
  exit 0;
fi

latestVersion=$(npm info prisma version)
tag=${CIRCLE_TAG:-$latestVersion}
tagElements=(${tag//./ })
nextDockerMinor=${tagElements[1]}
if [[ $CIRCLE_TAG ]]; then
  nextDockerTag="${tagElements[0]}.${nextDockerMinor}"
else
  step=1
  if [ $CIRCLE_BRANCH == "alpha" ]; then
    step=2
  fi
  nextDockerMinor=$((nextDockerMinor + step))
  nextDockerTag="${tagElements[0]}.${nextDockerMinor}-${CIRCLE_BRANCH}"
fi

node cli/scripts/waitUntilTagPublished.js $nextDockerTag

cd cli/packages/

export introspectionVersionBefore=$(cat prisma-db-introspection/package.json | jq -r '.version')

if [ $introspectionChanged ] || [ $CIRCLE_TAG ]; then
  cd prisma-db-introspection
  sleep 0.5
  ../../scripts/doubleInstall.sh
  yarn build
  if [[ $CIRCLE_TAG ]]; then
    npm version --allow-same-version $(npm info prisma-db-introspection version)
    npm version patch --no-git-tag-version
    npm publish
  else
    npm version --allow-same-version $(npm info prisma-db-introspection version --tag $CIRCLE_BRANCH)
    npm version prerelease --no-git-tag-version
    npm publish --tag $CIRCLE_BRANCH
  fi
  cd ..
fi
export introspectionVersion=$(cat prisma-db-introspection/package.json | jq -r '.version')


export ymlVersionBefore=$(cat prisma-yml/package.json | jq -r '.version')

if [ $ymlChanged ] || [ $CIRCLE_TAG ]; then
  echo "Going to publish yml"
  cd prisma-yml
  yarn install
  yarn build
  if [[ $CIRCLE_TAG ]]; then
    # make sure it is the latest version
    npm version --allow-same-version $(npm info prisma-yml version)
    npm version patch --no-git-tag-version
    npm publish
  else
    npm version --allow-same-version $(npm info prisma-yml version --tag $CIRCLE_BRANCH)
    npm version prerelease --no-git-tag-version
    npm publish --tag $CIRCLE_BRANCH
  fi
  yarn install
  cd ..
fi
export ymlVersion=$(cat prisma-yml/package.json | jq -r '.version')

if [ $ymlVersionBefore != $ymlVersion ] || [ $engineChanged ]; then
  cd prisma-cli-engine
  sleep 3.0
  yarn add prisma-yml@$ymlVersion
  sleep 0.2
  ../../scripts/doubleInstall.sh
  yarn build
  if [[ $CIRCLE_TAG ]]; then
    npm version --allow-same-version $(npm info prisma-cli-engine version)
    npm version patch --no-git-tag-version
    npm publish
  else
    npm version --allow-same-version $(npm info prisma-cli-engine version --tag $CIRCLE_BRANCH)
    npm version prerelease --no-git-tag-version
    npm publish --tag $CIRCLE_BRANCH
  fi
  cd ..
fi
export engineVersion=$(cat prisma-cli-engine/package.json | jq -r '.version')


if [ $ymlVersionBefore != $ymlVersion ] || [ $coreChanged ] || [ $introspectionChanged ]; then
  cd prisma-cli-core
  sleep 3.0
  yarn add prisma-yml@$ymlVersion
  sleep 0.2
  yarn add prisma-db-introspection@$introspectionVersion
  yarn install

  # replace docker tag
  sed -i.bak "s/CLUSTER_VERSION: [0-9]\{1,\}\.[0-9]\{1,\}/CLUSTER_VERSION: $nextDockerTag/g" src/commands/local/docker/docker-compose.yml
  sed -i.bak "s/image: prismagraphql\/prisma:[0-9]\{1,\}\.[0-9]\{1,\}/image: prismagraphql\/prisma:$nextDockerTag/g" src/commands/local/docker/docker-compose.yml

  # new docker tag
  sed -i.bak "s/image: prismagraphql\/prisma:[0-9]\{1,\}\.[0-9]\{1,\}/image: prismagraphql\/prisma:$nextDockerTag/g" src/util.ts

  yarn build
  if [[ $CIRCLE_TAG ]]; then
    npm version --allow-same-version $(npm info prisma-cli-core version)
    npm version patch --no-git-tag-version
    npm publish
  else
    npm version --allow-same-version $(npm info prisma-cli-core version --tag $CIRCLE_BRANCH)
    npm version prerelease --no-git-tag-version
    npm publish --tag $CIRCLE_BRANCH
  fi
  cd ..
fi
export coreVersion=$(cat prisma-cli-core/package.json | jq -r '.version')

cd prisma-cli
sleep 0.5
yarn add prisma-cli-engine@$engineVersion prisma-cli-core@$coreVersion
yarn install
yarn build

if [ -z "$CIRCLE_TAG" ]; then
  latestBetaVersion=$(npm info prisma version --tag $CIRCLE_BRANCH)
  latestVersionElements=(${latestVersion//./ })
  latestBetaVersionElements=(${latestBetaVersion//./ })

  betaMinor=${latestBetaVersionElements[1]}
  latestMinor=${latestVersionElements[1]}
  latestMajor=${latestVersionElements[0]}

  betaLastNumber=`echo $latestBetaVersion | sed -n "s/.*$CIRCLE_BRANCH\.\([0-9]\{1,\}\)/\1/p"`

  echo "betaLastNumber $betaLastNumber"

  # calc next minor
  step=1
  if [ $CIRCLE_BRANCH == "alpha" ]; then
    step=2
  fi
  nextMinor=$((latestMinor + step))

  nextLastNumber=0

  echo "beta minor $betaMinor latest minor $latestMinor next minor ${nextMinor}"

  # calc next last number
  if [ $betaMinor > $latestMinor ] && [ $betaMinor != $latestMinor ]; then
    echo "$betaMinor is greater than $latestMinor"
    nextLastNumber=$((betaLastNumber + step))
  fi

  newVersion="$latestMajor.$nextMinor.0-$CIRCLE_BRANCH.$nextLastNumber"
  echo "new version: $newVersion"

  npm version $newVersion
  npm publish --tag $CIRCLE_BRANCH
else
  newVersion=$CIRCLE_TAG

  echo "new version: $newVersion"
  npm version $newVersion
  npm publish
fi
