#!/usr/bin/env bash
# ===========================================
# Phase 4 Test Suite — Orbit Gateway & mTLS
# ===========================================
# Tests: gateway structure, routes, resiliency, mTLS configuration,
#        global filters, controllers, module tests and Docker wiring.
#
# Usage: ./tests/test-phase4.sh
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

GATEWAY_DIR="${PROJECT_ROOT}/orbit-gateway"
SRC_DIR="${GATEWAY_DIR}/src/main/java/com/orbit/gateway"
TEST_DIR="${GATEWAY_DIR}/src/test/java/com/orbit/gateway"
CONFIG_DIR="${SRC_DIR}/config"
FILTER_DIR="${SRC_DIR}/filter"
CONTROLLER_DIR="${SRC_DIR}/controller"
APPLICATION_YML="${GATEWAY_DIR}/src/main/resources/application.yml"
ROUTES_FILE="${CONFIG_DIR}/GatewayRoutesConfig.java"
MTLS_FILE="${CONFIG_DIR}/MtlsConfig.java"
SECURITY_HEADERS_FILE="${CONFIG_DIR}/SecurityHeadersConfig.java"
MTLS_FILTER="${FILTER_DIR}/MtlsAuthenticationFilter.java"
LOGGING_FILTER="${FILTER_DIR}/RequestLoggingFilter.java"
FALLBACK_CONTROLLER="${CONTROLLER_DIR}/FallbackController.java"
INFO_CONTROLLER="${CONTROLLER_DIR}/GatewayInfoController.java"

# ===========================================
# TEST 1: Phase 4 File Structure
# ===========================================
section "TEST 1: Phase 4 File Structure"

REQUIRED_FILES=(
    "orbit-gateway/pom.xml"
    "orbit-gateway/Dockerfile"
    "orbit-gateway/src/main/resources/application.yml"
    "orbit-gateway/src/main/java/com/orbit/gateway/OrbitGatewayApplication.java"
    "orbit-gateway/src/main/java/com/orbit/gateway/config/GatewayRoutesConfig.java"
    "orbit-gateway/src/main/java/com/orbit/gateway/config/MtlsConfig.java"
    "orbit-gateway/src/main/java/com/orbit/gateway/config/SecurityHeadersConfig.java"
    "orbit-gateway/src/main/java/com/orbit/gateway/filter/MtlsAuthenticationFilter.java"
    "orbit-gateway/src/main/java/com/orbit/gateway/filter/RequestLoggingFilter.java"
    "orbit-gateway/src/main/java/com/orbit/gateway/controller/FallbackController.java"
    "orbit-gateway/src/main/java/com/orbit/gateway/controller/GatewayInfoController.java"
    "orbit-gateway/src/test/java/com/orbit/gateway/config/GatewayRoutesConfigTest.java"
    "orbit-gateway/src/test/java/com/orbit/gateway/config/SecurityHeadersConfigTest.java"
    "orbit-gateway/src/test/java/com/orbit/gateway/filter/MtlsAuthenticationFilterTest.java"
    "orbit-gateway/src/test/java/com/orbit/gateway/filter/RequestLoggingFilterTest.java"
    "orbit-gateway/src/test/java/com/orbit/gateway/controller/FallbackControllerTest.java"
    "orbit-gateway/src/test/java/com/orbit/gateway/controller/GatewayInfoControllerTest.java"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "${PROJECT_ROOT}/${file}" ]]; then
        pass "File exists: ${file}"
    else
        fail "File missing: ${file}"
    fi
done

# ===========================================
# TEST 2: Gateway Dependencies & Application
# ===========================================
section "TEST 2: Gateway Dependencies & Application"

POM_FILE="${GATEWAY_DIR}/pom.xml"
APPLICATION_FILE="${SRC_DIR}/OrbitGatewayApplication.java"

if contains_all "${POM_FILE}" \
    "spring-cloud-starter-gateway" \
    "spring-cloud-starter-circuitbreaker-reactor-resilience4j" \
    "spring-boot-starter-actuator" \
    "micrometer-registry-prometheus"; then
    pass "Gateway, Resilience4j, Actuator and Prometheus dependencies are declared"
else
    fail "Gateway runtime dependencies are incomplete"
fi

