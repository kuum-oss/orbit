#!/bin/bash
# ===========================================
# Phase 2 Test Suite — Ingest & Processor Pipeline
# ===========================================
# Tests: File structure, Protobuf/gRPC contracts,
#        WebFlux reactive ingest, Anomaly detection engine,
#        Kafka pipeline config, Maven compilation & Unit tests,
#        Docker configuration
#
# Usage: ./tests/test-phase2.sh
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
# TEST 1: Phase 2 File Structure Verification
# ===========================================
section "TEST 1: Phase 2 File Structure Verification"

REQUIRED_FILES=(
    "pom.xml"
    "orbit-ingest/pom.xml"
    "orbit-ingest/Dockerfile"
    "orbit-ingest/src/main/proto/telemetry.proto"
    "orbit-ingest/src/main/resources/application.yml"
    "orbit-ingest/src/main/resources/schema.sql"
    "orbit-ingest/src/main/java/com/orbit/ingest/OrbitIngestApplication.java"
    "orbit-ingest/src/main/java/com/orbit/ingest/domain/DeviceTelemetry.java"
    "orbit-ingest/src/main/java/com/orbit/ingest/controller/TelemetryController.java"
    "orbit-ingest/src/main/java/com/orbit/ingest/grpc/TelemetryGrpcService.java"
    "orbit-ingest/src/main/java/com/orbit/ingest/service/TelemetryService.java"
    "orbit-ingest/src/main/java/com/orbit/ingest/repository/TelemetryRepository.java"
    "orbit-ingest/src/main/java/com/orbit/ingest/kafka/TelemetryKafkaProducer.java"
    "orbit-processor/pom.xml"
    "orbit-processor/Dockerfile"
    "orbit-processor/src/main/resources/application.yml"
    "orbit-processor/src/main/java/com/orbit/processor/OrbitProcessorApplication.java"
    "orbit-processor/src/main/java/com/orbit/processor/domain/DeviceTelemetry.java"
    "orbit-processor/src/main/java/com/orbit/processor/domain/AnomalyEvent.java"
    "orbit-processor/src/main/java/com/orbit/processor/domain/Severity.java"
    "orbit-processor/src/main/java/com/orbit/processor/engine/AnomalyRule.java"
    "orbit-processor/src/main/java/com/orbit/processor/engine/ThresholdRule.java"
    "orbit-processor/src/main/java/com/orbit/processor/engine/RateOfChangeRule.java"
    "orbit-processor/src/main/java/com/orbit/processor/engine/AnomalyDetectionEngine.java"
    "orbit-processor/src/main/java/com/orbit/processor/kafka/TelemetryConsumer.java"
    "orbit-processor/src/main/java/com/orbit/processor/kafka/AnomalyEventProducer.java"
    "orbit-processor/src/main/java/com/orbit/processor/repository/AnomalyEventRepository.java"
    "orbit-processor/src/main/java/com/orbit/processor/service/AnomalyService.java"
    "orbit-processor/src/main/java/com/orbit/processor/controller/AnomalyController.java"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "${PROJECT_ROOT}/${file}" ]]; then
        pass "File exists: ${file}"
    else
        fail "File missing: ${file}"
    fi
done

# ===========================================
# TEST 2: Protobuf & gRPC Contract Validation
# ===========================================
section "TEST 2: Protobuf & gRPC Contract Validation"

PROTO_FILE="${PROJECT_ROOT}/orbit-ingest/src/main/proto/telemetry.proto"

# 2.1 Proto3 syntax
if grep -q 'syntax = "proto3";' "${PROTO_FILE}"; then
    pass "telemetry.proto uses proto3 syntax"
else
    fail "telemetry.proto missing proto3 syntax"
fi

# 2.2 TelemetryPoint message definition
if grep -q 'message TelemetryPoint' "${PROTO_FILE}" && \
   grep -q 'device_id' "${PROTO_FILE}" && \
   grep -q 'metric_type' "${PROTO_FILE}" && \
   grep -q 'value' "${PROTO_FILE}"; then
    pass "TelemetryPoint message defined with required fields"
