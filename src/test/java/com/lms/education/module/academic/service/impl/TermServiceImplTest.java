package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.TermDto;
import com.lms.education.module.academic.entity.Term;
import com.lms.education.module.academic.repository.TermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TermServiceImplTest {

    @Mock
    private TermRepository termRepository;

    @InjectMocks
    private TermServiceImpl termService;

    private Term mockTerm;
    private TermDto mockTermDto;

    @BeforeEach
    void setUp() {
        mockTerm = new Term();
        mockTerm.setId(1L);
        mockTerm.setCode("SP2024");
        mockTerm.setName("Spring 2024");
        mockTerm.setStartDate(LocalDate.of(2024, 1, 1));
        mockTerm.setEndDate(LocalDate.of(2024, 6, 1));
        mockTerm.setYear(2024);
        mockTerm.setStatus("ACTIVE");

        mockTermDto = TermDto.builder()
                .code("sp2024 ")
                .name("Spring 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 1))
                .year(2024)
                .status("ACTIVE")
                .build();
    }

    @Test
    void create_Success() {
        when(termRepository.existsByCode("SP2024")).thenReturn(false);
        when(termRepository.save(any(Term.class))).thenReturn(mockTerm);

        TermDto result = termService.create(mockTermDto);

        assertNotNull(result);
        assertEquals("SP2024", result.getCode());
    }

    @Test
    void create_Success_NullStatus_SetsActive() {
        mockTermDto.setStatus(null);
        when(termRepository.existsByCode("SP2024")).thenReturn(false);
        when(termRepository.save(any(Term.class))).thenReturn(mockTerm);

        TermDto result = termService.create(mockTermDto);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void create_DuplicateCode_ThrowsException() {
        when(termRepository.existsByCode("SP2024")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> termService.create(mockTermDto));
    }

    @Test
    void create_InvalidDates_ThrowsException() {
        when(termRepository.existsByCode("SP2024")).thenReturn(false);
        mockTermDto.setStartDate(LocalDate.of(2024, 7, 1));
        assertThrows(OperationNotPermittedException.class, () -> termService.create(mockTermDto));
    }

    @Test
    void update_Success() {
        when(termRepository.findById(1L)).thenReturn(Optional.of(mockTerm));
        mockTermDto.setCode("FA2024");
        when(termRepository.existsByCode("FA2024")).thenReturn(false);
        when(termRepository.save(any(Term.class))).thenReturn(mockTerm);

        TermDto result = termService.update(1L, mockTermDto);

        assertNotNull(result);
        verify(termRepository).save(mockTerm);
    }

    @Test
    void update_DuplicateCode_ThrowsException() {
        when(termRepository.findById(1L)).thenReturn(Optional.of(mockTerm));
        mockTermDto.setCode("FA2024");
        when(termRepository.existsByCode("FA2024")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> termService.update(1L, mockTermDto));
    }

    @Test
    void update_InvalidDates_ThrowsException() {
        when(termRepository.findById(1L)).thenReturn(Optional.of(mockTerm));
        mockTermDto.setStartDate(LocalDate.of(2024, 7, 1));

        assertThrows(OperationNotPermittedException.class, () -> termService.update(1L, mockTermDto));
    }

    @Test
    void update_TermNotFound_ThrowsException() {
        when(termRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> termService.update(1L, mockTermDto));
    }

    @Test
    void delete_Success() {
        when(termRepository.findById(1L)).thenReturn(Optional.of(mockTerm));
        termService.delete(1L);
        verify(termRepository).delete(mockTerm);
    }

    @Test
    void delete_TermNotFound_ThrowsException() {
        when(termRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> termService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(termRepository.findById(1L)).thenReturn(Optional.of(mockTerm));
        TermDto result = termService.getById(1L);
        assertEquals("SP2024", result.getCode());
    }

    @Test
    void getById_TermNotFound_ThrowsException() {
        when(termRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> termService.getById(1L));
    }

    @Test
    void getAllTerms_WithKeyword() {
        Page<Term> page = new PageImpl<>(List.of(mockTerm));
        Pageable pageable = PageRequest.of(0, 10);
        when(termRepository.searchTerms("SP", pageable)).thenReturn(page);

        Page<TermDto> result = termService.getAllTerms("SP", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllTerms_WithoutKeyword() {
        Page<Term> page = new PageImpl<>(List.of(mockTerm));
        Pageable pageable = PageRequest.of(0, 10);
        when(termRepository.findAll(pageable)).thenReturn(page);

        Page<TermDto> result = termService.getAllTerms(null, pageable);

        assertEquals(1, result.getTotalElements());
    }
}
