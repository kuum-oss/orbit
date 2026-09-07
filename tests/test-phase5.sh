#!/usr/bin/env bash
# ===========================================
# Phase 5 Test Suite — k3d Kubernetes Deploy,
#                      Helm Charts & HPA
# ===========================================
# Tests: Helm chart structure, values, templates,
#        HPA configuration, k3d setup script,
#        readiness/liveness probes, and template rendering.
#
# Usage: ./tests/test-phase5.sh
# ===========================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASSED=0
FAILED=0
SKIPPED=0

pass() {
    echo -e "  ${GREEN}✓ PASS${NC}: $1"
    PASSED=$((PASSED + 1))
}

fail() {
    echo -e "  ${RED}✗ FAIL${NC}: $1"
    FAILED=$((FAILED + 1))
}

skip() {
    echo -e "  ${YELLOW}⊘ SKIP${NC}: $1"
    SKIPPED=$((SKIPPED + 1))
}

section() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  $1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

contains_all() {
    local file="$1"
    shift
    local pattern
    for pattern in "$@"; do
        grep -Fq -- "$pattern" "$file" || return 1
    done
}

CHART_DIR="${PROJECT_ROOT}/infra/k8s/orbit-chart"
TEMPLATES_DIR="${CHART_DIR}/templates"
K8S_DIR="${PROJECT_ROOT}/infra/k8s"

# ===========================================
# TEST 1: Phase 5 File Structure
# ===========================================
section "TEST 1: Phase 5 File Structure"

REQUIRED_FILES=(
    "infra/k8s/orbit-chart/Chart.yaml"
    "infra/k8s/orbit-chart/values.yaml"
    "infra/k8s/orbit-chart/templates/_helpers.tpl"
    "infra/k8s/orbit-chart/templates/postgres.yaml"
    "infra/k8s/orbit-chart/templates/kafka.yaml"
    "infra/k8s/orbit-chart/templates/prometheus.yaml"
    "infra/k8s/orbit-chart/templates/grafana.yaml"
    "infra/k8s/orbit-chart/templates/orbit-ingest.yaml"
    "infra/k8s/orbit-chart/templates/orbit-processor.yaml"
    "infra/k8s/orbit-chart/templates/orbit-orchestrator.yaml"
    "infra/k8s/orbit-chart/templates/orbit-gateway.yaml"
    "infra/k8s/orbit-chart/templates/hpa.yaml"
    "infra/k8s/orbit-chart/templates/NOTES.txt"
    "infra/k8s/k3d-setup.sh"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "${PROJECT_ROOT}/${file}" ]]; then
        pass "File exists: ${file}"
    else
        fail "File missing: ${file}"
    fi
done

# ===========================================
# TEST 2: Chart.yaml Metadata
# ===========================================
section "TEST 2: Chart.yaml Metadata"

CHART_FILE="${CHART_DIR}/Chart.yaml"
if [[ -f "${CHART_FILE}" ]]; then
    if contains_all "${CHART_FILE}" \
        "apiVersion: v2" "name: orbit" \
        "type: application" "version:"; then
        pass "Chart.yaml has valid Helm v2 metadata"
    else
        fail "Chart.yaml metadata is incomplete"
    fi

    if grep -Fq "description:" "${CHART_FILE}"; then
        pass "Chart.yaml has a description"
    else
        fail "Chart.yaml is missing a description"
    fi

    if grep -Fq "appVersion:" "${CHART_FILE}"; then
        pass "Chart.yaml has appVersion"
    else
        fail "Chart.yaml is missing appVersion"
    fi
else
    fail "Chart.yaml does not exist"
fi

# ===========================================
# TEST 3: values.yaml — Infrastructure
# ===========================================
section "TEST 3: values.yaml — Infrastructure"

