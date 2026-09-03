package com.orbit.orchestrator.service;

import com.orbit.orchestrator.domain.MaintenanceTicket;
import com.orbit.orchestrator.domain.Severity;
import com.orbit.orchestrator.domain.TicketStatus;
import com.orbit.orchestrator.repository.MaintenanceTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceTicketServiceTest {

    @Mock
    private MaintenanceTicketRepository ticketRepository;

    private MaintenanceTicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new MaintenanceTicketService(ticketRepository);
    }

    @Test
    @DisplayName("createTicket saves and returns ticket with OPEN status")
    void createTicket() {
        UUID eventId = UUID.randomUUID();
        when(ticketRepository.save(any(MaintenanceTicket.class))).thenAnswer(i -> i.getArgument(0));

        MaintenanceTicket result = ticketService.createTicket(
                "dev-01", eventId, Severity.CRITICAL, "OVERHEAT", "Temperature critical", "proc-123"
        );

        assertThat(result.getDeviceId()).isEqualTo("dev-01");
        assertThat(result.getCreatedFromEvent()).isEqualTo(eventId);
        assertThat(result.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(result.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(result.getProcessInstanceId()).isEqualTo("proc-123");
    }

    @Test
    @DisplayName("assignTechnician updates status to ASSIGNED")
    void assignTechnician() {
        UUID ticketId = UUID.randomUUID();
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .ticketId(ticketId)
                .status(TicketStatus.OPEN)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MaintenanceTicket updated = ticketService.assignTechnician(ticketId, "tech-thermo-alpha");

        assertThat(updated.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(updated.getAssignedTechnician()).isEqualTo("tech-thermo-alpha");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("assignTechnician throws exception when ticket not found")
    void assignTechnicianNotFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.assignTechnician(ticketId, "tech-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ticket not found");
    }

    @Test
    @DisplayName("closeTicket sets CLOSED status and resolution notes")
    void closeTicket() {
        UUID ticketId = UUID.randomUUID();
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .ticketId(ticketId)
                .status(TicketStatus.WAITING_CONFIRMATION)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MaintenanceTicket closed = ticketService.closeTicket(ticketId, "Repaired successfully");

        assertThat(closed.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closed.getResolutionNotes()).isEqualTo("Repaired successfully");
        assertThat(closed.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("getAllTickets filters correctly by deviceId and status")
    void getAllTicketsFiltering() {
        ticketService.getAllTickets("dev-01", TicketStatus.OPEN);
        verify(ticketRepository).findByDeviceIdAndStatus("dev-01", TicketStatus.OPEN);

        ticketService.getAllTickets("dev-01", null);
        verify(ticketRepository).findByDeviceId("dev-01");

        ticketService.getAllTickets(null, TicketStatus.OPEN);
        verify(ticketRepository).findByStatus(TicketStatus.OPEN);

        ticketService.getAllTickets(null, null);
        verify(ticketRepository).findAll();
    }

    @Test
    @DisplayName("getTicketStats calculates counts correctly")
    void getTicketStats() {
        when(ticketRepository.count()).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(3L);
        when(ticketRepository.countBySeverity(Severity.CRITICAL)).thenReturn(2L);

        Map<String, Object> stats = ticketService.getTicketStats();

        assertThat(stats.get("totalTickets")).isEqualTo(10L);
        assertThat(stats.get("openTickets")).isEqualTo(3L);
        assertThat(stats.get("criticalTickets")).isEqualTo(2L);
    }
}
