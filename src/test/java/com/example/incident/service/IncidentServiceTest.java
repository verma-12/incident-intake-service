package com.example.incident.service;

import com.example.incident.model.Incident;
import com.example.incident.model.IncidentApi.ApiException;
import com.example.incident.model.IncidentApi.CreateRequest;
import com.example.incident.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class IncidentServiceTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void idempotentIntakeReturnsSameIncident() {
        CreateRequest request = new CreateRequest(
                "Network outage",
                Incident.Severity.HIGH,
                "netops@example.com",
                "NET-100",
                "Core switch down"
        );

        IncidentService.IntakeResult first = incidentService.intake(request);
        IncidentService.IntakeResult second = incidentService.intake(request);

        assertThat(first.idempotentReplay()).isFalse();
        assertThat(second.idempotentReplay()).isTrue();
        assertThat(second.incident().id()).isEqualTo(first.incident().id());
        assertThat(incidentRepository.count()).isEqualTo(1);
    }

    @Test
    void conflictingDuplicateExternalReferenceThrows() {
        incidentService.intake(new CreateRequest(
                "First", Incident.Severity.LOW, "a@example.com", "REF-1", null));

        assertThatThrownBy(() -> incidentService.intake(new CreateRequest(
                "Second", Incident.Severity.LOW, "a@example.com", "REF-1", null)))
                .isInstanceOf(ApiException.class);
    }
}