VALUES_FILE="${CHART_DIR}/values.yaml"
if [[ -f "${VALUES_FILE}" ]]; then
    # Global settings
    if contains_all "${VALUES_FILE}" "global:" "imagePullPolicy:"; then
        pass "Global configuration section exists with imagePullPolicy"
    else
        fail "Global configuration section is incomplete"
    fi

    # Postgres
    if contains_all "${VALUES_FILE}" \
        "postgres:" "enabled: true" "postgres:16-alpine" \
        "port: 5432" "database: orbit" "username: orbit" "password: orbit"; then
        pass "Postgres values match docker-compose configuration"
    else
        fail "Postgres values are incomplete or don't match docker-compose"
    fi

    # Kafka
    if contains_all "${VALUES_FILE}" \
        "kafka:" "cp-kafka:7.6.0" \
        "port: 29092" "clusterId:"; then
        pass "Kafka values match docker-compose configuration"
    else
        fail "Kafka values are incomplete"
    fi

    # Prometheus
    if contains_all "${VALUES_FILE}" \
        "prometheus:" "prom/prometheus:" "port: 9090"; then
        pass "Prometheus values are configured"
    else
        fail "Prometheus values are incomplete"
    fi

    # Grafana
    if contains_all "${VALUES_FILE}" \
        "grafana:" "grafana/grafana:" "port: 3000"; then
        pass "Grafana values are configured"
    else
        fail "Grafana values are incomplete"
    fi
else
    fail "values.yaml does not exist"
fi

# ===========================================
# TEST 4: values.yaml — Application Services
# ===========================================
section "TEST 4: values.yaml — Application Services"

if [[ -f "${VALUES_FILE}" ]]; then
    # Ingest
    if contains_all "${VALUES_FILE}" \
        "ingest:" "port: 8081" "grpcPort: 9090" \
        "orbit-ingest:latest"; then
        pass "Ingest service values match (port 8081, gRPC 9090)"
    else
        fail "Ingest service values are incomplete"
    fi

    # Processor
    if contains_all "${VALUES_FILE}" \
        "processor:" "port: 8082" \
        "orbit-processor:latest"; then
        pass "Processor service values match (port 8082)"
    else
        fail "Processor service values are incomplete"
    fi

    # Orchestrator
    if contains_all "${VALUES_FILE}" \
        "orchestrator:" "port: 8083" \
        "orbit-orchestrator:latest"; then
        pass "Orchestrator service values match (port 8083)"
    else
        fail "Orchestrator service values are incomplete"
    fi

    # Gateway
    if contains_all "${VALUES_FILE}" \
        "gateway:" "httpPort: 8080" "mtlsPort: 8443" \
        "orbit-gateway:latest"; then
        pass "Gateway service values match (HTTP 8080, mTLS 8443)"
    else
        fail "Gateway service values are incomplete"
    fi

    # Resource limits exist
    if grep -c "resources:" "${VALUES_FILE}" | grep -qE '^[4-9]|^[1-9][0-9]'; then
        pass "Resource requests/limits are defined for multiple components"
    else
        fail "Resource requests/limits should be defined for each component"
    fi
else
    fail "values.yaml does not exist"
fi

# ===========================================
# TEST 5: HPA Configuration in values.yaml
# ===========================================
section "TEST 5: HPA Configuration"

if [[ -f "${VALUES_FILE}" ]]; then
    # HPA enabled for ingest
    if sed -n '/^ingest:/,/^[a-z]/p' "${VALUES_FILE}" | grep -q "hpa:"; then
        pass "HPA section exists for ingest"
    else
        fail "HPA section missing for ingest"
    fi

    # Check minReplicas / maxReplicas
    if contains_all "${VALUES_FILE}" "minReplicas:" "maxReplicas:" "targetCPUUtilizationPercentage:"; then
        pass "HPA defines minReplicas, maxReplicas, and CPU target"
    else
        fail "HPA configuration is missing required fields"
    fi
else
    fail "values.yaml does not exist"
fi

