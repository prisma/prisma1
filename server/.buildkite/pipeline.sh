#!/usr/bin/env bash

CMD=$1

whoami
env

if [ -n "$CMD" ]; then
  $(which ruby) $(dirname $0)/build-cli/cli.rb "$@"
else
  $(which ruby) $(dirname $0)/build-cli/cli.rb pipeline
fi
