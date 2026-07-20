package com.lms.education.module.academic.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.academic.dto.TermDto;
import com.lms.education.module.academic.service.TermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @PostMapping
    @PreAuthorize("hasAuthority('TERM_CREATE')")
    @LogActivity(module = "TERM", action = "CREATE", targetType = "term", description = "Tạo mới đợt/kỳ học")
    public ResponseEntity<Map<String, Object>> createTerm(@Valid @RequestBody TermDto dto) {
        TermDto createdTerm = termService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo đợt/kỳ học thành công!");
        response.put("data", createdTerm);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TERM_UPDATE')")
    @LogActivity(module = "TERM", action = "UPDATE", targetType = "term", description = "Cập nhật thông tin đợt/kỳ học")
    public ResponseEntity<Map<String, Object>> updateTerm(
            @PathVariable Long id,
            @Valid @RequestBody TermDto dto) {

        TermDto updatedTerm = termService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin đợt/kỳ học thành công!");
        response.put("data", updatedTerm);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TERM_DELETE')")
    @LogActivity(module = "TERM", action = "DELETE", targetType = "term", description = "Xóa đợt/kỳ học")
    public ResponseEntity<Map<String, String>> deleteTerm(@PathVariable Long id) {
        termService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa đợt/kỳ học thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TERM_VIEW')")
    public ResponseEntity<TermDto> getTermById(@PathVariable Long id) {
        return ResponseEntity.ok(termService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TERM_VIEW')")
    public ResponseEntity<Page<TermDto>> getAllTerms(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(termService.getAllTerms(keyword, pageable));
    }
}