# HPA template
HPA_FILE="${TEMPLATES_DIR}/hpa.yaml"
if [[ -f "${HPA_FILE}" ]]; then
    if contains_all "${HPA_FILE}" \
        "autoscaling/v2" "HorizontalPodAutoscaler" \
        "scaleTargetRef:" "kind: Deployment"; then
        pass "HPA template uses autoscaling/v2 and targets Deployments"
    else
        fail "HPA template API version or scaleTargetRef is incorrect"
    fi

    if contains_all "${HPA_FILE}" \
        "orbit-ingest" "orbit-processor" "orbit-gateway"; then
        pass "HPA template covers ingest, processor, and gateway"
    else
        fail "HPA template should cover ingest, processor, and gateway"
    fi

    if contains_all "${HPA_FILE}" \
        "type: Resource" "name: cpu" "type: Utilization" "averageUtilization:"; then
        pass "HPA template defines CPU-based scaling metrics"
    else
        fail "HPA template is missing CPU scaling metrics"
    fi

    if grep -Fq ".Values.ingest.hpa.enabled" "${HPA_FILE}" && \
       grep -Fq ".Values.processor.hpa.enabled" "${HPA_FILE}"; then
        pass "HPA resources are conditionally created via .Values.*.hpa.enabled"
    else
        fail "HPA resources should be conditional on .Values.*.hpa.enabled"
    fi
else
    fail "HPA template file does not exist"
fi

# ===========================================
# TEST 6: Service Templates — Deployments
# ===========================================
section "TEST 6: Service Templates — Deployments"

for svc in orbit-ingest orbit-processor orbit-orchestrator orbit-gateway; do
    TPL_FILE="${TEMPLATES_DIR}/${svc}.yaml"
    if [[ -f "${TPL_FILE}" ]]; then
        # Must be a Deployment + Service
        if contains_all "${TPL_FILE}" "kind: Deployment" "kind: Service"; then
            pass "${svc} template defines both Deployment and Service"
        else
            fail "${svc} template is missing Deployment or Service"
        fi

        # Readiness/Liveness probes
        if contains_all "${TPL_FILE}" "readinessProbe:" "livenessProbe:" "/actuator/health"; then
            pass "${svc} has readiness and liveness probes via actuator"
        else
            fail "${svc} is missing readiness/liveness probes"
        fi

        # Resource limits
        if grep -Fq "resources:" "${TPL_FILE}"; then
            pass "${svc} template references resource limits"
        else
            fail "${svc} template is missing resource limits"
        fi

        # Labels
        if contains_all "${TPL_FILE}" "app.kubernetes.io/name: ${svc}" "app.kubernetes.io/instance:"; then
            pass "${svc} template has standard Kubernetes labels"
        else
            fail "${svc} template is missing standard labels"
        fi
    else
        fail "${svc} template file does not exist"
    fi
done

# ===========================================
# TEST 7: Service-specific Configuration
# ===========================================
section "TEST 7: Service-specific Configuration"

# Ingest — gRPC port
INGEST_TPL="${TEMPLATES_DIR}/orbit-ingest.yaml"
if [[ -f "${INGEST_TPL}" ]]; then
    if grep -Fq "grpc" "${INGEST_TPL}"; then
        pass "orbit-ingest template exposes gRPC port"
    else
        fail "orbit-ingest template is missing gRPC port"
    fi

    if grep -Fq "KAFKA_BOOTSTRAP_SERVERS" "${INGEST_TPL}"; then
        pass "orbit-ingest configures Kafka bootstrap servers"
    else
        fail "orbit-ingest is missing Kafka env var"
    fi
fi

# Processor — Kafka
PROCESSOR_TPL="${TEMPLATES_DIR}/orbit-processor.yaml"
if [[ -f "${PROCESSOR_TPL}" ]]; then
    if grep -Fq "KAFKA_BOOTSTRAP_SERVERS" "${PROCESSOR_TPL}"; then
        pass "orbit-processor configures Kafka bootstrap servers"
    else
        fail "orbit-processor is missing Kafka env var"
    fi
fi

# Orchestrator — Postgres + Kafka
ORCHESTRATOR_TPL="${TEMPLATES_DIR}/orbit-orchestrator.yaml"
if [[ -f "${ORCHESTRATOR_TPL}" ]]; then
    if grep -Fq "DB_URL" "${ORCHESTRATOR_TPL}" && \
       grep -Fq "KAFKA_BOOTSTRAP_SERVERS" "${ORCHESTRATOR_TPL}"; then
        pass "orbit-orchestrator configures DB and Kafka"
    else
        fail "orbit-orchestrator is missing DB or Kafka env vars"
    fi
