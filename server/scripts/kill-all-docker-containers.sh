#! /bin/bash

(docker kill $(docker ps -q) &> /dev/null) || true
echo "Stopped all old docker containers..."