package com.example.incident.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Request/response POJOs and API errors for the REST layer. */
public final class IncidentApi {

    private IncidentApi() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull Incident.Severity severity,
            @NotBlank @Size(max = 120) String reportedBy,
            @Size(max = 120) String externalReferenceId,
            @Size(max = 2000) String description
    ) {
    }

    public record TransitionRequest(
            @NotNull Incident.Status status,
            @NotBlank @Size(max = 120) String actor,
            @Size(max = 2000) String note
    ) {
    }

    public record EventView(
            UUID id,
            IncidentEvent.Type eventType,
            String actor,
            String message,
            Instant occurredAt
    ) {
        static EventView from(IncidentEvent event) {
            return new EventView(
                    event.getId(),
                    event.getEventType(),
                    event.getActor(),
                    event.getMessage(),
                    event.getOccurredAt()
            );
        }
    }

    public record IncidentView(
            UUID id,
            String title,
            Incident.Severity severity,
            String reportedBy,
            String externalReferenceId,
            Incident.Status status,
            String description,
            Instant createdAt,
            Instant updatedAt,
            List<EventView> timeline
    ) {
        public static IncidentView from(Incident incident, boolean includeTimeline) {
            List<EventView> timeline = includeTimeline
                    ? incident.getEvents().stream()
                    .sorted(Comparator.comparing(IncidentEvent::getOccurredAt))
                    .map(EventView::from)
                    .toList()
                    : List.of();

            return new IncidentView(
                    incident.getId(),
                    incident.getTitle(),
                    incident.getSeverity(),
                    incident.getReportedBy(),
                    incident.getExternalReferenceId(),
                    incident.getStatus(),
                    incident.getDescription(),
                    incident.getCreatedAt(),
                    incident.getUpdatedAt(),
                    timeline
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ErrorResponse(
            String code,
            String message,
            Instant timestamp,
            String path,
            List<FieldError> fieldErrors
    ) {
        public record FieldError(String field, String message) {
        }
    }

    public static final class ApiException extends RuntimeException {
        private final String code;
        private final int httpStatus;

        private ApiException(String code, int httpStatus, String message) {
            super(message);
            this.code = code;
            this.httpStatus = httpStatus;
        }

        public static ApiException notFound(String message) {
            return new ApiException("INCIDENT_NOT_FOUND", 404, message);
        }

        public static ApiException duplicateReference(String message) {
            return new ApiException("DUPLICATE_EXTERNAL_REFERENCE_CONFLICT", 409, message);
        }

        public static ApiException invalidTransition(String message) {
            return new ApiException("INVALID_STATUS_TRANSITION", 422, message);
        }

        public String getCode() {
            return code;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }
}
