package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.TermDto;
import com.lms.education.module.academic.entity.Term;
import com.lms.education.module.academic.repository.TermRepository;
import com.lms.education.module.academic.service.TermService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;

    @Override
    @Transactional
    public TermDto create(TermDto dto) {
        String formattedCode = dto.getCode().trim().toUpperCase();

        if (termRepository.existsByCode(formattedCode)) {
            throw new DuplicateResourceException("Mã đợt/kỳ học '" + formattedCode + "' đã tồn tại trên hệ thống!");
        }

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày bắt đầu phải trước ngày kết thúc!");
        }

        Term term = Term.builder()
                .code(formattedCode)
                .name(dto.getName().trim())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .year(dto.getYear())
                .status(dto.getStatus() != null ? dto.getStatus().trim().toUpperCase() : "ACTIVE")
                .build();

        Term savedTerm = termRepository.save(term);
        log.info("Đã tạo mới kỳ/đợt học: {} (Mã: {})", savedTerm.getName(), savedTerm.getCode());

        return mapToDto(savedTerm);
    }

    @Override
    @Transactional
    public TermDto update(Long id, TermDto dto) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt/kỳ học với ID: " + id));

        String formattedCode = dto.getCode().trim().toUpperCase();

        if (!term.getCode().equals(formattedCode) && termRepository.existsByCode(formattedCode)) {
            throw new DuplicateResourceException("Mã đợt/kỳ học '" + formattedCode + "' đã được sử dụng!");
        }

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày bắt đầu phải trước ngày kết thúc!");
        }

        term.setCode(formattedCode);
        term.setName(dto.getName().trim());
        term.setStartDate(dto.getStartDate());
        term.setEndDate(dto.getEndDate());
        term.setYear(dto.getYear());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            term.setStatus(dto.getStatus().trim().toUpperCase());
        }

        Term updatedTerm = termRepository.save(term);
        log.info("Đã cập nhật đợt/kỳ học ID: {}", id);

        return mapToDto(updatedTerm);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt/kỳ học với ID: " + id));

        termRepository.delete(term);
        log.info("Đã xóa hoàn toàn đợt/kỳ học ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TermDto getById(Long id) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt/kỳ học với ID: " + id));
        return mapToDto(term);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TermDto> getAllTerms(String keyword, Pageable pageable) {
        Page<Term> terms;
        if (keyword != null && !keyword.trim().isEmpty()) {
            terms = termRepository.searchTerms(keyword.trim(), pageable);
        } else {
            terms = termRepository.findAll(pageable);
        }
        return terms.map(this::mapToDto);
    }

    private TermDto mapToDto(Term term) {
        return TermDto.builder()
                .id(term.getId())
                .code(term.getCode())
                .name(term.getName())
                .startDate(term.getStartDate())
                .endDate(term.getEndDate())
                .year(term.getYear())
                .status(term.getStatus())
                .createdAt(term.getCreatedAt())
                .build();
    }
}