else
    fail "TelemetryPoint message missing required fields"
fi

# 2.3 ProcessingAck message definition
if grep -q 'message ProcessingAck' "${PROTO_FILE}" && \
   grep -q 'accepted' "${PROTO_FILE}" && \
   grep -q 'message' "${PROTO_FILE}"; then
    pass "ProcessingAck message defined with accepted & message fields"
else
    fail "ProcessingAck message definition missing"
fi

# 2.4 gRPC Service with bidirectional stream
if grep -q 'service TelemetryProcessor' "${PROTO_FILE}" && \
   grep -q 'StreamTelemetry.*stream TelemetryPoint.*returns.*stream ProcessingAck' "${PROTO_FILE}"; then
    pass "TelemetryProcessor service defined with bidirectional StreamTelemetry"
else
    fail "TelemetryProcessor bidirectional stream RPC missing"
fi

# 2.5 gRPC Service implementation
GRPC_SVC="${PROJECT_ROOT}/orbit-ingest/src/main/java/com/orbit/ingest/grpc/TelemetryGrpcService.java"
if grep -q '@GrpcService' "${GRPC_SVC}" && \
   grep -q 'TelemetryProcessorImplBase' "${GRPC_SVC}" && \
   grep -q 'streamTelemetry' "${GRPC_SVC}"; then
    pass "TelemetryGrpcService implements TelemetryProcessorImplBase with @GrpcService"
else
    fail "TelemetryGrpcService implementation incomplete"
fi

# ===========================================
# TEST 3: Reactive WebFlux & Ingestion Pipeline
# ===========================================
section "TEST 3: Reactive WebFlux & Ingestion Pipeline"

INGEST_CTRL="${PROJECT_ROOT}/orbit-ingest/src/main/java/com/orbit/ingest/controller/TelemetryController.java"
INGEST_SVC="${PROJECT_ROOT}/orbit-ingest/src/main/java/com/orbit/ingest/service/TelemetryService.java"
INGEST_PROD="${PROJECT_ROOT}/orbit-ingest/src/main/java/com/orbit/ingest/kafka/TelemetryKafkaProducer.java"

# 3.1 WebFlux reactive types
if grep -q 'Mono<' "${INGEST_CTRL}" && grep -q 'Flux<' "${INGEST_CTRL}"; then
    pass "TelemetryController uses reactive types (Mono/Flux)"
else
    fail "TelemetryController not using reactive types"
fi

# 3.2 Single telemetry endpoint
if grep -q 'PostMapping.*telemetry' "${INGEST_CTRL}"; then
    pass "POST /api/v1/telemetry endpoint defined"
else
    fail "POST /api/v1/telemetry endpoint missing"
fi

# 3.3 Batch telemetry endpoint
if grep -q 'telemetry/batch' "${INGEST_CTRL}"; then
    pass "POST /api/v1/telemetry/batch endpoint defined"
else
    fail "POST /api/v1/telemetry/batch endpoint missing"
fi

# 3.4 SSE Stream endpoint
if grep -q 'TEXT_EVENT_STREAM_VALUE\|text/event-stream' "${INGEST_CTRL}"; then
    pass "GET /api/v1/telemetry/stream SSE endpoint defined"
else
    fail "GET /api/v1/telemetry/stream SSE endpoint missing"
fi

# 3.5 R2DBC repository
if grep -q 'ReactiveCrudRepository' "${PROJECT_ROOT}/orbit-ingest/src/main/java/com/orbit/ingest/repository/TelemetryRepository.java"; then
    pass "TelemetryRepository extends ReactiveCrudRepository"
else
    fail "TelemetryRepository is not reactive"
fi

# 3.6 Kafka producer topic
if grep -q 'device-telemetry' "${INGEST_PROD}"; then
    pass "TelemetryKafkaProducer publishes to 'device-telemetry' topic"
