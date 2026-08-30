package com.orbit.orchestrator;

import com.orbit.orchestrator.domain.AnomalyEvent;
import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.domain.Severity;
import com.orbit.orchestrator.domain.TicketStatus;
import com.orbit.orchestrator.kafka.AnomalyEventConsumer;
import com.orbit.orchestrator.repository.MaintenanceTicketRepository;
import com.orbit.orchestrator.service.OrchestrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrchestrationProcessTest {

    @Autowired
    private MaintenanceTicketRepository ticketRepository;

    @Autowired
    private AnomalyEventConsumer anomalyEventConsumer;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void highSeverityKafkaEventCanBeConfirmedThroughThePublicTicketApi() throws Exception {
        UUID eventId = UUID.randomUUID();
        anomalyEventConsumer.consume(new AnomalyEvent(
                eventId, "device-42", Severity.HIGH, Instant.now(),
                "TEMPERATURE_THRESHOLD", "Temperature exceeded safe range", 91.5, "TEMPERATURE"));

        MaintenanceTicket ticket = ticketRepository.findByCreatedFromEvent(eventId).orElseThrow();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.WAITING_CONFIRMATION);
        assertThat(ticket.getAssignedTechnician()).startsWith("tech-thermo-");

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/confirm", ticket.getTicketId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"technicianId\":\"" + ticket.getAssignedTechnician()
                                + "\",\"notes\":\"Replaced cooling fan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED_AND_COMPLETED"));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticket.getTicketId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Replaced cooling fan"));

        MaintenanceTicket closedTicket = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(closedTicket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closedTicket.getResolutionNotes()).isEqualTo("Replaced cooling fan");
    }
}
