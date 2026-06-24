package com.example.incident.repository;

import com.example.incident.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByExternalReferenceId(String externalReferenceId);

    @Query("""
            SELECT i FROM Incident i
            WHERE (:severity IS NULL OR i.severity = :severity)
              AND (:status IS NULL OR i.status = :status)
            """)
    List<Incident> findFiltered(
            @Param("severity") Incident.Severity severity,
            @Param("status") Incident.Status status
    );
}
