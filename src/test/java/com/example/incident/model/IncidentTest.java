package com.example.incident.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentTest {

    @Test
    void allowsValidLifecycleTransitions() {
        Incident incident = new Incident("Power outage", Incident.Severity.HIGH, "ops@example.com", null, null);

        incident.transitionTo(Incident.Status.ACKNOWLEDGED, "dispatcher", "On it");
        incident.transitionTo(Incident.Status.IN_PROGRESS, "field-team", null);
        incident.transitionTo(Incident.Status.RESOLVED, "field-team", "Generator restored");
        incident.transitionTo(Incident.Status.CLOSED, "dispatcher", null);
    }

    @Test
    void rejectsSkippingStates() {
        Incident incident = new Incident("Breach", Incident.Severity.CRITICAL, "security@example.com", null, null);

        assertThatThrownBy(() -> incident.transitionTo(Incident.Status.RESOLVED, "sec", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void rejectsTransitionFromClosed() {
        Incident incident = new Incident("Weather", Incident.Severity.MEDIUM, "ops@example.com", null, null);
        incident.transitionTo(Incident.Status.CLOSED, "ops@example.com", "False alarm");

        assertThatThrownBy(() -> incident.transitionTo(Incident.Status.ACKNOWLEDGED, "ops", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
