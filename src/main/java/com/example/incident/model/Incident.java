package com.example.incident.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "incidents",
        uniqueConstraints = @UniqueConstraint(name = "uk_incidents_external_reference_id", columnNames = "external_reference_id")
)
public class Incident {

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    public enum Status { REPORTED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, CLOSED }

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "reported_by", nullable = false, length = 120)
    private String reportedBy;

    @Column(name = "external_reference_id", length = 120)
    private String externalReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IncidentEvent> events = new ArrayList<>();

    protected Incident() {
    }

    public Incident(String title, Severity severity, String reportedBy, String externalReferenceId, String description) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.severity = severity;
        this.reportedBy = reportedBy;
        this.externalReferenceId = externalReferenceId;
        this.description = description;
        this.status = Status.REPORTED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public String getExternalReferenceId() {
        return externalReferenceId;
    }

    public Status getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<IncidentEvent> getEvents() {
        return events;
    }

    public void transitionTo(Status newStatus, String actor, String note) {
        if (newStatus == status) {
            throw new IllegalArgumentException("Incident is already in status " + status);
        }
        validateTransition(status, newStatus);
        Status previous = status;
        status = newStatus;
        addEvent(IncidentEvent.Type.STATUS_CHANGED, actor,
                "Status changed from " + previous + " to " + newStatus
                        + (note != null && !note.isBlank() ? ": " + note : ""));
    }

    public void recordCreation() {
        addEvent(IncidentEvent.Type.CREATED, reportedBy, "Incident reported");
    }

    private void addEvent(IncidentEvent.Type type, String actor, String message) {
        events.add(new IncidentEvent(this, type, actor, message));
    }

    private static void validateTransition(Status from, Status to) {
        boolean allowed = switch (from) {
            case REPORTED -> to == Status.ACKNOWLEDGED || to == Status.CLOSED;
            case ACKNOWLEDGED -> to == Status.IN_PROGRESS || to == Status.CLOSED;
            case IN_PROGRESS -> to == Status.RESOLVED || to == Status.CLOSED;
            case RESOLVED -> to == Status.CLOSED;
            case CLOSED -> false;
        };
        if (!allowed) {
            throw new IllegalArgumentException("Invalid status transition from " + from + " to " + to);
        }
    }

    public boolean matchesIntake(String title, Severity severity, String reportedBy) {
        return this.title.equals(title)
                && this.severity == severity
                && this.reportedBy.equals(reportedBy);
    }
}
