#!/usr/bin/env bash

# Validation check
command -v VBoxManage >/dev/null 2>&1 || { echo >&2 "VirtualBox isn't installed. Aborting."; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo >&2 "KubeCTL isn't installed. Aborting."; exit 1; }
command -v minikube >/dev/null 2>&1 || { echo >&2 "Minikube isn't installed. Aborting."; exit 1; }

eval "kubectl replace -f $(pwd)/config/local/secrets.yaml"
eval "kubectl replace -f $(pwd)/config/local/db.yaml"
eval "kubectl replace -f $(pwd)/config/local/faas.yaml"
eval "kubectl replace -f $(pwd)/config/local/graphcool.yaml"

eval "sleep 3"

echo ""
echo "-----------------------------"
echo "Kubernetes Update Complete"
eval "minikube ip"
echo "Dashboard: minikube dashboard"
echo "-----------------------------"
exit 0