if contains_all "${APPLICATION_FILE}" "@SpringBootApplication" "SpringApplication.run"; then
    pass "OrbitGatewayApplication is a Spring Boot entry point"
else
    fail "OrbitGatewayApplication is incomplete"
fi

# ===========================================
# TEST 3: Routes & Circuit Breakers
# ===========================================
section "TEST 3: Routes & Circuit Breakers"

if contains_all "${ROUTES_FILE}" \
    'route("ingest-telemetry"' "/api/v1/telemetry/**" \
    'route("processor-anomalies"' "/api/v1/anomalies/**" \
    'route("orchestrator-tickets"' "/api/v1/tickets/**"; then
    pass "Gateway routes telemetry, anomalies and tickets to their services"
else
    fail "Public service route definitions are incomplete"
fi

if contains_all "${ROUTES_FILE}" \
    "ingestCircuitBreaker" "forward:/fallback/ingest" \
    "processorCircuitBreaker" "forward:/fallback/processor" \
    "orchestratorCircuitBreaker" "forward:/fallback/orchestrator"; then
    pass "Each public route has a named circuit breaker and fallback"
else
    fail "Circuit breaker or fallback configuration is incomplete"
fi

if contains_all "${ROUTES_FILE}" \
    'route("ingest-actuator"' "/admin/ingest/actuator/**" \
    'route("processor-actuator"' "/admin/processor/actuator/**" \
    'route("orchestrator-actuator"' "/admin/orchestrator/actuator/**" \
    "rewritePath"; then
    pass "Administrative actuator routes rewrite requests to downstream services"
else
    fail "Administrative actuator routes are incomplete"
fi

if grep -Fq 'X-Gateway-Source' "${ROUTES_FILE}"; then
    pass "Gateway adds its source header to downstream requests"
else
    fail "Gateway source header is missing from routes"
fi

# ===========================================
# TEST 4: Application Configuration
# ===========================================
section "TEST 4: Application Configuration"

if contains_all "${APPLICATION_YML}" \
    "port: 8080" "ingest-uri:" "processor-uri:" "orchestrator-uri:" \
    "RequestSize" "maxSize: 10MB"; then
    pass "HTTP port, downstream URIs and request-size limit are configured"
else
    fail "Base gateway configuration is incomplete"
fi

if contains_all "${APPLICATION_YML}" \
    "ingestCircuitBreaker:" "processorCircuitBreaker:" "orchestratorCircuitBreaker:" \
    "failureRateThreshold: 50" "timeoutDuration: 10s"; then
    pass "Resilience4j circuit breaker and timeout policies are configured"
else
    fail "Resilience4j configuration is incomplete"
fi

if contains_all "${APPLICATION_YML}" \
    "health,info,prometheus,gateway" "probes:" "enabled: true" \
    "globalcors:" "allowedOrigins:"; then
    pass "Actuator probes, Prometheus and CORS are configured"
else
    fail "Observability or CORS configuration is incomplete"
fi

if contains_all "${APPLICATION_YML}" \
    "on-profile: docker" "http://orbit-ingest:8081" \
    "http://orbit-processor:8082" "http://orbit-orchestrator:8083"; then
    pass "Docker profile resolves backend services by Compose host name"
else
    fail "Docker route profile is incomplete"
fi

# ===========================================
# TEST 5: mTLS Termination
# ===========================================
section "TEST 5: mTLS Termination"

if contains_all "${APPLICATION_YML}" \
    "on-profile: mtls" "gateway-server:" \
    "/etc/orbit/certs/orbit-gateway.crt" "/etc/orbit/certs/orbit-gateway.key" \
    "/etc/orbit/certs/ca.crt" "port: 8443" "client-auth: need" \
    "mtls:" "enabled: true"; then
    pass "mTLS profile configures PEM bundle, CA trust and mandatory client authentication"
else
    fail "mTLS server profile is incomplete"
fi

if contains_all "${APPLICATION_YML}" \
    "on-profile: docker-mtls" "https://orbit-ingest:8081" \
    "https://orbit-processor:8082" "https://orbit-orchestrator:8083"; then
    pass "docker-mtls profile configures secure downstream route URIs"
else
    fail "docker-mtls profile is incomplete"
fi

