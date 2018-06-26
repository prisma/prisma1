#!/bin/bash
CONTENT=$(cat test.txt)

TOKEN=`prisma token`
OUTPUT="$(curl http://localhost:4466/travis/test -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' --data-binary '{"query":"{users{name}}","operationName":null}' --compressed)"
if [[ "${OUTPUT}" != "${CONTENT}" ]]; then
  echo "There was a problem with setting up Prisma in Travis."
  exit 1
else
  echo "The Prisma server is now set up in Travis."
  exit 0
fi