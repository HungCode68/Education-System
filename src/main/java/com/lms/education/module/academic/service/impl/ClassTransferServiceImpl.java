package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ClassTransferHistoryDto;
import com.lms.education.module.academic.dto.TransferStudentRequest;
import com.lms.education.module.academic.entity.ClassStudent;
import com.lms.education.module.academic.entity.ClassTransferHistory;
import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.academic.repository.ClassStudentRepository;
import com.lms.education.module.academic.repository.ClassTransferHistoryRepository;
import com.lms.education.module.academic.repository.PhysicalClassRepository;
import com.lms.education.module.academic.service.ClassTransferService;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
// import com.lms.education.module.user.repository.UserRepository; // (Optional: Nếu muốn lưu người thực hiện)
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.education.security.UserPrincipal; // Import UserPrincipal
import com.lms.education.module.user.repository.UserRepository; // Import UserRepository
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassTransferServiceImpl implements ClassTransferService {

    private final ClassTransferHistoryRepository historyRepository;
    private final ClassStudentRepository classStudentRepository; // Để thao tác nhập/xuất lớp
    private final PhysicalClassRepository physicalClassRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class) // Quan trọng: Lỗi bất kỳ bước nào cũng sẽ rollback
    public void transferStudent(TransferStudentRequest request) {
        log.info("Yêu cầu chuyển lớp cho HS: {} sang lớp: {}", request.getStudentId(), request.getToClassId());

        // Kiểm tra Lớp Mới (To Class)
        PhysicalClass toClass = physicalClassRepository.findById(request.getToClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Lớp mới không tồn tại"));

        // Kiểm tra sĩ số lớp mới (Có còn chỗ không?)
        long currentSize = classStudentRepository.countByPhysicalClassIdAndStatus(toClass.getId(), ClassStudent.StudentStatus.studying);
        if (currentSize >= toClass.getMaxStudents()) {
            throw new OperationNotPermittedException("Lớp đích " + toClass.getName() + " đã đủ sĩ số (" + toClass.getMaxStudents() + " học sinh).");
        }

        // Tìm lớp hiện tại của học sinh (From Class)
        // Lưu ý: Phải tìm trong cùng Năm học với lớp mới (Tránh chuyển nhầm năm)
        ClassStudent currentEnrollment = classStudentRepository
                .findCurrentClassOfStudent(request.getStudentId(), toClass.getSchoolYear().getId())
                .orElseThrow(() -> new OperationNotPermittedException("Học sinh này chưa được xếp vào lớp nào trong năm học " + toClass.getSchoolYear().getName()));

        PhysicalClass fromClass = currentEnrollment.getPhysicalClass();
        Student student = currentEnrollment.getStudent();

        // Kiểm tra logic: Không cho chuyển sang chính lớp đang học
        if (fromClass.getId().equals(toClass.getId())) {
            throw new OperationNotPermittedException("Học sinh đang học lớp này rồi, không thể chuyển!");
        }

        // THỰC HIỆN LOGIC CHUYỂN (TRANSACTION)

        // "Rút hồ sơ" khỏi lớp cũ
        currentEnrollment.setStatus(ClassStudent.StudentStatus.transferred);
        currentEnrollment.setEndDate(LocalDate.now());
        classStudentRepository.save(currentEnrollment);

        // "Nhập học" vào lớp mới
        // Tính STT mới
        Integer maxNumber = classStudentRepository.findMaxStudentNumber(toClass.getId());
        int nextNumber = (maxNumber == null) ? 1 : maxNumber + 1;

        ClassStudent newEnrollment = ClassStudent.builder()
                .student(student)
                .physicalClass(toClass)
                .studentNumber(nextNumber)
                .enrollmentDate(LocalDate.now())
                .status(ClassStudent.StudentStatus.studying) // Trạng thái đang học
                .build();
        classStudentRepository.save(newEnrollment);

        // Ghi Log Lịch sử
        User currentUser = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem có user đăng nhập không và Principal có đúng kiểu UserPrincipal không
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal) {

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            // Tìm User Entity trong DB dựa trên ID lấy từ Token
            currentUser = userRepository.findById(principal.getId()).orElse(null);
        }
        ClassTransferHistory history = ClassTransferHistory.builder()
                .student(student)
                .fromClass(fromClass)
                .toClass(toClass)
                .transferDate(LocalDate.now())
                .reason(request.getReason())
                .createdBy(currentUser)
                .build();

        historyRepository.save(history);

        log.info("Chuyển lớp thành công: {} -> {} bởi User: {}",
                fromClass.getName(), toClass.getName(),
                (currentUser != null ? currentUser.getEmail() : "System/Unknown"));
    }

    @Override
    public List<ClassTransferHistoryDto> getHistoryByStudent(String studentId) {
        return historyRepository.findByStudentId(studentId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public PageResponse<ClassTransferHistoryDto> searchHistory(String keyword, String classId, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ClassTransferHistory> pageResult = historyRepository.search(keyword, classId, startDate, endDate, pageable);

        List<ClassTransferHistoryDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<ClassTransferHistoryDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    // Mapper Helper
    private ClassTransferHistoryDto mapToDto(ClassTransferHistory entity) {
        return ClassTransferHistoryDto.builder()
                .id(entity.getId())
                .studentId(entity.getStudent().getId())
                .studentName(entity.getStudent().getFullName())
                .studentCode(entity.getStudent().getStudentCode())
                .fromClassId(entity.getFromClass().getId())
                .fromClassName(entity.getFromClass().getName())
                .toClassId(entity.getToClass().getId())
                .toClassName(entity.getToClass().getName())
                .transferDate(entity.getTransferDate())
                .reason(entity.getReason())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getEmail() : "Hệ thống") // Fallback name
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
