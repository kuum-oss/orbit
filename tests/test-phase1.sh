#!/bin/bash
# ===========================================
# Phase 1 Test Suite — Orbit Infrastructure
# ===========================================
# Tests: Terraform validation, mTLS cert generation,
#        Docker Compose validation, LocalStack integration
#
# Usage: ./tests/test-phase1.sh
# ===========================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

# ===========================================
# TEST 1: File Structure Verification
# ===========================================
section "TEST 1: File Structure Verification"

REQUIRED_FILES=(
    "docker-compose.yml"
    ".env"
    "infra/terraform/main.tf"
    "infra/terraform/s3.tf"
    "infra/terraform/sqs.tf"
    "infra/terraform/sns.tf"
    "infra/terraform/outputs.tf"
    "infra/terraform/variables.tf"
    "infra/terraform/terraform.tfvars"
    "infra/certs/generate-certs.sh"
    "infra/certs/openssl.cnf"
    "infra/certs/.gitignore"
    "infra/localstack/init-aws.sh"
    "infra/prometheus/prometheus.yml"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "${PROJECT_ROOT}/${file}" ]]; then
        pass "File exists: ${file}"
    else
        fail "File missing: ${file}"
    fi
done

# ===========================================
# TEST 2: Terraform Configuration Validation
# ===========================================
section "TEST 2: Terraform Configuration Validation"

# 2.1 Check required provider
if grep -q 'hashicorp/aws' "${PROJECT_ROOT}/infra/terraform/main.tf"; then
    pass "Terraform uses hashicorp/aws provider"
else
    fail "Terraform missing hashicorp/aws provider"
fi

# 2.2 Check LocalStack endpoint configuration
if grep -q 'localstack_endpoint' "${PROJECT_ROOT}/infra/terraform/main.tf"; then
    pass "Terraform configured with LocalStack endpoint"
else
    fail "Terraform missing LocalStack endpoint config"
fi

# 2.3 Check skip validations (required for LocalStack)
if grep -q 'skip_credentials_validation' "${PROJECT_ROOT}/infra/terraform/main.tf" && \
   grep -q 'skip_metadata_api_check' "${PROJECT_ROOT}/infra/terraform/main.tf"; then
    pass "Terraform skips AWS validations (LocalStack compatible)"
else
    fail "Terraform missing skip validations for LocalStack"
fi

# 2.4 Check S3 bucket
if grep -q 'orbit-telemetry-archives' "${PROJECT_ROOT}/infra/terraform/s3.tf"; then
    pass "S3 bucket 'orbit-telemetry-archives' defined"
else
    fail "S3 bucket 'orbit-telemetry-archives' not found"
fi

# 2.5 Check S3 versioning
if grep -q 'aws_s3_bucket_versioning' "${PROJECT_ROOT}/infra/terraform/s3.tf"; then
    pass "S3 bucket versioning enabled"
else
    fail "S3 bucket versioning not configured"
fi

# 2.6 Check S3 lifecycle (Glacier transition)
if grep -q 'GLACIER' "${PROJECT_ROOT}/infra/terraform/s3.tf"; then
    pass "S3 lifecycle rule for GLACIER transition defined"
else
    fail "S3 GLACIER lifecycle rule not found"
fi

# 2.7 Check SQS DLQ
if grep -q 'orbit-telemetry-dlq' "${PROJECT_ROOT}/infra/terraform/sqs.tf"; then
    pass "SQS DLQ 'orbit-telemetry-dlq' defined"
else
    fail "SQS DLQ not found"
fi

# 2.8 Check SQS main queue
if grep -q 'orbit-telemetry-queue' "${PROJECT_ROOT}/infra/terraform/sqs.tf"; then
    pass "SQS main queue 'orbit-telemetry-queue' defined"
else
    fail "SQS main queue not found"
fi

# 2.9 Check SQS redrive policy
if grep -q 'redrive_policy' "${PROJECT_ROOT}/infra/terraform/sqs.tf" && \
   grep -q 'maxReceiveCount' "${PROJECT_ROOT}/infra/terraform/sqs.tf"; then
    pass "SQS redrive policy with maxReceiveCount configured"
else
    fail "SQS redrive policy missing"
fi

# 2.10 Check SNS topic
if grep -q 'orbit-critical-alerts' "${PROJECT_ROOT}/infra/terraform/sns.tf"; then
    pass "SNS topic 'orbit-critical-alerts' defined"
else
    fail "SNS topic not found"
fi

# 2.11 Check SNS → SQS subscription
if grep -q 'aws_sns_topic_subscription' "${PROJECT_ROOT}/infra/terraform/sns.tf"; then
    pass "SNS → SQS subscription defined"
