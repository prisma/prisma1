#!/bin/bash

#
# Prisma Node.JS packages publish script
#

# Build Order
# prisma-client-lib
# prisma-generate-schema
# prisma-db-introspection
# prisma-yml
# prisma-cli-engine
# prisma-cli-core
# prisma-cli

set -e
set -x

#
# Normalize CIRCLE_BRANCH
#

if [[ -z "$CIRCLE_BRANCH" ]]; then
  if [[ $CIRCLE_TAG == "*beta" ]]; then
    export CIRCLE_BRANCH=beta
  fi
  if [[ $CIRCLE_TAG == "*alpha" ]]; then
    export CIRCLE_BRANCH=alpha
  fi
fi

if [ -z "$CIRCLE_TAG" ] && [ $CIRCLE_BRANCH == "master" ]; then
  echo "Builds on master are only executed when a tag is provided"
  exit 0
fi

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

#
# Detect change
#

export changedFiles=$(git diff-tree --no-commit-id --name-only -r $lastCommits)

ymlChanged=false
introspectionChanged=false
coreChanged=false
engineChanged=false
clientChanged=false
generateSchemaChanged=false

if [[ "$changedFiles" = *"cli/packages/prisma-yml"* ]]; then
  ymlChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-db-introspection"* ]]; then
  introspectionChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-cli-core"* ]]; then
  coreChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-cli-engine"* ]]; then
  engineChanged=true
fi

if [[ "$changedFiles" = *"client"* ]]; then
  clientChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-generate-schema"* ]]; then
  generateSchemaChanged=true
fi

echo "introspection changed: $introspectionChanged yml changed: $ymlChanged. core changed: $coreChanged. engine changed: $engineChanged"

if [ $introspectionChanged == false ] && [ $ymlChanged == false ] && [ $coreChanged == false ] && [ $engineChanged == false ] && [ $clientChanged == false ] && [ $generateSchemaChanged ] && [ -z "$CIRCLE_TAG" ]; then
  echo "There are no changes in the CLI."
  exit 0;
fi

#
# Get docker tag
#

latestVersion=$(npm info prisma version)
tag=${CIRCLE_TAG:-$latestVersion}
tagElements=(${tag//./ })
nextDockerMinor=${tagElements[1]}
if [[ $CIRCLE_TAG ]] && [[ $CIRCLE_BRANCH == "master" ]]; then
  nextDockerTag="${tagElements[0]}.${nextDockerMinor}"
else
  step=1
  if [ $CIRCLE_BRANCH == "alpha" ]; then
    step=2
  fi
  if [ $CIRCLE_BRANCH == "beta" ]; then
    step=3
  fi
  nextDockerMinor=$((nextDockerMinor + step))
  nextDockerTag="${tagElements[0]}.${nextDockerMinor}-${CIRCLE_BRANCH}"
fi

node cli/scripts/waitUntilTagPublished.js $nextDockerTag

#
# Get new version
#

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

  export newVersion="$latestMajor.$nextMinor.0-$CIRCLE_BRANCH.$nextLastNumber"
  echo "new version: $newVersion"
else
  export newVersion=$CIRCLE_TAG
fi


#
# Build prisma-client-lib
#

cd client
export clientVersionBefore=$(cat package.json | jq -r '.version')
if [ $clientChanged ] || [ $CIRCLE_TAG ]; then
  echo "Going to publish client"
  yarn install
  yarn build
  npm version $newVersion

  if [[ $CIRCLE_TAG ]]; then
    npm publish
  else
    npm publish --tag $CIRCLE_BRANCH
  fi

  yarn install
fi
export clientVersion=$(cat package.json | jq -r '.version')
cd ..


######################
# Build cli/packages #
######################

cd cli/packages/

#
# Build prisma-yml
#

export ymlVersionBefore=$(cat prisma-yml/package.json | jq -r '.version')
if [ $ymlChanged ] || [ $CIRCLE_TAG ]; then
  echo "Going to publish yml"
  cd prisma-yml
  yarn install
  yarn build
  npm version $newVersion

  if [[ $CIRCLE_TAG ]]; then
    npm publish
  else
    npm publish --tag $CIRCLE_BRANCH
  fi
  yarn install
  cd ..
fi
export ymlVersion=$(cat prisma-yml/package.json | jq -r '.version')

#
# Build prisma-db-introspection
#

export introspectionVersionBefore=$(cat prisma-db-introspection/package.json | jq -r '.version')

if [ $ymlVersionBefore != $ymlVersion ] || [ $introspectionChanged ] || [ $CIRCLE_TAG ]; then
  cd prisma-db-introspection
  sleep 0.5
  yarn add prisma-yml@$ymlVersion
  sleep 0.2
  ../../scripts/doubleInstall.sh
  yarn build
  npm version $newVersion

  if [[ $CIRCLE_TAG ]]; then
    npm publish
  else
    npm publish --tag $CIRCLE_BRANCH
  fi
  cd ..
fi
export introspectionVersion=$(cat prisma-db-introspection/package.json | jq -r '.version')


#
# Build prisma-generate-schema
#

# if [ $generateSchemaChanged ]; then
#   cd prisma-generate-schema
#   sleep 3.0
#   ../../scripts/doubleInstall.sh
#   yarn build
#   npm version $newVersion

#   if [[ $CIRCLE_TAG ]]; then
#     npm publish
#   else
#     npm publish --tag $CIRCLE_BRANCH
#   fi
#   cd ..
# fi
export generateSchemaVersion=$(cat prisma-generate-schema/package.json | jq -r '.version')


#
# Build prisma-cli-core
#

if [ $ymlVersionBefore != $ymlVersion ] || [ $coreChanged ] || [ $introspectionChanged ]; then
  cd prisma-cli-core
  sleep 3.0
  yarn add prisma-yml@$ymlVersion
  sleep 0.2
  yarn add prisma-db-introspection@$introspectionVersion
  # sleep 0.5
  # yarn add prisma-generate-schema@$generateSchemaVersion
  sleep 0.2
  yarn add prisma-client-lib@$clientVersion
  sleep 0.3
  yarn install

  # new docker tag
  sed -i.bak "s/image: prismagraphql\/prisma:[0-9]\{1,\}\.[0-9]\{1,\}/image: prismagraphql\/prisma:$nextDockerTag/g" src/util.ts

  cat src/util.ts

  yarn build
  npm version $newVersion

  if [[ $CIRCLE_TAG ]]; then
    npm publish
  else
    npm publish --tag $CIRCLE_BRANCH
  fi
  cd ..
fi
export coreVersion=$(cat prisma-cli-core/package.json | jq -r '.version')


#
# Build prisma
#

cd prisma-cli
cp ../../../README.md ./
sleep 0.5
yarn add prisma-cli-engine@$engineVersion prisma-cli-core@$coreVersion
yarn install
yarn build

if [ -z "$CIRCLE_TAG" ]; then

  npm version $newVersion
  npm publish --tag $CIRCLE_BRANCH
else
  newVersion=$CIRCLE_TAG

  echo "new version: $newVersion"
  npm version $newVersion
  npm publish
fi
