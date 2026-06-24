package com.example.incident.controller;

import com.example.incident.model.Incident;
import com.example.incident.model.IncidentApi.CreateRequest;
import com.example.incident.model.IncidentApi.IncidentView;
import com.example.incident.model.IncidentApi.TransitionRequest;
import com.example.incident.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService service;

    public IncidentController(IncidentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<IncidentView> create(@Valid @RequestBody CreateRequest request) {
        IncidentService.IntakeResult result = service.intake(request);
        HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.incident());
    }

    @GetMapping("/{id}")
    public IncidentView getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping
    public List<IncidentView> list(
            @RequestParam(required = false) Incident.Severity severity,
            @RequestParam(required = false) Incident.Status status
    ) {
        return service.list(severity, status);
    }

    @PatchMapping("/{id}/status")
    public IncidentView transition(@PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        return service.transition(id, request);
    }
}
