#!/bin/bash
set -e

git submodule update --init || true
cargo test