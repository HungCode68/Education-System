package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.TermDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TermService {

    TermDto create(TermDto dto);

    TermDto update(Long id, TermDto dto);

    void delete(Long id);

    TermDto getById(Long id);

    Page<TermDto> getAllTerms(String keyword, Pageable pageable);
}