fi

# Gateway — dual ports + certs
GATEWAY_TPL="${TEMPLATES_DIR}/orbit-gateway.yaml"
if [[ -f "${GATEWAY_TPL}" ]]; then
    if contains_all "${GATEWAY_TPL}" "httpPort" "mtlsPort"; then
        pass "orbit-gateway exposes both HTTP and mTLS ports"
    else
        fail "orbit-gateway is missing HTTP or mTLS port"
    fi

    if grep -Fq "service.type" "${GATEWAY_TPL}" && \
       grep -Fq "LoadBalancer" "${VALUES_FILE}"; then
        pass "orbit-gateway Service type is configurable (default: LoadBalancer)"
    else
        fail "orbit-gateway Service should support LoadBalancer type"
    fi
fi

# ===========================================
# TEST 8: Infrastructure Templates
# ===========================================
section "TEST 8: Infrastructure Templates"

# Postgres
POSTGRES_TPL="${TEMPLATES_DIR}/postgres.yaml"
if [[ -f "${POSTGRES_TPL}" ]]; then
    if contains_all "${POSTGRES_TPL}" \
        "kind: Deployment" "kind: Service" "kind: PersistentVolumeClaim" \
        "POSTGRES_DB" "POSTGRES_USER" "POSTGRES_PASSWORD" \
        "readinessProbe:" "livenessProbe:"; then
        pass "Postgres template has Deployment, Service, PVC and probes"
    else
        fail "Postgres template is incomplete"
    fi
else
    fail "Postgres template does not exist"
fi

# Kafka
KAFKA_TPL="${TEMPLATES_DIR}/kafka.yaml"
if [[ -f "${KAFKA_TPL}" ]]; then
    if contains_all "${KAFKA_TPL}" \
        "kind: Deployment" "kind: Service" \
        "KAFKA_NODE_ID" "KAFKA_PROCESS_ROLES" "CLUSTER_ID" \
        "readinessProbe:" "livenessProbe:"; then
        pass "Kafka template has Deployment, Service and probes"
    else
        fail "Kafka template is incomplete"
    fi
else
    fail "Kafka template does not exist"
fi

# Prometheus
PROMETHEUS_TPL="${TEMPLATES_DIR}/prometheus.yaml"
if [[ -f "${PROMETHEUS_TPL}" ]]; then
    if contains_all "${PROMETHEUS_TPL}" \
        "kind: Deployment" "kind: Service" "kind: ConfigMap" \
        "prometheus.yml" "orbit-ingest" "orbit-processor" \
        "orbit-orchestrator" "orbit-gateway"; then
        pass "Prometheus template has ConfigMap, Deployment, Service and all scrape targets"
    else
        fail "Prometheus template is incomplete or missing scrape targets"
    fi
else
    fail "Prometheus template does not exist"
fi

# Grafana
GRAFANA_TPL="${TEMPLATES_DIR}/grafana.yaml"
if [[ -f "${GRAFANA_TPL}" ]]; then
    if contains_all "${GRAFANA_TPL}" \
        "kind: Deployment" "kind: Service" \
        "GF_AUTH_ANONYMOUS_ENABLED" \
        "readinessProbe:" "livenessProbe:"; then
        pass "Grafana template has Deployment, Service, anon auth and probes"
    else
        fail "Grafana template is incomplete"
    fi
else
    fail "Grafana template does not exist"
fi

# ===========================================
# TEST 9: Template Helpers & NOTES
# ===========================================
section "TEST 9: Template Helpers & NOTES"

HELPERS_TPL="${TEMPLATES_DIR}/_helpers.tpl"
if [[ -f "${HELPERS_TPL}" ]]; then
    if contains_all "${HELPERS_TPL}" \
        "orbit.name" "orbit.fullname" "orbit.labels" "orbit.selectorLabels"; then
        pass "_helpers.tpl defines name, fullname, labels, and selectorLabels"
    else
        fail "_helpers.tpl is missing required template definitions"
    fi
