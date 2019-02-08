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
  if [[ $CIRCLE_TAG == *"beta" ]]; then
    export CIRCLE_BRANCH=beta
  fi
  if [[ $CIRCLE_TAG == *"alpha" ]]; then
    export CIRCLE_BRANCH=alpha
  fi
fi

if [[ -z "$CIRCLE_BRANCH" ]]; then
  export CIRCLE_BRANCH=master
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
datamodelChanged=false

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

if [[ "$changedFiles" = *"cli/packages/prisma-client-lib"* ]]; then
  clientChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-generate-schema"* ]]; then
  generateSchemaChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-datamodel"* ]]; then
  datamodelChanged=true
fi

echo "introspection changed: $introspectionChanged yml changed: $ymlChanged. core changed: $coreChanged. engine changed: $engineChanged"

if [ $introspectionChanged == false ] && [ $ymlChanged == false ] && [ $coreChanged == false ] && [ $engineChanged == false ] && [ $clientChanged == false ] && [ $generateSchemaChanged == false ] && [ -z "$CIRCLE_TAG" ] && [ $datamodelChanged == false ]; then
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
  if [[ -n "$CIRCLE_TAG" ]]; then
    echo "Setting Step to 0"
    step=0
  fi
  nextDockerMinor=$((nextDockerMinor + step))
  nextDockerTag="${tagElements[0]}.${nextDockerMinor}-${CIRCLE_BRANCH}"
fi

node cli/scripts/waitUntilTagPublished.js $nextDockerTag

#
# Get new version
#

# If CIRCLE_TAG doesnt exist, generate the version with our node script
if [ -z "$CIRCLE_TAG" ]; then
  export newVersion=$(eval node ./cli/scripts/get-version.js $CIRCLE_BRANCH)
  echo "New version: $newVersion"
  echo "Waiting 10 seconds so you can stop the script if this is not correct"
  sleep 10
else
  export newVersion=$CIRCLE_TAG
fi



######################
# Build cli/packages #
######################

cd cli/packages/
#
# Build prisma-datamodel
#

if [ $generateSchemaChanged ] || [ $clientChanged ] || [ $coreChanged ] || [ $datamodelChanged ]; then
  cd prisma-datamodel
  sleep 3.0
  ../../scripts/doubleInstall.sh
  yarn build
  npm version $newVersion

  if [[ $CIRCLE_TAG ]]; then
    npm publish
  else
    npm publish --tag $CIRCLE_BRANCH
  fi
  sleep 20.0
  cd ..
fi

#
# Build prisma-generate-schema
#

if [ $generateSchemaChanged ] || [ $clientChanged ] || [ $coreChanged ]; then
  cd prisma-generate-schema
  sleep 3.0
  ../../scripts/doubleInstall.sh
  yarn add prisma-datamodel@$newVersion
  yarn build
  npm version $newVersion

  if [[ $CIRCLE_TAG ]]; then
    npm publish
  else
    npm publish --tag $CIRCLE_BRANCH
  fi
  cd ..
fi
export generateSchemaVersion=$(cat prisma-generate-schema/package.json | jq -r '.version')

#
# Build prisma-client-lib
#

cd prisma-client-lib
export clientVersionBefore=$(cat package.json | jq -r '.version')
if [ $clientChanged ] || [ $CIRCLE_TAG ]; then
  echo "Going to publish client"
  yarn add prisma-datamodel@$newVersion prisma-generate-schema@$newVersion
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



########################
# Back to cli/packages #
########################

#
# Build prisma-yml
#

export ymlVersionBefore=$(cat prisma-yml/package.json | jq -r '.version')
if [ $ymlChanged ] || [ $CIRCLE_TAG ]; then
  echo "Going to publish yml"
  cd prisma-yml
  ../../scripts/doubleInstall.sh
  yarn build
  npm version $newVersion

  if [[ $CIRCLE_TAG ]]; then
    npm publish
  else
    npm publish --tag $CIRCLE_BRANCH
  fi
  ../../scripts/doubleInstall.sh
  cd ..
fi
export ymlVersion=$(cat prisma-yml/package.json | jq -r '.version')

#
# Build prisma-cli-engine
#

if [ $ymlVersionBefore != $ymlVersion ] || [ $engineChanged ]; then
  cd prisma-cli-engine
  sleep 3.0
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
export engineVersion=$(cat prisma-cli-engine/package.json | jq -r '.version')


#
# Build prisma-db-introspection
#

export introspectionVersionBefore=$(cat prisma-db-introspection/package.json | jq -r '.version')

if [ $ymlVersionBefore != $ymlVersion ] || [ $introspectionChanged ] || [ $CIRCLE_TAG ]; then
  cd prisma-db-introspection
  sleep 0.5
  yarn add prisma-datamodel@$ymlVersion prisma-yml@$ymlVersion
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
# Build prisma-cli-core
#

if [ $ymlVersionBefore != $ymlVersion ] || [ $coreChanged ] || [ $introspectionChanged ]; then
  cd prisma-cli-core
  sleep 3.0
  yarn add prisma-datamodel@$newVersion
  sleep 0.2
  yarn add prisma-yml@$ymlVersion
  sleep 0.2
  yarn add prisma-db-introspection@$introspectionVersion
  sleep 0.5
  yarn add prisma-generate-schema@$generateSchemaVersion
  sleep 0.2
  yarn add prisma-client-lib@$clientVersion
  sleep 0.3
  ../../scripts/doubleInstall.sh

  # new docker tag
  sed -i.bak "s/image: prismagraphql\/prisma:[0-9]\{1,\}\.[0-9]\{1,\}/image: prismagraphql\/prisma:$nextDockerTag/g" src/utils/util.ts

  cat src/utils/util.ts

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
../../scripts/doubleInstall.sh
yarn build

if [[ -n "$CIRCLE_TAG" ]] && [[ "$CIRCLE_BRANCH" == "master" ]]; then
  newVersion=$CIRCLE_TAG

  echo "new version: $newVersion"
  npm version $newVersion
  npm publish
else
  npm version $newVersion
  npm publish --tag $CIRCLE_BRANCH
fi

############################################
# Run integration tests on prisma-examples #
############################################

languages=( typescript flow )

# Clone

git clone git@github.com:prisma/prisma-examples.git
cd prisma-examples
branch="client-$CIRCLE_BRANCH"

if [ $CIRCLE_BRANCH == "alpha" ] || [ $CIRCLE_BRANCH == "beta" ]; then
  # Setup branch
  git checkout $branch

  # Bump prisma-client-lib version
  for language in "${languages[@]}"; do
    cd $language

    for example in */; do
      cd $example

      yarn add prisma-client-lib@$CIRCLE_BRANCH 

      cd ..
    done 

    cd ..
  done

  # Push changes
  git config --global user.email "tim.suchanek@gmail.com"
  git config --global user.name "Tim Suchanek"
  git commit -a -m "bump prisma-client-lib versions to ${newVersion}"
  git remote add origin-push https://${GH_TOKEN}@github.com/prisma/prisma-examples.git > /dev/null 2>&1
  git push --quiet --set-upstream origin-push $branch
fi
