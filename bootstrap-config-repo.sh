#!/bin/bash

# Define the root of the new repo
REPO_NAME="billing-app-config"
mkdir -p "$REPO_NAME"
cd "$REPO_NAME"

echo "Initializing git repository..."
git init

# Create the directory structure
echo "Creating Helm chart structure..."
mkdir -p charts/backend/templates
mkdir -p charts/frontend/templates

# Create the base Chart.yaml for backend
cat <<EOF > charts/backend/Chart.yaml
apiVersion: v2
name: backend
description: A generic Helm chart for all Java Spring Boot microservices
type: application
version: 1.0.0
appVersion: "1.0"
EOF

# Create the values directory for each microservice
SERVICES=("api-gateway" "authentication-service" "billing-service" "boutique-service" "catalog-service" "customer-service" "subscription-service" "usage-service")

for svc in "${SERVICES[@]}"; do
    mkdir -p "charts/backend/services/$svc"
    cat <<EOF > "charts/backend/services/$svc/values.yaml"
# Default values for $svc
image:
  repository: 123456789012.dkr.ecr.us-east-1.amazonaws.com/billing/$svc
  tag: "latest"
  pullPolicy: Always

replicaCount: 1

resources:
  requests:
    cpu: 200m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi
EOF
done

echo "# GitOps Config Repository" > README.md
echo "This repository holds the Kubernetes configuration (Helm charts & values.yaml) for the billing application." >> README.md
echo "It is automatically updated by Jenkins and monitored by ArgoCD." >> README.md

git add .
git commit -m "Initial commit: Setup Helm chart and values structure"

echo "=================================================="
echo "Successfully created the local repository at: $PWD"
echo "To push this to GitHub, run:"
echo "  cd $REPO_NAME"
echo "  git remote add origin https://github.com/ghassenchouchen/billing-app-config.git"
echo "  git branch -M main"
echo "  git push -u origin main"
echo "=================================================="
