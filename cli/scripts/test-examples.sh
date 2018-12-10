git clone git@github.com:prisma/prisma-examples.git

cd prisma-examples
cd flow

flow_dirs=(
  graphql
  script
)

sudo yarn global add prisma@$newVersion typescript

for dir in $flow_dirs
do
  cd $dir
  yarn
  yarn add prisma-client-lib@$newVersion
  prisma generate
  flow check
  cd ..
done

cd ..
cd typescript

ts_dirs=(
  circleci
  cli-app
  graphql-auth
  graphql-subscriptions
  graphql
  script
)

for dir in $ts_dirs
do
  cd $dir
  yarn
  yarn add --dev prisma@$newVersion
  yarn add prisma-client-lib@$newVersion
  prisma generate
  tsc -d
  cd ..
done

rm -rf ./prisma-examples