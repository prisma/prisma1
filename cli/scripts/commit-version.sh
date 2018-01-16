#!/bin/bash

set -e
export version=$(cat packages/prisma-cli/package.json | jq -r '.version')
git add .
git commit -m "$version"