else
    fail "SNS subscription not found"
fi

# 2.12 Check outputs
if grep -q 's3_bucket_arn' "${PROJECT_ROOT}/infra/terraform/outputs.tf" && \
   grep -q 'sqs_queue_url' "${PROJECT_ROOT}/infra/terraform/outputs.tf" && \
   grep -q 'sns_topic_arn' "${PROJECT_ROOT}/infra/terraform/outputs.tf"; then
    pass "Terraform outputs for S3, SQS, SNS defined"
else
    fail "Terraform outputs incomplete"
fi

# 2.13 Check default tags
if grep -q 'Project' "${PROJECT_ROOT}/infra/terraform/main.tf" && \
   grep -q 'Environment' "${PROJECT_ROOT}/infra/terraform/main.tf"; then
    pass "Default tags (Project, Environment) configured"
else
    fail "Default tags missing"
fi

# 2.14 Terraform fmt check
if command -v terraform >/dev/null 2>&1; then
    if terraform -chdir="${PROJECT_ROOT}/infra/terraform" fmt -check -recursive >/dev/null 2>&1; then
        pass "Terraform files are properly formatted"
    else
        fail "Terraform formatting issues detected (run: terraform fmt)"
    fi
else
    skip "Terraform CLI not installed — cannot run fmt check"
fi

# 2.15 Terraform validate
if command -v terraform >/dev/null 2>&1; then
    cd "${PROJECT_ROOT}/infra/terraform"
    if terraform init -backend=false >/dev/null 2>&1 && terraform validate >/dev/null 2>&1; then
        pass "Terraform configuration is valid"
    else
        fail "Terraform validation failed"
    fi
    cd "${PROJECT_ROOT}"
else
    skip "Terraform CLI not installed — cannot run validate"
fi

# ===========================================
# TEST 3: mTLS Certificate Generation
# ===========================================
section "TEST 3: mTLS Certificate Generation"

CERT_SCRIPT="${PROJECT_ROOT}/infra/certs/generate-certs.sh"

# 3.1 Script has shebang
if head -1 "${CERT_SCRIPT}" | grep -q '#!/bin/bash'; then
    pass "generate-certs.sh has bash shebang"
else
    fail "generate-certs.sh missing bash shebang"
fi

# 3.2 Script uses strict mode
if grep -q 'set -euo pipefail' "${CERT_SCRIPT}"; then
    pass "generate-certs.sh uses strict mode (set -euo pipefail)"
else
    fail "generate-certs.sh missing strict mode"
fi

# 3.3 Script generates CA
if grep -q 'ca.key' "${CERT_SCRIPT}" && grep -q 'ca.crt' "${CERT_SCRIPT}"; then
    pass "Script generates Root CA (ca.key + ca.crt)"
else
    fail "Root CA generation not found in script"
fi

# 3.4 Script generates service certs
SERVICES=("orbit-ingest" "orbit-processor" "orbit-orchestrator" "orbit-gateway")
ALL_SERVICES_FOUND=true
for svc in "${SERVICES[@]}"; do
    if ! grep -q "${svc}" "${CERT_SCRIPT}"; then
        ALL_SERVICES_FOUND=false
        break
    fi
done
if [[ "${ALL_SERVICES_FOUND}" == "true" ]]; then
    pass "Script generates certs for all 4 services"
else
    fail "Some service certs are missing from script"
fi

# 3.5 Script creates PKCS12 keystores
if grep -q '.p12' "${CERT_SCRIPT}"; then
    pass "Script creates PKCS12 keystores (.p12)"
else
    fail "PKCS12 keystore creation not found"
fi

# 3.6 Script creates truststore
if grep -q 'truststore.p12' "${CERT_SCRIPT}"; then
    pass "Script creates truststore.p12"
else
    fail "truststore.p12 creation not found"
fi

# 3.7 Script uses SAN (Subject Alternative Names)
if grep -q 'subjectAltName\|alt_names\|SAN' "${CERT_SCRIPT}"; then
    pass "Script uses Subject Alternative Names (SAN)"
else
    fail "SAN configuration not found"
fi

# 3.8 Script is idempotent (cleans old certs)
if grep -q 'rm -rf' "${CERT_SCRIPT}"; then
    pass "Script is idempotent (removes old certs before generating)"
else
    fail "Script not idempotent — missing cleanup"
fi

# 3.9 .gitignore excludes generated dir
if grep -q 'generated' "${PROJECT_ROOT}/infra/certs/.gitignore"; then
    pass ".gitignore excludes generated/ directory"
else
    fail ".gitignore missing generated/ exclusion"
fi

