package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ScheduleCancellationDto;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.ScheduleCancellation;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.ScheduleCancellationRepository;
import com.lms.education.module.academic.service.ClassesService;
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
public class ScheduleCancellationServiceImplTest {

    @Mock
    private ScheduleCancellationRepository cancellationRepository;

    @Mock
    private ClassesRepository classesRepository;

    @Mock
    private ClassesService classesService;

    @InjectMocks
    private ScheduleCancellationServiceImpl cancellationService;

    private ScheduleCancellation mockCancellation;
    private ScheduleCancellationDto mockDto;
    private Classes mockClass;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);

        mockCancellation = new ScheduleCancellation();
        mockCancellation.setId(1L);
        mockCancellation.setClasses(mockClass);
        mockCancellation.setReason("Holiday");
        mockCancellation.setStartDate(LocalDate.of(2024, 4, 30));
        mockCancellation.setEndDate(LocalDate.of(2024, 5, 1));

        mockDto = ScheduleCancellationDto.builder()
                .classId(10L)
                .reason("Holiday")
                .startDate(LocalDate.of(2024, 4, 30))
                .endDate(LocalDate.of(2024, 5, 1))
                .build();
    }

    @Test
    void create_Success_WithClassId() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(cancellationRepository.save(any(ScheduleCancellation.class))).thenReturn(mockCancellation);

        ScheduleCancellationDto result = cancellationService.create(mockDto);

        assertNotNull(result);
        assertEquals("Holiday", result.getReason());
        verify(classesService).recalculateEndDate(10L);
    }

    @Test
    void create_Success_CenterWide() {
        mockDto.setClassId(null);
        mockCancellation.setClasses(null);
        
        when(cancellationRepository.save(any(ScheduleCancellation.class))).thenReturn(mockCancellation);

        ScheduleCancellationDto result = cancellationService.create(mockDto);

        assertNotNull(result);
        assertNull(result.getClassId());
        verify(classesService).recalculateAllActiveClasses();
    }

    @Test
    void create_ClassNotFound_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cancellationService.create(mockDto));
    }

    @Test
    void update_Success() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.of(mockCancellation));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(cancellationRepository.save(any(ScheduleCancellation.class))).thenReturn(mockCancellation);

        ScheduleCancellationDto result = cancellationService.update(1L, mockDto);

        assertNotNull(result);
        verify(classesService).recalculateEndDate(10L);
    }
    
    @Test
    void update_ToCenterWide_Success() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.of(mockCancellation));
        mockDto.setClassId(null);
        mockCancellation.setClasses(null);
        when(cancellationRepository.save(any(ScheduleCancellation.class))).thenReturn(mockCancellation);

        ScheduleCancellationDto result = cancellationService.update(1L, mockDto);

        assertNotNull(result);
        verify(classesService).recalculateAllActiveClasses();
    }

    @Test
    void update_ClassNotFound_ThrowsException() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.of(mockCancellation));
        when(classesRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cancellationService.update(1L, mockDto));
    }

    @Test
    void update_CancellationNotFound_ThrowsException() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cancellationService.update(1L, mockDto));
    }

    @Test
    void delete_Success_WithClassId() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.of(mockCancellation));
        
        cancellationService.delete(1L);
        
        verify(cancellationRepository).delete(mockCancellation);
        verify(classesService).recalculateEndDate(10L);
    }
    
    @Test
    void delete_Success_CenterWide() {
        mockCancellation.setClasses(null);
        when(cancellationRepository.findById(1L)).thenReturn(Optional.of(mockCancellation));
        
        cancellationService.delete(1L);
        
        verify(cancellationRepository).delete(mockCancellation);
        verify(classesService).recalculateAllActiveClasses();
    }

    @Test
    void delete_CancellationNotFound_ThrowsException() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cancellationService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.of(mockCancellation));
        
        ScheduleCancellationDto result = cancellationService.getById(1L);
        
        assertEquals("Holiday", result.getReason());
    }

    @Test
    void getById_CancellationNotFound_ThrowsException() {
        when(cancellationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cancellationService.getById(1L));
    }

    @Test
    void getAll_Success() {
        Page<ScheduleCancellation> page = new PageImpl<>(List.of(mockCancellation));
        Pageable pageable = PageRequest.of(0, 10);
        when(cancellationRepository.findAll(pageable)).thenReturn(page);

        Page<ScheduleCancellationDto> result = cancellationService.getAll(10L, pageable);

        assertEquals(1, result.getTotalElements());
    }
}
