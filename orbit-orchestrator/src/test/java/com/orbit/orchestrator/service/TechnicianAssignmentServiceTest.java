package com.orbit.orchestrator.service;

import com.orbit.orchestrator.domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicianAssignmentServiceTest {

    private TechnicianAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new TechnicianAssignmentService();
    }

    @Test
    @DisplayName("Assigns technicians for TEMPERATURE metric from thermo pool round-robin")
    void assignTemperatureTechnicians() {
        String tech1 = service.assignTechnician("dev-1", Severity.HIGH, "TEMPERATURE");
        String tech2 = service.assignTechnician("dev-2", Severity.HIGH, "TEMPERATURE");

        assertThat(tech1).startsWith("tech-thermo-");
        assertThat(tech2).startsWith("tech-thermo-");
        assertThat(tech1).isNotEqualTo(tech2);
    }

    @Test
    @DisplayName("Assigns technicians for BATTERY_LEVEL metric from power pool")
    void assignBatteryTechnicians() {
        String tech = service.assignTechnician("dev-1", Severity.CRITICAL, "BATTERY_LEVEL");
        assertThat(tech).startsWith("tech-power-");
    }

    @Test
    @DisplayName("Assigns default technicians when unknown metric is provided")
    void assignDefaultTechnicians() {
        String tech = service.assignTechnician("dev-1", Severity.LOW, "UNKNOWN_METRIC");
        assertThat(tech).startsWith("tech-general-");
    }

    @Test
    @DisplayName("Assigns default technicians when null metric is provided")
    void assignDefaultWhenNullMetric() {
        String tech = service.assignTechnician("dev-1", Severity.MEDIUM, null);
        assertThat(tech).startsWith("tech-general-");
    }
}
