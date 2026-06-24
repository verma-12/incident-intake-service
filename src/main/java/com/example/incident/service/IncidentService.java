package com.example.incident.service;

import com.example.incident.model.Incident;
import com.example.incident.model.IncidentApi.ApiException;
import com.example.incident.model.IncidentApi.CreateRequest;
import com.example.incident.model.IncidentApi.IncidentView;
import com.example.incident.model.IncidentApi.TransitionRequest;
import com.example.incident.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository repository;

    public IncidentService(IncidentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IntakeResult intake(CreateRequest request) {
        String externalRef = normalizeExternalReference(request.externalReferenceId());

        if (externalRef != null) {
            Optional<Incident> existing = repository.findByExternalReferenceId(externalRef);
            if (existing.isPresent()) {
                Incident incident = existing.get();
                mdc(incident, "intake-idempotent");
                if (!incident.matchesIntake(request.title(), request.severity(), request.reportedBy())) {
                    log.warn("Duplicate externalReferenceId with conflicting payload externalReferenceId={}", externalRef);
                    throw ApiException.duplicateReference(
                            "externalReferenceId '" + externalRef + "' already exists with different incident data");
                }
                log.info("Returning existing incident for duplicate externalReferenceId");
                return new IntakeResult(IncidentView.from(incident, true), true);
            }
        }

        Incident incident = new Incident(
                request.title(),
                request.severity(),
                request.reportedBy(),
                externalRef,
                request.description()
        );
        incident.recordCreation();
        Incident saved = repository.save(incident);

        mdc(saved, "intake-create");
        log.info("Incident created");
        return new IntakeResult(IncidentView.from(saved, true), false);
    }

    @Transactional(readOnly = true)
    public IncidentView getById(UUID id) {
        Incident incident = find(id);
        mdc(incident, "get-by-id");
        log.info("Incident retrieved");
        return IncidentView.from(incident, true);
    }

    @Transactional(readOnly = true)
    public List<IncidentView> list(Incident.Severity severity, Incident.Status status) {
        MDC.put("operation", "list");
        if (severity != null) {
            MDC.put("severity", severity.name());
        }
        if (status != null) {
            MDC.put("status", status.name());
        }

        List<IncidentView> results = repository.findFiltered(severity, status).stream()
                .map(i -> IncidentView.from(i, false))
                .toList();
        log.info("Listed incidents count={}", results.size());
        return results;
    }

    @Transactional
    public IncidentView transition(UUID id, TransitionRequest request) {
        Incident incident = find(id);
        mdc(incident, "transition");
        MDC.put("status", request.status().name());

        Incident.Status previous = incident.getStatus();
        try {
            incident.transitionTo(request.status(), request.actor(), request.note());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid transition attempted targetStatus={} currentStatus={}", request.status(), previous);
            throw ApiException.invalidTransition(ex.getMessage());
        }

        Incident saved = repository.save(incident);
        log.info("Incident status transitioned from={} to={}", previous, saved.getStatus());
        return IncidentView.from(saved, true);
    }

    private Incident find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Incident not found: " + id));
    }

    private static String normalizeExternalReference(String externalReferenceId) {
        if (externalReferenceId == null) {
            return null;
        }
        String trimmed = externalReferenceId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void mdc(Incident incident, String operation) {
        MDC.put("operation", operation);
        MDC.put("incidentId", incident.getId().toString());
        if (incident.getExternalReferenceId() != null) {
            MDC.put("externalReferenceId", incident.getExternalReferenceId());
        }
        MDC.put("severity", incident.getSeverity().name());
        MDC.put("status", incident.getStatus().name());
    }

    public record IntakeResult(IncidentView incident, boolean idempotentReplay) {
    }
}
