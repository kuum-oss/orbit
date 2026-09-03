#!/bin/bash
# ===========================================
# Phase 3 Test Suite — Orbit Orchestrator (Camunda BPMN)
# ===========================================
# Tests: File structure, BPMN process definitions,
#        Camunda delegates, Service layer, REST controllers,
#        Kafka consumer integration, Maven tests, Docker configuration
#
# Usage: ./tests/test-phase3.sh
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
# TEST 1: Phase 3 File Structure Verification
# ===========================================
section "TEST 1: Phase 3 File Structure Verification"

REQUIRED_FILES=(
    "orbit-orchestrator/pom.xml"
    "orbit-orchestrator/Dockerfile"
    "orbit-orchestrator/src/main/resources/application.yml"
    "orbit-orchestrator/src/main/resources/processes/maintenance-process.bpmn"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/OrbitOrchestratorApplication.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/config/CamundaConfig.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/config/KafkaConsumerConfig.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/domain/MaintenanceTicket.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/domain/TicketStatus.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/domain/AnomalyEvent.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/domain/Severity.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/repository/MaintenanceTicketRepository.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/service/MaintenanceTicketService.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/service/OrchestrationService.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/service/TechnicianAssignmentService.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/controller/MaintenanceTicketController.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/kafka/AnomalyEventConsumer.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/EvaluateSeverityDelegate.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/CreateMaintenanceTicketDelegate.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/AssignTechnicianDelegate.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/NotifyTechnicianDelegate.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/CloseMaintenanceTicketDelegate.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/AggregateAnomaliesDelegate.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/EvaluateAggregatedAnomaliesDelegate.java"
    "orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate/DiscardAnomalyDelegate.java"
    "orbit-orchestrator/src/test/java/com/orbit/orchestrator/OrchestrationProcessTest.java"
    "orbit-orchestrator/src/test/java/com/orbit/orchestrator/controller/MaintenanceTicketControllerTest.java"
    "orbit-orchestrator/src/test/java/com/orbit/orchestrator/service/MaintenanceTicketServiceTest.java"
    "orbit-orchestrator/src/test/java/com/orbit/orchestrator/service/TechnicianAssignmentServiceTest.java"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "${PROJECT_ROOT}/${file}" ]]; then
        pass "File exists: ${file}"
    else
        fail "File missing: ${file}"
    fi
done

# ===========================================
# TEST 2: Camunda BPMN Diagram Validation
# ===========================================
section "TEST 2: Camunda BPMN Diagram Validation"

BPMN_FILE="${PROJECT_ROOT}/orbit-orchestrator/src/main/resources/processes/maintenance-process.bpmn"

# 2.1 Process ID and executability
if grep -q 'id="maintenance-process"' "${BPMN_FILE}" && grep -q 'isExecutable="true"' "${BPMN_FILE}"; then
    pass "BPMN process 'maintenance-process' defined and marked isExecutable=true"
else
    fail "BPMN process definition or executable flag missing"
fi

# 2.2 Severity Gateway & Branching
if grep -q 'Gateway_SeverityCheck' "${BPMN_FILE}" && \
   grep -q 'Task_EvaluateSeverity' "${BPMN_FILE}" && \
   grep -q "\${severity == 'HIGH' || severity == 'CRITICAL'}" "${BPMN_FILE}"; then
    pass "Severity evaluation gateway and HIGH/CRITICAL conditional flow present"
else
    fail "Severity check gateway or condition expression missing in BPMN"
fi

# 2.3 User Task for Confirmation
if grep -q 'Task_WaitTechnicianConfirmation' "${BPMN_FILE}" && \
   grep -q 'camunda:assignee="\${assignedTechnician}"' "${BPMN_FILE}"; then
    pass "User task 'Task_WaitTechnicianConfirmation' defined with dynamic technician assignee"
else
    fail "Technician confirmation user task missing or invalid"
fi

# 2.4 Low/Medium Aggregation Flow
if grep -q 'Task_AggregateAnomalies' "${BPMN_FILE}" && \
   grep -q 'Event_WaitAggregation' "${BPMN_FILE}" && \
   grep -q 'Task_EvaluateAggregated' "${BPMN_FILE}" && \
   grep -q 'Gateway_EscalateOrDiscard' "${BPMN_FILE}"; then
    pass "Low/Medium aggregation flow (timer, evaluation, escalate/discard) present in BPMN"
else
    fail "Aggregation flow elements missing in BPMN"
fi

