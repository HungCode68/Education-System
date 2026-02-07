package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.PhysicalClassDto;
import com.lms.education.module.academic.entity.Grade;
import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.academic.entity.SchoolYear;
import com.lms.education.module.academic.repository.ClassStudentRepository;
import com.lms.education.module.academic.repository.GradeRepository;
import com.lms.education.module.academic.repository.PhysicalClassRepository;
import com.lms.education.module.academic.repository.SchoolYearRepository;
import com.lms.education.module.academic.service.PhysicalClassService;
// Import Teacher Repository và Entity
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.repository.TeacherRepository;
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhysicalClassServiceImpl implements PhysicalClassService {

    private final PhysicalClassRepository classRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final GradeRepository gradeRepository;
    private final TeacherRepository teacherRepository;
    private final ClassStudentRepository classStudentRepository;

    @Override
    @Transactional
    public PhysicalClassDto create(PhysicalClassDto dto) {
        log.info("Tạo lớp học mới: {} - Năm học ID: {}", dto.getName(), dto.getSchoolYearId());

        // Validate Năm học
        SchoolYear schoolYear = schoolYearRepository.findById(dto.getSchoolYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Năm học ID: " + dto.getSchoolYearId()));

        // Check năm học có Active không?
        if (SchoolYear.SchoolYearStatus.archived.equals(schoolYear.getStatus())) {
            throw new OperationNotPermittedException("Không thể tạo lớp trong năm học đã lưu trữ!");
        }

        // Validate Khối
        Grade grade = gradeRepository.findById(dto.getGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khối ID: " + dto.getGradeId()));

        // Validate Trùng tên lớp trong năm học
        if (classRepository.existsByNameAndSchoolYearId(dto.getName(), dto.getSchoolYearId())) {
            throw new DuplicateResourceException("Lớp '" + dto.getName() + "' đã tồn tại trong năm học " + schoolYear.getName());
        }

        // Validate Giáo viên chủ nhiệm
        Teacher homeroomTeacher = null;
        if (dto.getHomeroomTeacherId() != null && !dto.getHomeroomTeacherId().isEmpty()) {
            homeroomTeacher = teacherRepository.findById(dto.getHomeroomTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Giáo viên ID: " + dto.getHomeroomTeacherId()));

            // Check: GV này đã chủ nhiệm lớp nào khác trong năm nay chưa?
            if (classRepository.existsByHomeroomTeacherIdAndSchoolYearId(dto.getHomeroomTeacherId(), dto.getSchoolYearId())) {
                throw new DuplicateResourceException("Giáo viên " + homeroomTeacher.getFullName() + " đang chủ nhiệm một lớp khác trong năm học này!");
            }
        }

        // Save
        PhysicalClass entity = PhysicalClass.builder()
                .name(dto.getName())
                .roomNumber(dto.getRoomNumber())
                .maxStudents(dto.getMaxStudents())
                .schoolYear(schoolYear)
                .grade(grade)
                .homeroomTeacher(homeroomTeacher)
                .status(PhysicalClass.ClassStatus.active)
                .build();

        return mapToDto(classRepository.save(entity));
    }

    @Override
    @Transactional
    public PhysicalClassDto update(String id, PhysicalClassDto dto) {
        log.info("Update lớp ID: {}", id);

        PhysicalClass existingClass = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học ID: " + id));

        // Validate Trùng tên (Nếu đổi tên)
        // Chỉ check khi tên thay đổi VÀ tên mới đã tồn tại trong CÙNG năm học
        if (!existingClass.getName().equals(dto.getName())) {
            Optional<PhysicalClass> duplicate = classRepository.findByNameAndSchoolYearId(dto.getName(), existingClass.getSchoolYear().getId());
            if (duplicate.isPresent()) {
                throw new DuplicateResourceException("Tên lớp '" + dto.getName() + "' đã được sử dụng trong năm học này!");
            }
        }

        // Validate GVCN (Nếu thay đổi)
        if (dto.getHomeroomTeacherId() != null) {
            // Nếu gửi ID khác với GV hiện tại
            boolean isNewTeacher = existingClass.getHomeroomTeacher() == null ||
                    !existingClass.getHomeroomTeacher().getId().equals(dto.getHomeroomTeacherId());

            if (isNewTeacher) {
                Teacher newTeacher = teacherRepository.findById(dto.getHomeroomTeacherId())
                        .orElseThrow(() -> new ResourceNotFoundException("GV không tồn tại"));

                // Check trùng nhiệm vụ
                // Tìm xem GV đang dạy lớp nào? Nếu có lớp và lớp đó KHÁC lớp hiện tại -> Lỗi.
                Optional<PhysicalClass> currentClassOfTeacher = classRepository
                        .findBySchoolYearIdAndHomeroomTeacherId(existingClass.getSchoolYear().getId(), dto.getHomeroomTeacherId());

                if (currentClassOfTeacher.isPresent() && !currentClassOfTeacher.get().getId().equals(id)) {
                    throw new DuplicateResourceException("Giáo viên " + newTeacher.getFullName() + " đang chủ nhiệm lớp " + currentClassOfTeacher.get().getName());
                }

                existingClass.setHomeroomTeacher(newTeacher);
            }
        } else {
            // Nếu gửi null -> Gỡ GVCN
            existingClass.setHomeroomTeacher(null);
        }

        // Cập nhật các trường thông tin cơ bản
        // Không cho phép đổi SchoolYear hay Grade ở đây (Muốn đổi thì xóa đi tạo lại cho chuẩn logic)
        existingClass.setName(dto.getName());
        existingClass.setRoomNumber(dto.getRoomNumber());
        existingClass.setMaxStudents(dto.getMaxStudents());

        if (dto.getStatus() != null) {
            existingClass.setStatus(dto.getStatus());
        }

        return mapToDto(classRepository.save(existingClass));
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!classRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp ID: " + id);
        }
        // Nếu lớp đã có học sinh (dù là đang học hay đã thôi học) thì KHÔNG ĐƯỢC XÓA.
        if (classStudentRepository.existsByPhysicalClassId(id)) {
            throw new OperationNotPermittedException(
                    "Không thể xóa lớp học này vì đã có danh sách học sinh. " +
                            "Vui lòng chỉ cập nhật trạng thái sang 'Archived' (Lưu trữ) hoặc gỡ hết học sinh trước khi xóa."
            );
        }

        classRepository.deleteById(id);
    }

    @Override
    public PhysicalClassDto getById(String id) {
        PhysicalClass entity = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public PageResponse<PhysicalClassDto> search(String schoolYearId, String gradeId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PhysicalClass> pageResult = classRepository.search(schoolYearId, gradeId, keyword, pageable);

        List<PhysicalClassDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<PhysicalClassDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<PhysicalClassDto> getDropdownList(String schoolYearId, String gradeId) {
        return classRepository.findBySchoolYearIdAndGradeId(schoolYearId, gradeId).stream()
                .map(this::mapToDto)
                .toList();
    }

    // --- Helper Mapper ---
    private PhysicalClassDto mapToDto(PhysicalClass entity) {
        return PhysicalClassDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .roomNumber(entity.getRoomNumber())
                .maxStudents(entity.getMaxStudents())
                .status(entity.getStatus())
                // Map School Year
                .schoolYearId(entity.getSchoolYear().getId())
                .schoolYearName(entity.getSchoolYear().getName())
                // Map Grade
                .gradeId(entity.getGrade().getId())
                .gradeName(entity.getGrade().getName())
                // Map Teacher (Check Null cực kỳ quan trọng)
                .homeroomTeacherId(entity.getHomeroomTeacher() != null ? entity.getHomeroomTeacher().getId() : null)
                .homeroomTeacherName(entity.getHomeroomTeacher() != null ? entity.getHomeroomTeacher().getFullName() : "Chưa phân công")
                .homeroomTeacherCode(entity.getHomeroomTeacher() != null ? entity.getHomeroomTeacher().getTeacherCode() : null)

                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
