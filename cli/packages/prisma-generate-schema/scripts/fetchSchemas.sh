#!/bin/bash

if [ -z $PRISMA_HOST ]; then echo "Please specify the prisma host to use by setting the environment variable PRISMA_HOST."; exit; fi
if [ -z $1 ]; then echo "Please specify a name for the model (usually mongodb or relational)"; exit; fi

#!/bin/bash
for dir in ../__tests__/blackbox/cases/*/
do
    model=$(basename $dir)
    echo "Fetching $model"
    ts-node fetchSchemaFromEndpoint.ts http://${PRISMA_HOST}:4466/schema-generator/${model} > ../__tests__/blackbox/cases/${model}/${1}.graphql
done
