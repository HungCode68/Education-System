package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.SemesterDto;
import com.lms.education.module.academic.entity.SchoolYear;
import com.lms.education.module.academic.entity.Semester;
import com.lms.education.module.academic.repository.SchoolYearRepository;
import com.lms.education.module.academic.repository.SemesterRepository;
import com.lms.education.module.academic.service.SemesterService;
import com.lms.education.module.teaching_assignment.repository.TeachingAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;

    @Override
    @Transactional
    public SemesterDto create(SemesterDto dto) {
        log.info("Tạo học kỳ mới: {} - Năm học ID: {}", dto.getName(), dto.getSchoolYearId());

        // Validate Năm học
        SchoolYear schoolYear = schoolYearRepository.findById(dto.getSchoolYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Năm học không tồn tại"));

        // Validate Logic Ngày tháng cơ bản
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }

        // Validate: Thời gian học kỳ phải nằm TRONG phạm vi của Năm học
        if (dto.getStartDate().isBefore(schoolYear.getStartDate()) || dto.getEndDate().isAfter(schoolYear.getEndDate())) {
            throw new OperationNotPermittedException(
                    "Thời gian học kỳ phải nằm trong phạm vi của năm học (" +
                            schoolYear.getStartDate() + " đến " + schoolYear.getEndDate() + ")"
            );
        }

        // Validate Trùng lặp (Tên, Mã, Thứ tự) trong cùng năm
        if (semesterRepository.existsByNameAndSchoolYearId(dto.getName(), dto.getSchoolYearId())) {
            throw new DuplicateResourceException("Tên học kỳ '" + dto.getName() + "' đã tồn tại trong năm học này.");
        }
        if (semesterRepository.existsByCodeAndSchoolYearId(dto.getCode(), dto.getSchoolYearId())) {
            throw new DuplicateResourceException("Mã học kỳ '" + dto.getCode() + "' đã tồn tại.");
        }
        if (semesterRepository.existsByPriorityAndSchoolYearId(dto.getPriority(), dto.getSchoolYearId())) {
            throw new DuplicateResourceException("Thứ tự ưu tiên '" + dto.getPriority() + "' đã được sử dụng.");
        }

        // Validate Chồng chéo thời gian (Quan trọng nhất)
        if (semesterRepository.existsOverlapDates(dto.getSchoolYearId(), dto.getStartDate(), dto.getEndDate(), null)) {
            throw new OperationNotPermittedException("Thời gian của học kỳ này bị chồng chéo lên một học kỳ khác trong cùng năm.");
        }

        // Save
        Semester entity = Semester.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .priority(dto.getPriority())
                .schoolYear(schoolYear)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus() != null ? dto.getStatus() : Semester.SemesterStatus.upcoming)
                .build();

        return mapToDto(semesterRepository.save(entity));
    }

    @Override
    @Transactional
    public SemesterDto update(String id, SemesterDto dto) {
        Semester existing = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ ID: " + id));

        // Validate Date
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }

        // Check range năm học
        SchoolYear schoolYear = existing.getSchoolYear();
        if (dto.getStartDate().isBefore(schoolYear.getStartDate()) || dto.getEndDate().isAfter(schoolYear.getEndDate())) {
            throw new OperationNotPermittedException("Thời gian học kỳ phải nằm trong phạm vi của năm học.");
        }

        // Validate Trùng lặp
        if (!existing.getName().equals(dto.getName()) &&
                semesterRepository.existsByNameAndSchoolYearId(dto.getName(), schoolYear.getId())) {
            throw new DuplicateResourceException("Tên học kỳ đã tồn tại.");
        }
        if (!existing.getCode().equals(dto.getCode()) &&
                semesterRepository.existsByCodeAndSchoolYearId(dto.getCode(), schoolYear.getId())) {
            throw new DuplicateResourceException("Mã học kỳ đã tồn tại.");
        }
        // Riêng Priority check kỹ: Nếu đổi Priority -> Check trùng
        if (!existing.getPriority().equals(dto.getPriority()) &&
                semesterRepository.existsByPriorityAndSchoolYearId(dto.getPriority(), schoolYear.getId())) {
            throw new DuplicateResourceException("Thứ tự ưu tiên '" + dto.getPriority() + "' đã bị trùng.");
        }

        // Validate Chồng chéo (Truyền ID hiện tại để loại trừ)
        if (semesterRepository.existsOverlapDates(schoolYear.getId(), dto.getStartDate(), dto.getEndDate(), id)) {
            throw new OperationNotPermittedException("Thời gian cập nhật bị chồng chéo lên học kỳ khác.");
        }

        // Update
        existing.setName(dto.getName());
        existing.setCode(dto.getCode());
        existing.setPriority(dto.getPriority());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());

        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }

        return mapToDto(semesterRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(String id) {
        // Kiểm tra tồn tại
        if (!semesterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy học kỳ ID: " + id);
        }

        // Validate Ràng buộc dữ liệu: Kiểm tra Phân công giảng dạy
        // Nếu học kỳ này đã có giáo viên được phân công -> CHẶN XÓA
        if (teachingAssignmentRepository.existsBySemesterId(id)) {
            throw new OperationNotPermittedException(
                    "Không thể xóa học kỳ này vì đã có dữ liệu Phân công giảng dạy (Teaching Assignment) liên quan. " +
                            "Vui lòng gỡ bỏ các phân công hoặc xóa dữ liệu liên quan trước."
            );
        }

        // Lưu ý: Sau này nếu có bảng Điểm (Scores), bạn cũng cần check thêm ở đây
        // Ví dụ: if (scoreRepository.existsBySemesterId(id)) throw ...

        // Xóa an toàn
        try {
            semesterRepository.deleteById(id);
        } catch (Exception e) {
            // Check dự phòng cho các ràng buộc khác chưa handle
            throw new OperationNotPermittedException("Không thể xóa học kỳ này vì dính líu đến dữ liệu khác trong hệ thống.");
        }
    }

    @Override
    public SemesterDto getById(String id) {
        Semester entity = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Học kỳ không tồn tại"));
        return mapToDto(entity);
    }

    @Override
    public List<SemesterDto> getAllBySchoolYear(String schoolYearId) {
        // Dùng hàm sắp xếp theo Priority để hiển thị đúng thứ tự
        return semesterRepository.findBySchoolYearIdOrderByPriorityAsc(schoolYearId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(String id, Semester.SemesterStatus status) {
        Semester entity = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Học kỳ không tồn tại"));
        entity.setStatus(status);
        semesterRepository.save(entity);
    }

    // --- Mapper Helper ---
    private SemesterDto mapToDto(Semester entity) {
        return SemesterDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .priority(entity.getPriority())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                // School Year Info
                .schoolYearId(entity.getSchoolYear().getId())
                .schoolYearName(entity.getSchoolYear().getName())
                // Audit
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
