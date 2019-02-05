#!/bin/bash
for dir in ../__tests__/blackbox/cases/*/
do
    model=$(basename $dir)
    echo "Fetching schema for schema-generator\$$model"
    mysqldump "schema-generator@$model" --protocol=tcp --host=localhost --port=3306 --user=root --password=prisma --no-data > ../__tests__/blackbox/cases/${model}/mysql.sql
done