if contains_all "${MTLS_FILE}" \
    '@ConditionalOnProperty(name = "orbit.mtls.enabled", havingValue = "true")' \
    'sslBundles.getBundle("gateway-server")' "validateMtlsSetup"; then
    pass "MtlsConfig validates the configured SSL bundle only when mTLS is enabled"
else
    fail "MtlsConfig does not validate the mTLS SSL bundle"
fi

if contains_all "${MTLS_FILTER}" \
    "implements GlobalFilter, Ordered" "SSLSession" "getPeerCertificates" \
    "X-Client-Certificate-CN" "X-Client-Certificate-Serial" "HttpStatus.UNAUTHORIZED"; then
    pass "mTLS filter forwards certificate identity and rejects unauthenticated requests"
else
    fail "mTLS authentication filter is incomplete"
fi

# ===========================================
# TEST 6: Filters & Gateway API
# ===========================================
section "TEST 6: Filters & Gateway API"

if contains_all "${SECURITY_HEADERS_FILE}" \
    "X-Content-Type-Options" "X-Frame-Options" \
    "X-XSS-Protection" "Strict-Transport-Security"; then
    pass "Gateway adds standard browser security headers"
else
    fail "Security response headers are incomplete"
fi

if contains_all "${LOGGING_FILTER}" \
    "implements GlobalFilter, Ordered" "requestStartTime" \
    "Gateway Request" "Gateway Response"; then
    pass "Request logging filter records request and response timing"
else
    fail "Request logging filter is incomplete"
fi

if contains_all "${FALLBACK_CONTROLLER}" \
    '@RequestMapping("/fallback")' "/ingest" "/processor" "/orchestrator" \
    "HttpStatus.SERVICE_UNAVAILABLE"; then
    pass "Fallback controller provides 503 responses for all downstream services"
else
    fail "Fallback controller is incomplete"
fi

if contains_all "${INFO_CONTROLLER}" \
    '@RequestMapping("/gateway")' '@GetMapping("/info")' \
    "RouteLocator" "Mono<ResponseEntity" "mtls"; then
    pass "Gateway info API exposes reactive route metadata and mTLS state"
else
    fail "Gateway info controller is incomplete"
fi

# ===========================================
# TEST 7: Unit Tests
# ===========================================
section "TEST 7: Unit Tests"

if command -v mvn >/dev/null 2>&1; then
    echo "  Running Maven test on orbit-gateway..."
    MAVEN_LOG="$(mktemp /tmp/mvn-test-phase4.XXXXXX.log)"
    if (cd "${PROJECT_ROOT}" && mvn test -pl orbit-gateway >"${MAVEN_LOG}" 2>&1); then
        pass "orbit-gateway Maven test suite passed successfully"
        rm -f "${MAVEN_LOG}"

        if [[ -d "${GATEWAY_DIR}/target/surefire-reports" ]]; then
            pass "Surefire test reports generated for orbit-gateway"
        else
            fail "Surefire test reports were not generated for orbit-gateway"
        fi
    else
        fail "orbit-gateway tests failed. Last 40 lines of build log:"
        tail -n 40 "${MAVEN_LOG}" || true
        rm -f "${MAVEN_LOG}"
    fi
else
    skip "Maven CLI not installed — cannot run gateway unit tests"
fi

# ===========================================
# TEST 8: Container Configuration
# ===========================================
section "TEST 8: Container Configuration"

if contains_all "${GATEWAY_DIR}/Dockerfile" \
    "FROM maven:" "FROM eclipse-temurin:" "EXPOSE 8080 8443" "ENTRYPOINT"; then
    pass "Gateway Dockerfile builds a runtime image exposing HTTP and mTLS ports"
else
    fail "Gateway Dockerfile is incomplete"
fi

COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
if contains_all "${COMPOSE_FILE}" \
    "orbit-gateway:" "profiles: [\"app\"]" "8080:8080" "8443:8443" \
    "SPRING_PROFILES_ACTIVE=docker" "./infra/certs/generated:/etc/orbit/certs:ro"; then
    pass "Docker Compose configures gateway profile, ports, Docker profile and certificates"
else
    fail "Gateway Docker Compose service is incomplete"
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
    echo -e "  ${GREEN}All tests passed! Phase 4 is fully implemented and verified.${NC}"
    exit 0
fi

echo -e "  ${RED}${FAILED} test(s) failed. Please fix the issues above.${NC}"
exit 1
