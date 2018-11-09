#!/bin/bash

if [ -z $PRISMA_HOST ]; then echo "Please specify the prisma host to use by setting the environment variable PRISMA_HOST."; exit; fi

for dir in ../../__tests__/blackbox/cases/*/
do
    model=$(basename $dir)
    echo "Deploying $model"
    sed s/{{model}}/${model}/g prisma.yml.template | sed s/{{prisma_host}}/${PRISMA_HOST}/g > prisma.yml
    prisma deploy
done