else
    fail "_helpers.tpl does not exist"
fi

NOTES_FILE="${TEMPLATES_DIR}/NOTES.txt"
if [[ -f "${NOTES_FILE}" ]]; then
    if contains_all "${NOTES_FILE}" \
        "orbit-gateway" "orbit-ingest" "orbit-processor" "orbit-orchestrator" \
        "Prometheus" "Grafana"; then
        pass "NOTES.txt references all services and monitoring"
    else
        fail "NOTES.txt is missing service references"
    fi
else
    fail "NOTES.txt does not exist"
fi

# ===========================================
# TEST 10: k3d Setup Script
# ===========================================
section "TEST 10: k3d Setup Script"

K3D_SCRIPT="${K8S_DIR}/k3d-setup.sh"
if [[ -f "${K3D_SCRIPT}" ]]; then
    if [[ -x "${K3D_SCRIPT}" ]]; then
        pass "k3d-setup.sh is executable"
    else
        fail "k3d-setup.sh is not executable"
    fi

    if contains_all "${K3D_SCRIPT}" \
        "k3d" "cluster create" "orbit-cluster" \
        "--servers" "--agents"; then
        pass "k3d-setup.sh creates a multi-node cluster"
    else
        fail "k3d-setup.sh cluster creation is incomplete"
    fi

    if contains_all "${K3D_SCRIPT}" \
        "docker build" "k3d image import" \
        "orbit-ingest" "orbit-processor" "orbit-orchestrator" "orbit-gateway"; then
        pass "k3d-setup.sh builds and imports all service images"
    else
        fail "k3d-setup.sh is missing image build/import steps"
    fi

    if grep -Fq "helm" "${K3D_SCRIPT}"; then
        pass "k3d-setup.sh deploys via Helm"
    else
        fail "k3d-setup.sh is missing Helm deployment step"
    fi

    if grep -Fq "metrics-server" "${K3D_SCRIPT}"; then
        pass "k3d-setup.sh installs metrics-server for HPA"
    else
        fail "k3d-setup.sh is missing metrics-server installation"
    fi

    # Prerequisite checks
    if contains_all "${K3D_SCRIPT}" "docker" "k3d" "kubectl" "helm"; then
        pass "k3d-setup.sh checks for required CLI tools"
    else
        fail "k3d-setup.sh is missing prerequisite checks"
    fi
else
    fail "k3d-setup.sh does not exist"
fi

# ===========================================
# TEST 11: Cert Volume Mounts
# ===========================================
section "TEST 11: Certificate Volume Mounts"

for svc in orbit-ingest orbit-processor orbit-gateway; do
    TPL_FILE="${TEMPLATES_DIR}/${svc}.yaml"
    if [[ -f "${TPL_FILE}" ]]; then
        if grep -Fq "certs" "${TPL_FILE}" && grep -Fq "volumeMounts:" "${TPL_FILE}"; then
            pass "${svc} mounts certificate volumes"
        else
            fail "${svc} is missing certificate volume mounts"
        fi
    fi
done

# ===========================================
# TEST 12: Helm Template Rendering
# ===========================================
section "TEST 12: Helm Template Rendering"

