#!/bin/bash
export PGPASSWORD=prisma

#!/bin/bash
for dir in ../__tests__/blackbox/cases/*/
do
    model=$(basename $dir)
    echo "Fetching schema for schema-generator\$$model"
    pg_dump --host localhost --port 5433 --user prisma -d prisma -n "\"schema-generator\$$model\"" > ../__tests__/blackbox/cases/${model}/postgres.sql
done
