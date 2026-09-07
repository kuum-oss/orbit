#!/usr/bin/env bash
# ===========================================
# k3d Cluster Setup for Orbit Platform
# ===========================================
# Creates a k3d cluster with port mappings
# for local Kubernetes development.
#
# Usage: ./infra/k8s/k3d-setup.sh
# ===========================================
set -euo pipefail

CLUSTER_NAME="orbit-cluster"

echo "=== Orbit k3d Cluster Setup ==="

# Check prerequisites
for cmd in docker k3d kubectl helm; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: $cmd is required but not installed."
        exit 1
    fi
done

# Delete existing cluster if present
if k3d cluster list | grep -q "${CLUSTER_NAME}"; then
    echo "Deleting existing cluster '${CLUSTER_NAME}'..."
    k3d cluster delete "${CLUSTER_NAME}"
fi

echo "Creating k3d cluster '${CLUSTER_NAME}'..."
k3d cluster create "${CLUSTER_NAME}" \
    --servers 1 \
    --agents 2 \
    --port "8080:80@loadbalancer" \
    --port "8443:443@loadbalancer" \
    --port "30080:30080@server:0" \
    --port "30443:30443@server:0" \
    --k3s-arg "--disable=traefik@server:0" \
    --wait

echo "Cluster created. Switching kubectl context..."
kubectl cluster-info

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Build and import Docker images into k3d
echo ""
echo "Building Docker images..."
for svc in orbit-ingest orbit-processor orbit-orchestrator orbit-gateway; do
    echo "  Building ${svc}..."
    docker build -t "${svc}:latest" -f "${PROJECT_ROOT}/${svc}/Dockerfile" "${PROJECT_ROOT}"
    echo "  Importing ${svc} into k3d..."
    k3d image import "${svc}:latest" -c "${CLUSTER_NAME}"
done

# Install metrics-server for HPA
echo ""
echo "Installing metrics-server..."
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml 2>/dev/null || true
# Patch metrics-server for k3d (insecure TLS)
kubectl patch deployment metrics-server -n kube-system \
    --type='json' \
    -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]' 2>/dev/null || true

# Create mTLS secret if certificates are generated
if [ -d "${PROJECT_ROOT}/infra/certs/generated" ] && [ -f "${PROJECT_ROOT}/infra/certs/generated/ca.crt" ]; then
    echo ""
    echo "Creating orbit-certs secret from generated certificates..."
    kubectl create secret generic orbit-certs \
        --from-file="${PROJECT_ROOT}/infra/certs/generated" \
        --dry-run=client -o yaml | kubectl apply -f -
fi

# Deploy with Helm
echo ""
echo "Deploying Orbit with Helm..."
helm upgrade --install orbit "${SCRIPT_DIR}/orbit-chart" \
    --set global.imagePullPolicy=Never \
    --wait --timeout 300s

echo ""
echo "=== Orbit deployed to k3d! ==="
echo ""
kubectl get pods
echo ""
kubectl get svc
echo ""
kubectl get hpa
