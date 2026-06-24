package com.example.incident.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_events")
public class IncidentEvent {

    public enum Type { CREATED, STATUS_CHANGED, NOTE_ADDED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private Type eventType;

    @Column(length = 120)
    private String actor;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected IncidentEvent() {
    }

    public IncidentEvent(Incident incident, Type eventType, String actor, String message) {
        this.incident = incident;
        this.eventType = eventType;
        this.actor = actor;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Type getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getMessage() {
        return message;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
