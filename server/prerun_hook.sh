#!/usr/bin/env bash

# Replace this file / the contents of this file in your custom Dockerfile for Prisma,
# to have the contents of the script run on Prisma startup.

# Example Dockerfile:
#   FROM prismagraphql/prisma:<desired version here>
#   COPY ./my_hook_script.sh /app/prerun_hook.sh

# If you want to trigger a webhook, for example, you will need to add additional packages. Prisma uses anapsix/alpine-java as base image.
#   RUN apk --no-cache add curl