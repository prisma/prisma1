#!/bin/bash

if [ -z $PRISMA_HOST ]; then echo "Please specify the prisma host to use by setting the environment variable PRISMA_HOST."; exit; fi
if [ -z $1 ]; then echo "Please specify a database type as first param (usually document or relational)"; exit; fi

for dir in ../../__tests__/blackbox/cases/*/
do
    model=$(basename $dir)
    echo "Deploying $model"
    sed s/{{model}}/${model}/g prisma.yml.template | sed s/{{prisma_host}}/${PRISMA_HOST}/g | sed s/{{type}}/$1/g > prisma.yml
    prisma deploy
#    ~/code/prisma/cli/packages/prisma-cli/dist/index.js introspect --sdl --prototype > "${dir}/model_${1}_v1.1.graphql"
done
