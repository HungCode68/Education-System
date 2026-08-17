package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.ScheduleCancellationDto;
import com.lms.education.module.academic.service.ScheduleCancellationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cancellations")
@RequiredArgsConstructor
public class ScheduleCancellationController {

    private final ScheduleCancellationService cancellationService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHEDULE_CREATE')")
    public ResponseEntity<ScheduleCancellationDto> create(@Valid @RequestBody ScheduleCancellationDto dto) {
        return new ResponseEntity<>(cancellationService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_UPDATE')")
    public ResponseEntity<ScheduleCancellationDto> update(@PathVariable Long id, @Valid @RequestBody ScheduleCancellationDto dto) {
        return ResponseEntity.ok(cancellationService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cancellationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public ResponseEntity<ScheduleCancellationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cancellationService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCHEDULE_VIEW') or isAuthenticated()")
    public ResponseEntity<Page<ScheduleCancellationDto>> getAll(
            @RequestParam(required = false) Long classId,
            Pageable pageable) {
        return ResponseEntity.ok(cancellationService.getAll(classId, pageable));
    }
}
