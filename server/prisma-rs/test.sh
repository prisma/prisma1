#!/bin/bash
set -e

git submodule update --init
cargo test