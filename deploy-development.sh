#!/usr/bin/env bash
set -euo pipefail

NAMESPACE=${NAMESPACE:-caritas}
DEPLOYMENT=${DEPLOYMENT:-oriso-platform-tenantservice}
# TenantService builds on Java 21 since the 2026-07 upgrade; the old
# java-17 default no longer exists on the deploy host and broke this script.
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}
export PATH="$JAVA_HOME/bin:$PATH"

echo "Starting TenantService build & deployment..."

cd "$(dirname "$(realpath "$0")")"

echo "Building Maven package..."
mvn -q -Dmaven.test.skip=true -Dspotless.check.skip=true package

echo "Copying JAR..."
cp -f target/TenantService.jar ./TenantService.jar

echo "Building Docker image..."
docker build -t oriso-tenantservice:latest .

echo "Importing image into k3s..."
docker save oriso-tenantservice:latest | sudo k3s ctr images import - > /dev/null 2>&1

echo "Restarting Kubernetes deployment..."
kubectl rollout restart deployment/${DEPLOYMENT} -n ${NAMESPACE}

echo "Waiting for rollout to complete..."
kubectl rollout status deployment/${DEPLOYMENT} -n ${NAMESPACE} --timeout=180s

echo "TenantService deployed successfully ✅"