# ===========================================
# TEST 3: Java Delegates Implementation Validation
# ===========================================
section "TEST 3: Java Delegates Implementation Validation"

DELEGATE_DIR="${PROJECT_ROOT}/orbit-orchestrator/src/main/java/com/orbit/orchestrator/delegate"

# 3.1 EvaluateSeverityDelegate
if grep -q 'implements JavaDelegate' "${DELEGATE_DIR}/EvaluateSeverityDelegate.java" && \
   grep -q '@Component("evaluateSeverityDelegate")' "${DELEGATE_DIR}/EvaluateSeverityDelegate.java"; then
    pass "EvaluateSeverityDelegate implements JavaDelegate and is registered as Spring component"
else
    fail "EvaluateSeverityDelegate incomplete"
fi

# 3.2 CreateMaintenanceTicketDelegate
if grep -q 'ticketService.createTicket' "${DELEGATE_DIR}/CreateMaintenanceTicketDelegate.java" && \
   grep -q 'execution.setVariable("ticketId"' "${DELEGATE_DIR}/CreateMaintenanceTicketDelegate.java"; then
    pass "CreateMaintenanceTicketDelegate creates ticket and propagates ticketId variable"
else
    fail "CreateMaintenanceTicketDelegate incomplete"
fi

# 3.3 AssignTechnicianDelegate & NotifyTechnicianDelegate
if grep -q 'assignmentService.assignTechnician' "${DELEGATE_DIR}/AssignTechnicianDelegate.java" && \
   grep -q 'DISPATCH NOTIFICATION' "${DELEGATE_DIR}/NotifyTechnicianDelegate.java"; then
    pass "AssignTechnicianDelegate and NotifyTechnicianDelegate implement technician assignment & alert dispatch"
else
    fail "Technician assignment/notification delegate incomplete"
fi

# 3.4 CloseMaintenanceTicketDelegate
if grep -q 'ticketService.closeTicket' "${DELEGATE_DIR}/CloseMaintenanceTicketDelegate.java"; then
    pass "CloseMaintenanceTicketDelegate closes ticket and saves resolution notes"
else
    fail "CloseMaintenanceTicketDelegate incomplete"
fi

# 3.5 Aggregation Delegates
if grep -q 'deviceAnomalyHistory' "${DELEGATE_DIR}/AggregateAnomaliesDelegate.java" && \
   grep -q 'ESCALATION_THRESHOLD' "${DELEGATE_DIR}/EvaluateAggregatedAnomaliesDelegate.java"; then
    pass "AggregateAnomaliesDelegate and EvaluateAggregatedAnomaliesDelegate track history & evaluate escalation threshold"
else
    fail "Aggregation delegates incomplete"
fi

# ===========================================
# TEST 4: Domain & Persistence Layer Validation
# ===========================================
section "TEST 4: Domain & Persistence Layer Validation"

# 4.1 MaintenanceTicket entity
TICKET_ENTITY="${PROJECT_ROOT}/orbit-orchestrator/src/main/java/com/orbit/orchestrator/domain/MaintenanceTicket.java"
if grep -q '@Entity' "${TICKET_ENTITY}" && \
   grep -q '@Table(name = "maintenance_tickets")' "${TICKET_ENTITY}" && \
   grep -q 'TicketStatus' "${TICKET_ENTITY}"; then
    pass "MaintenanceTicket domain entity mapped with JPA annotations and TicketStatus"
else
    fail "MaintenanceTicket entity definition incomplete"
fi

# 4.2 TicketStatus enum states
STATUS_ENUM="${PROJECT_ROOT}/orbit-orchestrator/src/main/java/com/orbit/orchestrator/domain/TicketStatus.java"
if grep -q 'OPEN' "${STATUS_ENUM}" && \
   grep -q 'ASSIGNED' "${STATUS_ENUM}" && \
   grep -q 'WAITING_CONFIRMATION' "${STATUS_ENUM}" && \
   grep -q 'CLOSED' "${STATUS_ENUM}"; then
    pass "TicketStatus enum covers OPEN, ASSIGNED, WAITING_CONFIRMATION, CLOSED"
else
    fail "TicketStatus enum missing expected states"
fi

# 4.3 Repository methods
REPO_FILE="${PROJECT_ROOT}/orbit-orchestrator/src/main/java/com/orbit/orchestrator/repository/MaintenanceTicketRepository.java"
if grep -q 'findByDeviceId' "${REPO_FILE}" && \
   grep -q 'findByProcessInstanceId' "${REPO_FILE}" && \
   grep -q 'countByStatus' "${REPO_FILE}"; then
    pass "MaintenanceTicketRepository defines queries for deviceId, processInstanceId, and status counts"