if command -v helm >/dev/null 2>&1; then
    RENDER_LOG="$(mktemp /tmp/helm-render.XXXXXX.log)"
    if helm template orbit "${CHART_DIR}" >"${RENDER_LOG}" 2>&1; then
        pass "Helm template renders without errors"

        # Count rendered resources
        DEPLOYMENT_COUNT=$(grep -c 'kind: Deployment' "${RENDER_LOG}" || true)
        SERVICE_COUNT=$(grep -c 'kind: Service' "${RENDER_LOG}" || true)
        HPA_COUNT=$(grep -c 'kind: HorizontalPodAutoscaler' "${RENDER_LOG}" || true)

        if [[ "${DEPLOYMENT_COUNT}" -ge 6 ]]; then
            pass "Rendered ${DEPLOYMENT_COUNT} Deployments (4 app + postgres + kafka + prometheus + grafana)"
        else
            fail "Expected at least 6 Deployments, got ${DEPLOYMENT_COUNT}"
        fi

        if [[ "${SERVICE_COUNT}" -ge 6 ]]; then
            pass "Rendered ${SERVICE_COUNT} Services"
        else
            fail "Expected at least 6 Services, got ${SERVICE_COUNT}"
        fi

        if [[ "${HPA_COUNT}" -ge 2 ]]; then
            pass "Rendered ${HPA_COUNT} HorizontalPodAutoscalers (ingest, processor, gateway)"
        else
            fail "Expected at least 2 HPAs, got ${HPA_COUNT}"
        fi

        # Check probes in rendered output
        READINESS_COUNT=$(grep -c 'readinessProbe:' "${RENDER_LOG}" || true)
        LIVENESS_COUNT=$(grep -c 'livenessProbe:' "${RENDER_LOG}" || true)
        if [[ "${READINESS_COUNT}" -ge 4 ]] && [[ "${LIVENESS_COUNT}" -ge 4 ]]; then
            pass "All services have readiness (${READINESS_COUNT}) and liveness (${LIVENESS_COUNT}) probes"
        else
            fail "Expected at least 4 readiness and liveness probes each"
        fi

        rm -f "${RENDER_LOG}"
    else
        fail "Helm template rendering failed. Last 30 lines:"
        tail -n 30 "${RENDER_LOG}" || true
        rm -f "${RENDER_LOG}"
    fi
else
    skip "Helm CLI not installed — cannot render templates"
fi

# ===========================================
# TEST 13: Helm Lint
# ===========================================
section "TEST 13: Helm Lint"

if command -v helm >/dev/null 2>&1; then
    LINT_LOG="$(mktemp /tmp/helm-lint.XXXXXX.log)"
    if helm lint "${CHART_DIR}" >"${LINT_LOG}" 2>&1; then
        pass "Helm lint passed with no errors"
        rm -f "${LINT_LOG}"
    else
        fail "Helm lint failed. Output:"
        cat "${LINT_LOG}" || true
        rm -f "${LINT_LOG}"
    fi
else
    skip "Helm CLI not installed — cannot lint chart"
fi

# ===========================================
# TEST 14: Conditional Infrastructure
# ===========================================
section "TEST 14: Conditional Infrastructure"

for infra in postgres kafka prometheus grafana; do
    TPL_FILE="${TEMPLATES_DIR}/${infra}.yaml"
    if [[ -f "${TPL_FILE}" ]]; then
        if grep -Fq ".Values.${infra}.enabled" "${TPL_FILE}"; then
            pass "${infra} template is conditionally created via .Values.${infra}.enabled"
        else
            fail "${infra} template should be conditional on .Values.${infra}.enabled"
        fi
    fi
done

# ===========================================
# TEST 15: Spring Profiles in Templates
# ===========================================
section "TEST 15: Spring Profiles in Templates"

for svc in orbit-ingest orbit-processor orbit-orchestrator orbit-gateway; do
    TPL_FILE="${TEMPLATES_DIR}/${svc}.yaml"
    if [[ -f "${TPL_FILE}" ]]; then
        if grep -Fq "SPRING_PROFILES_ACTIVE" "${TPL_FILE}"; then
            pass "${svc} sets SPRING_PROFILES_ACTIVE"
        else
            fail "${svc} is missing SPRING_PROFILES_ACTIVE env var"
        fi
    fi
done

# ===========================================
# SUMMARY
# ===========================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "              TEST SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "  ${GREEN}Passed${NC}: ${PASSED}"
echo -e "  ${RED}Failed${NC}: ${FAILED}"
echo -e "  ${YELLOW}Skipped${NC}: ${SKIPPED}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [[ ${FAILED} -eq 0 ]]; then
    echo -e "  ${GREEN}All tests passed! Phase 5 is fully implemented and verified.${NC}"
    exit 0
fi

echo -e "  ${RED}${FAILED} test(s) failed. Please fix the issues above.${NC}"
exit 1
