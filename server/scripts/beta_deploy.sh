#!/usr/bin/env bash

TOKEN=${GITHUB_TOKEN}


echo "Loading cb binary..."
curl --header "Authorization: token ${TOKEN}" \
     --header 'Accept: application/vnd.github.v3.raw' \
     --location https://api.github.com/repos/graphcool/coolbelt/releases/latest -sSL | \
     jq '.assets[] | select(.name == "coolbelt") | .url' | \
     xargs -I "{}" \
         curl -sSL --header 'Accept: application/octet-stream' -o cb \
         --location "{}?access_token=${TOKEN}"

chmod +x cb

echo "Replacing images..."
cb service replace-all --customer graphcool --cluster database-beta-eu-west-1 --mode env
