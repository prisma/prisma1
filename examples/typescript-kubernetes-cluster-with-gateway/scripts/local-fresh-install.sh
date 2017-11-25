#!/usr/bin/env bash

#
# THIS SHOULD ONLY BE RAN ONCE
# RE-RUNNING THIS SCRIPT WILL DELETE ALL PERSISTED DATA
# AND CHANGE YOUR MINIKUBE IP.
#

# Validation check
command -v VBoxManage >/dev/null 2>&1 || { echo >&2 "VirtualBox isn't installed. Aborting."; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo >&2 "Kube command line interface isn't installed. Aborting."; exit 1; }
command -v minikube >/dev/null 2>&1 || { echo >&2 "Minikube isn't installed. Aborting."; exit 1; }

eval "minikube delete"
eval "minikube start"

eval "sleep 3"

eval "minikube addons enable heapster"
eval "kubectl create -f $(pwd)/config/local/secrets.yaml"
eval "kubectl create -f $(pwd)/config/local/db.yaml"
eval "kubectl create -f $(pwd)/config/local/faas.yaml"
eval "kubectl create -f $(pwd)/config/local/graphcool.yaml"

eval "sleep 3"

echo ""
echo "-----------------------------"
echo "Kubernetes Creation Complete"
eval "minikube ip"
echo "Dashboard: minikube dashboard"
echo "-----------------------------"
exit 0