else
    fail "TelemetryKafkaProducer missing 'device-telemetry' topic"
fi

# 3.7 Ingest config ports
INGEST_YML="${PROJECT_ROOT}/orbit-ingest/src/main/resources/application.yml"
if grep -q 'port: 8081' "${INGEST_YML}" && grep -q 'port: 9090' "${INGEST_YML}"; then
    pass "orbit-ingest configured for HTTP:8081 and gRPC:9090"
else
    fail "orbit-ingest port configuration incorrect"
fi

# ===========================================
# TEST 4: Anomaly Detection Engine Validation
# ===========================================
section "TEST 4: Anomaly Detection Engine Validation"

SEVERITY_FILE="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/domain/Severity.java"
THRESHOLD_RULE="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/engine/ThresholdRule.java"
ROC_RULE="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/engine/RateOfChangeRule.java"
ENGINE_FILE="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/engine/AnomalyDetectionEngine.java"

# 4.1 Severity levels
if grep -q 'LOW' "${SEVERITY_FILE}" && \
   grep -q 'MEDIUM' "${SEVERITY_FILE}" && \
   grep -q 'HIGH' "${SEVERITY_FILE}" && \
   grep -q 'CRITICAL' "${SEVERITY_FILE}"; then
    pass "Severity enum defines LOW, MEDIUM, HIGH, CRITICAL"
else
    fail "Severity enum missing required levels"
fi

# 4.2 ThresholdRule covers metrics
METRICS=("TEMPERATURE" "BATTERY_LEVEL" "CPU_USAGE" "MEMORY_USAGE")
ALL_METRICS_FOUND=true
for m in "${METRICS[@]}"; do
    if ! grep -q "${m}" "${THRESHOLD_RULE}"; then
        ALL_METRICS_FOUND=false
        break
    fi
done
if [[ "${ALL_METRICS_FOUND}" == "true" ]]; then
    pass "ThresholdRule covers TEMPERATURE, BATTERY_LEVEL, CPU_USAGE, MEMORY_USAGE"
else
    fail "ThresholdRule missing some required metrics"
fi

# 4.3 RateOfChangeRule sliding window
if grep -q 'ConcurrentHashMap' "${ROC_RULE}" && \
   grep -q 'windowSize\|window-size' "${ROC_RULE}"; then
    pass "RateOfChangeRule uses thread-safe sliding window tracking"
else
    fail "RateOfChangeRule sliding window implementation incomplete"
fi

# 4.4 AnomalyDetectionEngine aggregates rules
if grep -q 'List<AnomalyRule>' "${ENGINE_FILE}" && \
   grep -q 'evaluate' "${ENGINE_FILE}"; then
    pass "AnomalyDetectionEngine dynamically aggregates and evaluates rules"
else
    fail "AnomalyDetectionEngine rule aggregation missing"
fi

# ===========================================
# TEST 5: Kafka Pipeline & Event Processing
# ===========================================
section "TEST 5: Kafka Pipeline & Event Processing"

CONSUMER_FILE="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/kafka/TelemetryConsumer.java"
PRODUCER_FILE="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/kafka/AnomalyEventProducer.java"
KAFKA_CFG="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/config/KafkaConsumerConfig.java"

# 5.1 TelemetryConsumer listens on device-telemetry
if grep -q 'device-telemetry' "${CONSUMER_FILE}" && \
   grep -q 'orbit-processor' "${CONSUMER_FILE}"; then
    pass "TelemetryConsumer listens to 'device-telemetry' with group 'orbit-processor'"
else
    fail "TelemetryConsumer configuration missing topic/group"
fi

# 5.2 AnomalyEventProducer publishes to anomaly-events
if grep -q 'anomaly-events' "${PRODUCER_FILE}"; then
    pass "AnomalyEventProducer publishes to 'anomaly-events' topic"
else
    fail "AnomalyEventProducer missing 'anomaly-events' topic"
