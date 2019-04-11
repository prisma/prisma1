#!/bin/bash

export PRISMA_HOST=localhost

cd postgres
docker-compose up -d
sleep 5s
../deploySchemas.sh relational_v1.1
cd ..
./fetchSchemas.sh relational_v1.1
./dumpPostgres.sh
cd postgres
docker-compose down
cd ..

cd mongodb
docker-compose up -d
sleep 5s
../deploySchemas.sh document
cd ..
./fetchSchemas.sh document
cd mongodb
docker-compose down
cd ..

cd mysql
docker-compose up -d
sleep 5s
../deploySchemas.sh relational_v1.1
cd ..
./dumpMysql.sh
cd mysql 
docker-compose down
cd ..