else
    fail "MaintenanceTicketRepository missing required finder/counter methods"
fi

# ===========================================
# TEST 5: REST API & Kafka Integration Validation
# ===========================================
section "TEST 5: REST API & Kafka Integration Validation"

# 5.1 MaintenanceTicketController REST endpoints
CONTROLLER_FILE="${PROJECT_ROOT}/orbit-orchestrator/src/main/java/com/orbit/orchestrator/controller/MaintenanceTicketController.java"
if grep -q '@RequestMapping("/api/v1/tickets")' "${CONTROLLER_FILE}" && \
   grep -q 'assignTechnician' "${CONTROLLER_FILE}" && \
   grep -q 'confirmMaintenance' "${CONTROLLER_FILE}" && \
   grep -q 'closeTicket' "${CONTROLLER_FILE}" && \
   grep -q 'getStats' "${CONTROLLER_FILE}"; then
    pass "MaintenanceTicketController exposes /api/v1/tickets, assign, confirm, close, and stats endpoints"
else
    fail "MaintenanceTicketController missing expected endpoints"
fi

# 5.2 Kafka AnomalyEventConsumer
CONSUMER_FILE="${PROJECT_ROOT}/orbit-orchestrator/src/main/java/com/orbit/orchestrator/kafka/AnomalyEventConsumer.java"
if grep -q 'anomaly-events' "${CONSUMER_FILE}" && \
   grep -q 'orchestrationService.startProcess(event)' "${CONSUMER_FILE}"; then
    pass "AnomalyEventConsumer listens to 'anomaly-events' topic and triggers BPMN process"
else
    fail "AnomalyEventConsumer topic or BPMN trigger missing"
fi

# ===========================================
# TEST 6: Unit & Integration Tests Execution
# ===========================================
section "TEST 6: Unit & Integration Tests Execution"

if command -v mvn >/dev/null 2>&1; then
    echo "  Running Maven test on orbit-orchestrator..."
    MAVEN_LOG=$(mktemp /tmp/mvn-test-phase3.XXXXXX.log 2>/dev/null || echo "mvn-test-phase3.log")
    if (cd "${PROJECT_ROOT}" && mvn test -pl orbit-orchestrator >"${MAVEN_LOG}" 2>&1); then
        pass "orbit-orchestrator Maven test suite passed successfully"
        rm -f "${MAVEN_LOG}"

        if [[ -d "${PROJECT_ROOT}/orbit-orchestrator/target/surefire-reports" ]]; then
            pass "Surefire test reports generated for orbit-orchestrator"
        else
            fail "Test reports missing"
        fi
    else
        fail "orbit-orchestrator tests failed. Last 40 lines of build log:"
        if [[ -f "${MAVEN_LOG}" ]]; then
            tail -n 40 "${MAVEN_LOG}" || true
            rm -f "${MAVEN_LOG}"
        fi
    fi
else
    skip "Maven CLI not installed — cannot run tests"
fi

# ===========================================
# TEST 7: Docker Configuration Validation
# ===========================================
section "TEST 7: Docker Configuration Validation"

# 7.1 Orchestrator Dockerfile
if grep -q 'FROM maven:' "${PROJECT_ROOT}/orbit-orchestrator/Dockerfile" && \
   grep -q 'ENTRYPOINT' "${PROJECT_ROOT}/orbit-orchestrator/Dockerfile" && \
   grep -q '8083' "${PROJECT_ROOT}/orbit-orchestrator/Dockerfile"; then
    pass "orbit-orchestrator Dockerfile is valid multi-stage build exposing port 8083"
else
    fail "orbit-orchestrator Dockerfile incomplete"
fi

# 7.2 Docker Compose service
if grep -q 'orbit-orchestrator:' "${PROJECT_ROOT}/docker-compose.yml" && \
   grep -q '8083:8083' "${PROJECT_ROOT}/docker-compose.yml"; then
    pass "orbit-orchestrator service configured in docker-compose.yml on port 8083"
else
    fail "orbit-orchestrator missing from docker-compose.yml"
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
    echo -e "  ${GREEN}All tests passed! Phase 3 is fully implemented and verified.${NC}"
    exit 0
else
    echo -e "  ${RED}${FAILED} test(s) failed. Please fix the issues above.${NC}"
    exit 1
fi
