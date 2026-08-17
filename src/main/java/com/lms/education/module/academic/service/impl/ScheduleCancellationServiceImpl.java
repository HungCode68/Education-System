package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ScheduleCancellationDto;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.ScheduleCancellation;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.ScheduleCancellationRepository;
import com.lms.education.module.academic.service.ClassesService;
import com.lms.education.module.academic.service.ScheduleCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleCancellationServiceImpl implements ScheduleCancellationService {

    private final ScheduleCancellationRepository cancellationRepository;
    private final ClassesRepository classesRepository;
    private final ClassesService classesService;

    @Override
    @Transactional
    public ScheduleCancellationDto create(ScheduleCancellationDto dto) {
        Classes classes = null;
        if (dto.getClassId() != null) {
            classes = classesRepository.findById(dto.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));
        }

        ScheduleCancellation cancellation = ScheduleCancellation.builder()
                .classes(classes)
                .reason(dto.getReason())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        ScheduleCancellation saved = cancellationRepository.save(cancellation);
        
        // Trigger endDate calculation
        if (classes != null) {
            classesService.recalculateEndDate(classes.getId());
        } else {
            classesService.recalculateAllActiveClasses();
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ScheduleCancellationDto update(Long id, ScheduleCancellationDto dto) {
        ScheduleCancellation cancellation = cancellationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ với ID: " + id));

        Classes newClasses = null;
        if (dto.getClassId() != null) {
            newClasses = classesRepository.findById(dto.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));
        }

        Long oldClassId = cancellation.getClasses() != null ? cancellation.getClasses().getId() : null;

        cancellation.setClasses(newClasses);
        cancellation.setReason(dto.getReason());
        cancellation.setStartDate(dto.getStartDate());
        cancellation.setEndDate(dto.getEndDate());

        ScheduleCancellation updated = cancellationRepository.save(cancellation);
        
        // Trigger endDate calculation
        if (newClasses == null || oldClassId == null || !newClasses.getId().equals(oldClassId)) {
             classesService.recalculateAllActiveClasses();
        } else if (newClasses != null) {
             classesService.recalculateEndDate(newClasses.getId());
        }

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ScheduleCancellation cancellation = cancellationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ với ID: " + id));

        Long classId = cancellation.getClasses() != null ? cancellation.getClasses().getId() : null;
        
        cancellationRepository.delete(cancellation);
        
        // Trigger endDate calculation
        if (classId != null) {
            classesService.recalculateEndDate(classId);
        } else {
            classesService.recalculateAllActiveClasses();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleCancellationDto getById(Long id) {
        ScheduleCancellation cancellation = cancellationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch nghỉ với ID: " + id));
        return mapToDto(cancellation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduleCancellationDto> getAll(Long classId, Pageable pageable) {
        return cancellationRepository.findAll(pageable).map(this::mapToDto); // To do: add query by classId if needed
    }

    private ScheduleCancellationDto mapToDto(ScheduleCancellation entity) {
        return ScheduleCancellationDto.builder()
                .id(entity.getId())
                .classId(entity.getClasses() != null ? entity.getClasses().getId() : null)
                .reason(entity.getReason())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