# 3.10 Actually run cert generation (if openssl available)
if command -v openssl >/dev/null 2>&1; then
    echo "  Running certificate generation..."
    chmod +x "${CERT_SCRIPT}"
    if bash "${CERT_SCRIPT}" >/dev/null 2>&1; then
        pass "Certificate generation script executed successfully"

        GENERATED_DIR="${PROJECT_ROOT}/infra/certs/generated"

        # Verify CA files
        if [[ -f "${GENERATED_DIR}/ca.key" ]] && [[ -f "${GENERATED_DIR}/ca.crt" ]]; then
            pass "CA key and certificate generated"
        else
            fail "CA files missing after generation"
        fi

        # Verify CA validity (3650 days)
        if openssl x509 -in "${GENERATED_DIR}/ca.crt" -noout -text 2>/dev/null | grep -q "Issuer.*Orbit"; then
            pass "CA certificate has correct issuer (Orbit)"
        else
            fail "CA certificate issuer incorrect"
        fi

        # Verify service keystores
        for svc in "${SERVICES[@]}"; do
            if [[ -f "${GENERATED_DIR}/${svc}.p12" ]]; then
                pass "Keystore generated: ${svc}.p12"
            else
                fail "Keystore missing: ${svc}.p12"
            fi
        done

        # Verify truststore
        if [[ -f "${GENERATED_DIR}/truststore.p12" ]]; then
            pass "Truststore generated: truststore.p12"
        else
            fail "Truststore missing: truststore.p12"
        fi

        # Verify a service cert is signed by CA
        if [[ -f "${GENERATED_DIR}/orbit-ingest.crt" ]]; then
            if openssl verify -CAfile "${GENERATED_DIR}/ca.crt" "${GENERATED_DIR}/orbit-ingest.crt" >/dev/null 2>&1; then
                pass "orbit-ingest.crt is signed by CA (chain valid)"
            else
                fail "orbit-ingest.crt not signed by CA"
            fi
        fi

        # Verify SAN on a cert
        if openssl x509 -in "${GENERATED_DIR}/orbit-ingest.crt" -noout -text 2>/dev/null | grep -q 'DNS:orbit-ingest'; then
            pass "orbit-ingest.crt has correct SAN (DNS:orbit-ingest)"
        else
            fail "orbit-ingest.crt missing SAN"
        fi

    else
        fail "Certificate generation script failed"
    fi
else
    skip "OpenSSL not installed — cannot run cert generation test"
fi

# ===========================================
# TEST 4: Docker Compose Validation
# ===========================================
section "TEST 4: Docker Compose Validation"

COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"

# 4.1 Required infrastructure services
INFRA_SERVICES=("localstack" "postgres" "kafka" "prometheus" "grafana")
for svc in "${INFRA_SERVICES[@]}"; do
    if grep -q "^\s*${svc}:" "${COMPOSE_FILE}" 2>/dev/null || grep -q "^  ${svc}:" "${COMPOSE_FILE}"; then
        pass "Infrastructure service defined: ${svc}"
    else
        fail "Infrastructure service missing: ${svc}"
    fi
done

# 4.2 Required app services (with profiles)
APP_SERVICES=("orbit-ingest" "orbit-processor" "orbit-orchestrator" "orbit-gateway")
for svc in "${APP_SERVICES[@]}"; do
    if grep -q "${svc}" "${COMPOSE_FILE}"; then
        pass "App service defined: ${svc}"
    else
        fail "App service missing: ${svc}"
    fi
done

# 4.3 App services use profiles
if grep -q 'profiles:' "${COMPOSE_FILE}" && grep -q '"app"' "${COMPOSE_FILE}"; then
    pass "App services use 'app' profile"
else
    fail "App services missing 'app' profile"
fi

# 4.4 Network configuration
if grep -q 'orbit-net' "${COMPOSE_FILE}"; then
    pass "Custom network 'orbit-net' defined"
else
    fail "Custom network 'orbit-net' missing"
fi

# 4.5 Healthchecks for infra services
if grep -c 'healthcheck' "${COMPOSE_FILE}" | grep -qE '[3-9]|[0-9]{2,}'; then
    pass "Healthchecks configured for services"
else
    fail "Insufficient healthchecks in compose"
fi

# 4.6 Postgres config
if grep -q 'POSTGRES_DB=orbit' "${COMPOSE_FILE}" && \
   grep -q 'postgres:16' "${COMPOSE_FILE}"; then
    pass "Postgres configured with correct DB and image version"
else
    fail "Postgres configuration incomplete"
fi

# 4.7 Kafka KRaft mode (no Zookeeper)
if grep -q 'KAFKA_PROCESS_ROLES' "${COMPOSE_FILE}" && \
   ! grep -q 'zookeeper' "${COMPOSE_FILE}"; then
    pass "Kafka runs in KRaft mode (no Zookeeper)"