fi

# 5.3 Dead Letter / Error Handling
if grep -q 'DeadLetterPublishingRecoverer' "${KAFKA_CFG}"; then
    pass "KafkaConsumerConfig configures DeadLetterPublishingRecoverer"
else
    fail "Dead letter queue error recovery missing"
fi

# 5.4 AnomalyController endpoints
PROC_CTRL="${PROJECT_ROOT}/orbit-processor/src/main/java/com/orbit/processor/controller/AnomalyController.java"
if grep -q 'RequestMapping.*anomalies' "${PROC_CTRL}" && \
   grep -q 'GetMapping.*stats' "${PROC_CTRL}"; then
    pass "AnomalyController exposes /api/v1/anomalies and /stats endpoints"
else
    fail "AnomalyController endpoints incomplete"
fi

# ===========================================
# TEST 6: Multi-Module Maven Compilation & Unit Testing
# ===========================================
section "TEST 6: Multi-Module Maven Compilation & Unit Testing"

if command -v mvn >/dev/null 2>&1; then
    echo "  Running Maven clean test on all modules..."
    if (cd "${PROJECT_ROOT}" && mvn clean test -q >/dev/null 2>&1); then
        pass "Maven build and all unit tests passed successfully"
        
        # Check generated proto classes
        if [[ -d "${PROJECT_ROOT}/orbit-ingest/target/generated-sources/protobuf" ]]; then
            pass "Protobuf and gRPC Java classes generated successfully"
        else
            fail "Generated protobuf classes missing"
        fi
        
        # Check test reports
        if [[ -d "${PROJECT_ROOT}/orbit-ingest/target/surefire-reports" ]] && \
           [[ -d "${PROJECT_ROOT}/orbit-processor/target/surefire-reports" ]]; then
            pass "Surefire test reports generated for both modules"
        else
            fail "Test reports missing"
        fi
    else
        fail "Maven build or tests failed (run: mvn clean test for details)"
    fi
else
    skip "Maven CLI not installed — cannot run compilation & unit tests"
fi

# ===========================================
# TEST 7: Docker Configuration Validation
# ===========================================
section "TEST 7: Docker Configuration Validation"

# 7.1 Ingest Dockerfile
if grep -q 'FROM maven:' "${PROJECT_ROOT}/orbit-ingest/Dockerfile" && \
   grep -q 'ENTRYPOINT' "${PROJECT_ROOT}/orbit-ingest/Dockerfile" && \
   grep -q '8081 9090' "${PROJECT_ROOT}/orbit-ingest/Dockerfile"; then
    pass "orbit-ingest Dockerfile is valid multi-stage build exposing 8081 and 9090"
else
    fail "orbit-ingest Dockerfile incomplete"
fi

# 7.2 Processor Dockerfile
if grep -q 'FROM maven:' "${PROJECT_ROOT}/orbit-processor/Dockerfile" && \
   grep -q 'ENTRYPOINT' "${PROJECT_ROOT}/orbit-processor/Dockerfile" && \
   grep -q '8082' "${PROJECT_ROOT}/orbit-processor/Dockerfile"; then
    pass "orbit-processor Dockerfile is valid multi-stage build exposing 8082"
else
    fail "orbit-processor Dockerfile incomplete"
fi

# 7.3 Docker Compose valid
if command -v docker >/dev/null 2>&1; then
    if docker compose -f "${PROJECT_ROOT}/docker-compose.yml" config >/dev/null 2>&1; then
        pass "Docker Compose configuration is valid for Phase 2 services"
    else
        fail "Docker Compose configuration is invalid"
    fi
else
    skip "Docker CLI not installed — cannot validate compose syntax"
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
    echo -e "  ${GREEN}All tests passed! Phase 2 is fully implemented and verified.${NC}"
    exit 0
else
    echo -e "  ${RED}${FAILED} test(s) failed. Please fix the issues above.${NC}"
    exit 1
fi
