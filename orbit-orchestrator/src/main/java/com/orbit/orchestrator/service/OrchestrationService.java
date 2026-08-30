package com.orbit.orchestrator.service;

import com.orbit.orchestrator.domain.AnomalyEvent;
import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.repository.MaintenanceTicketRepository;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);
    public static final String PROCESS_KEY = "maintenance-process";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final MaintenanceTicketRepository ticketRepository;

    public OrchestrationService(RuntimeService runtimeService,
                                TaskService taskService,
                                HistoryService historyService,
                                MaintenanceTicketRepository ticketRepository) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.ticketRepository = ticketRepository;
    }

    public ProcessInstance startProcess(AnomalyEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("eventId", event.getEventId() != null ? event.getEventId().toString() : UUID.randomUUID().toString());
        variables.put("deviceId", event.getDeviceId());
        variables.put("severity", event.getSeverity() != null ? event.getSeverity().name() : "LOW");
        variables.put("ruleTriggered", event.getRuleTriggered() != null ? event.getRuleTriggered() : "UNKNOWN_RULE");
        variables.put("description", event.getDescription() != null ? event.getDescription() : "Anomaly detected");
        variables.put("telemetryValue", event.getTelemetryValue());
        variables.put("metricType", event.getMetricType() != null ? event.getMetricType() : "UNKNOWN_METRIC");
        variables.put("detectedAt", event.getDetectedAt() != null ? event.getDetectedAt().toString() : "");

        String businessKey = "device-" + event.getDeviceId() + "-" + (event.getEventId() != null ? event.getEventId() : UUID.randomUUID());

        log.info("Starting BPMN process '{}' for device '{}' (severity={})",
                PROCESS_KEY, event.getDeviceId(), event.getSeverity());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                businessKey,
                variables
        );

        log.info("Process instance started with id '{}' (businessKey='{}')",
                processInstance.getId(), businessKey);

        return processInstance;
    }

    public boolean confirmMaintenance(UUID ticketId, String technicianId, String notes) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        String processInstanceId = ticket.getProcessInstanceId();
        if (processInstanceId == null) {
            throw new IllegalStateException("Ticket has no associated process instance");
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("Task_WaitTechnicianConfirmation")
                .singleResult();

        if (task == null) {
            // Check any active task in the process instance
            task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
        }

        if (task == null) {
            log.warn("No active user task found for process instance {}", processInstanceId);
            return false;
        }

        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("confirmedBy", technicianId);
        taskVariables.put("resolutionNotes", notes);

        taskService.complete(task.getId(), taskVariables);
        log.info("Completed user task '{}' for ticket {} by technician '{}'",
                task.getId(), ticketId, technicianId);
        return true;
    }

    public List<Map<String, Object>> getOpenTasks() {
        List<Task> tasks = taskService.createTaskQuery().list();
        return tasks.stream().map(task -> {
            Map<String, Object> item = new HashMap<>();
            item.put("taskId", task.getId());
            item.put("name", task.getName());
            item.put("assignee", task.getAssignee());
            item.put("processInstanceId", task.getProcessInstanceId());
            item.put("createTime", task.getCreateTime());
            item.put("taskDefinitionKey", task.getTaskDefinitionKey());
            return item;
        }).collect(Collectors.toList());
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables != null ? variables : Map.of());
        log.info("Directly completed task with ID '{}'", taskId);
    }

    public Map<String, Object> getOrchestratorStats() {
        long runningInstances = runtimeService.createProcessInstanceQuery().count();
        long completedInstances = historyService.createHistoricProcessInstanceQuery().completed().count();
        long openTasks = taskService.createTaskQuery().count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("runningProcessInstances", runningInstances);
        stats.put("completedProcessInstances", completedInstances);
        stats.put("totalProcessInstances", runningInstances + completedInstances);
        stats.put("openUserTasks", openTasks);
        stats.put("processKey", PROCESS_KEY);
        return stats;
    }
}