else
    fail "Kafka not in KRaft mode or Zookeeper present"
fi

# 4.8 LocalStack volume for init script
if grep -q 'init-aws.sh' "${COMPOSE_FILE}"; then
    pass "LocalStack init script mounted"
else
    fail "LocalStack init script not mounted"
fi

# 4.9 Prometheus config mounted
if grep -q 'prometheus.yml' "${COMPOSE_FILE}"; then
    pass "Prometheus config mounted"
else
    fail "Prometheus config not mounted"
fi

# 4.10 mTLS cert volumes for app services
if grep -q 'certs' "${COMPOSE_FILE}"; then
    pass "Certificate volumes mounted for mTLS"
else
    fail "Certificate volume mounts missing"
fi

# 4.11 Docker Compose syntax validation
if command -v docker >/dev/null 2>&1; then
    if docker compose -f "${COMPOSE_FILE}" config >/dev/null 2>&1; then
        pass "Docker Compose syntax is valid (docker compose config)"
    else
        fail "Docker Compose syntax validation failed"
    fi
else
    skip "Docker not installed — cannot validate compose syntax"
fi

# ===========================================
# TEST 5: Prometheus Configuration
# ===========================================
section "TEST 5: Prometheus Configuration"

PROM_CONFIG="${PROJECT_ROOT}/infra/prometheus/prometheus.yml"

# 5.1 Has scrape configs
if grep -q 'scrape_configs' "${PROM_CONFIG}"; then
    pass "Prometheus has scrape_configs section"
else
    fail "Prometheus missing scrape_configs"
fi

# 5.2 Targets orbit services
for svc in "orbit-ingest" "orbit-processor" "orbit-orchestrator" "orbit-gateway"; do
    if grep -q "${svc}" "${PROM_CONFIG}"; then
        pass "Prometheus scrapes ${svc}"
    else
        fail "Prometheus missing scrape target: ${svc}"
    fi
done

# 5.3 Uses actuator/prometheus path
if grep -q 'actuator/prometheus\|/actuator/prometheus' "${PROM_CONFIG}"; then
    pass "Prometheus uses /actuator/prometheus metrics path"
else
    fail "Prometheus missing actuator metrics path"
fi

# ===========================================
# TEST 6: LocalStack Init Script
# ===========================================
section "TEST 6: LocalStack Init Script"

INIT_SCRIPT="${PROJECT_ROOT}/infra/localstack/init-aws.sh"

# 6.1 Creates S3 bucket
if grep -q 'orbit-telemetry-archives' "${INIT_SCRIPT}"; then
    pass "Init creates S3 bucket orbit-telemetry-archives"
else
    fail "Init missing S3 bucket creation"
fi

# 6.2 Creates SQS queues
if grep -q 'orbit-telemetry-dlq' "${INIT_SCRIPT}" && \
   grep -q 'orbit-telemetry-queue' "${INIT_SCRIPT}"; then
    pass "Init creates both SQS queues (main + DLQ)"
else
    fail "Init missing SQS queue creation"
fi

# 6.3 Creates SNS topic
if grep -q 'orbit-critical-alerts' "${INIT_SCRIPT}"; then
    pass "Init creates SNS topic orbit-critical-alerts"
else
    fail "Init missing SNS topic creation"
fi

# 6.4 Consistent naming with Terraform
TF_NAMES=("orbit-telemetry-archives" "orbit-telemetry-dlq" "orbit-telemetry-queue" "orbit-critical-alerts")
ALL_CONSISTENT=true
for name in "${TF_NAMES[@]}"; do
    if ! grep -q "${name}" "${INIT_SCRIPT}"; then
        ALL_CONSISTENT=false
        break
    fi
done
if [[ "${ALL_CONSISTENT}" == "true" ]]; then
    pass "Init script resource names consistent with Terraform"
else
    fail "Init script has naming inconsistencies with Terraform"
fi

# ===========================================
# TEST 7: Environment File
# ===========================================
section "TEST 7: Environment File"

ENV_FILE="${PROJECT_ROOT}/.env"

if grep -q 'COMPOSE_PROJECT_NAME=orbit' "${ENV_FILE}"; then
    pass ".env has COMPOSE_PROJECT_NAME=orbit"
else
    fail ".env missing COMPOSE_PROJECT_NAME"
fi

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
    echo -e "  ${GREEN}All tests passed! Phase 1 infrastructure is ready.${NC}"
    exit 0
else
    echo -e "  ${RED}${FAILED} test(s) failed. Please fix the issues above.${NC}"
    exit 1
fi
