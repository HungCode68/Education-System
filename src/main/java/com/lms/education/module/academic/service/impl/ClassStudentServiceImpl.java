package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ClassStudentDto;
import com.lms.education.module.academic.dto.AutoDistributeRequest;
import com.lms.education.module.academic.entity.ClassStudent;
import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.academic.repository.ClassStudentRepository;
import com.lms.education.module.academic.repository.PhysicalClassRepository;
import com.lms.education.module.academic.service.ClassStudentService;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassStudentServiceImpl implements ClassStudentService {

    private final ClassStudentRepository classStudentRepository;
    private final PhysicalClassRepository physicalClassRepository;
    private final StudentRepository studentRepository; // Cần Repo module User

    // --- THÊM THỦ CÔNG (BASIC CRUD) ---
    @Override
    @Transactional
    public ClassStudentDto addStudentToClass(ClassStudentDto dto) {
        // Validate Lớp và Học sinh tồn tại
        PhysicalClass physicalClass = physicalClassRepository.findById(dto.getPhysicalClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh"));

        // Validate: Học sinh đã có lớp nào trong năm này chưa?
        if (classStudentRepository.isStudentEnrolledInYear(dto.getStudentId(), physicalClass.getSchoolYear().getId())) {
            // Có thể dùng hàm findCurrentClassOfStudent để báo rõ tên lớp đang học
            throw new DuplicateResourceException("Học sinh này đã được xếp vào một lớp khác trong năm học này rồi!");
        }

        // Validate Sĩ số
        long currentCount = classStudentRepository.countByPhysicalClassIdAndStatus(physicalClass.getId(), ClassStudent.StudentStatus.studying);
        if (currentCount >= physicalClass.getMaxStudents()) {
            throw new OperationNotPermittedException("Lớp học đã đủ sĩ số (" + physicalClass.getMaxStudents() + " học sinh).");
        }

        // Tính STT tự động (Max + 1)
        Integer maxNumber = classStudentRepository.findMaxStudentNumber(physicalClass.getId());
        int nextNumber = (maxNumber == null) ? 1 : maxNumber + 1;

        // Save
        ClassStudent entity = ClassStudent.builder()
                .physicalClass(physicalClass)
                .student(student)
                .studentNumber(nextNumber)
                .status(ClassStudent.StudentStatus.studying)
                .build();

        return mapToDto(classStudentRepository.save(entity));
    }

    // --- TOOL PHÂN LỚP TỰ ĐỘNG (ALGORITHM) ---
    @Override
    @Transactional
    public Map<String, String> autoDistributeStudents(AutoDistributeRequest request) {
        log.info("Bắt đầu phân lớp tự động cho {} học sinh vào {} lớp.", request.getStudentIds().size(), request.getClassIds().size());

        // Lấy danh sách Lớp đích
        List<PhysicalClass> targetClasses = physicalClassRepository.findAllById(request.getClassIds());
        if (targetClasses.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy lớp học nào trong danh sách gửi lên.");

        // Lọc bỏ học sinh đã có lớp (Tránh lỗi trùng)
        List<String> alreadyEnrolledIds = classStudentRepository.findEnrolledStudentIdsInYear(request.getSchoolYearId(), request.getStudentIds());
        List<String> validStudentIds = request.getStudentIds().stream()
                .filter(id -> !alreadyEnrolledIds.contains(id))
                .collect(Collectors.toList());

        if (validStudentIds.isEmpty()) {
            throw new OperationNotPermittedException("Tất cả học sinh trong danh sách đều đã được xếp lớp!");
        }

        // Chuẩn bị dữ liệu để chạy thuật toán
        List<Student> students = studentRepository.findAllById(validStudentIds);

        // Map theo dõi sĩ số hiện tại của từng lớp (để tính toán)
        Map<PhysicalClass, Integer> classCountMap = new HashMap<>();
        Map<PhysicalClass, Integer> classMaxNumberMap = new HashMap<>(); // Theo dõi STT hiện tại để tăng dần

        for (PhysicalClass pc : targetClasses) {
            // Lấy sĩ số hiện tại từ DB
            int currentSize = (int) classStudentRepository.countByPhysicalClassIdAndStatus(pc.getId(), ClassStudent.StudentStatus.studying);
            classCountMap.put(pc, currentSize);

            // Lấy STT lớn nhất hiện tại
            Integer maxNum = classStudentRepository.findMaxStudentNumber(pc.getId());
            classMaxNumberMap.put(pc, (maxNum == null) ? 0 : maxNum);
        }

        // THUẬT TOÁN "CHIA BÀI" (ROUND ROBIN) CÓ KIỂM TRA SĨ SỐ
        List<ClassStudent> toSaveList = new ArrayList<>();
        int classIndex = 0;
        int totalClasses = targetClasses.size();
        Map<String, Integer> resultReport = new HashMap<>(); // Để báo cáo kết quả: Lớp A thêm 5, Lớp B thêm 5

        for (Student student : students) {
            boolean assigned = false;
            int attempts = 0;

            // Thử tìm lớp còn chỗ
            while (!assigned && attempts < totalClasses) {
                PhysicalClass currentClass = targetClasses.get(classIndex);
                int currentSize = classCountMap.get(currentClass);

                if (currentSize < currentClass.getMaxStudents()) {
                    // Còn chỗ -> Xếp vào
                    int nextStt = classMaxNumberMap.get(currentClass) + 1;

                    ClassStudent newMapping = ClassStudent.builder()
                            .physicalClass(currentClass)
                            .student(student)
                            .studentNumber(nextStt)
                            .status(ClassStudent.StudentStatus.studying)
                            .build();
                    toSaveList.add(newMapping);

                    // Cập nhật bộ đếm tạm thời
                    classCountMap.put(currentClass, currentSize + 1);
                    classMaxNumberMap.put(currentClass, nextStt);
                    resultReport.put(currentClass.getName(), resultReport.getOrDefault(currentClass.getName(), 0) + 1);

                    assigned = true;
                }

                // Chuyển sang lớp tiếp theo (Vòng tròn)
                classIndex = (classIndex + 1) % totalClasses;
                attempts++;
            }

            if (!assigned) {
                log.warn("Không thể xếp lớp cho HS: {} vì tất cả các lớp đích đều đã đầy!", student.getFullName());
            }
        }

        // Lưu Batch xuống DB (Hiệu năng cao)
        classStudentRepository.saveAll(toSaveList);

        // 6. Trả về báo cáo dạng Text
        Map<String, String> finalResponse = new HashMap<>();
        finalResponse.put("total_assigned", toSaveList.size() + "/" + validStudentIds.size());
        resultReport.forEach((className, count) -> finalResponse.put(className, "Đã thêm " + count + " học sinh"));

        if (!alreadyEnrolledIds.isEmpty()) {
            finalResponse.put("skipped", "Đã bỏ qua " + alreadyEnrolledIds.size() + " học sinh do đã có lớp.");
        }

        return finalResponse;
    }

    // --- TOOL LÊN LỚP (AUTO PROMOTE) ---
    @Override
    @Transactional
    public void promoteStudents(String oldClassId, String newClassId) {
        // Lấy thông tin 2 lớp
        PhysicalClass oldClass = physicalClassRepository.findById(oldClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp cũ không tồn tại"));
        PhysicalClass newClass = physicalClassRepository.findById(newClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp mới không tồn tại"));

        // Lấy danh sách HS "Được lên lớp" (Status = completed hoặc studying)
        // (Tùy nghiệp vụ trường bạn, ở đây mình lấy 'studying' giả định là cuối năm chưa set completed)
        List<ClassStudent> oldStudents = classStudentRepository.findByPhysicalClassIdAndStatus(oldClassId, ClassStudent.StudentStatus.studying);

        if (oldStudents.isEmpty()) {
            throw new OperationNotPermittedException("Lớp cũ không có học sinh nào để lên lớp!");
        }

        // Chuẩn bị danh sách mới
        List<ClassStudent> newStudents = new ArrayList<>();
        // Lấy STT hiện tại của lớp mới (thường là 0 vì lớp mới tinh)
        int currentStt = 0;

        for (ClassStudent oldItem : oldStudents) {
            // Check xem HS này đã có trong lớp mới chưa (tránh click đúp)
            if (classStudentRepository.existsByPhysicalClassIdAndStudentId(newClassId, oldItem.getStudent().getId())) {
                continue;
            }

            currentStt++;
            ClassStudent newItem = ClassStudent.builder()
                    .physicalClass(newClass)
                    .student(oldItem.getStudent())
                    .studentNumber(currentStt)
                    .status(ClassStudent.StudentStatus.studying)
                    .build();
            newStudents.add(newItem);
        }

        // Validate Sĩ số tổng
        if (newStudents.size() > newClass.getMaxStudents()) {
            throw new OperationNotPermittedException("Lớp mới không đủ chỗ chứa (" + newStudents.size() + "/" + newClass.getMaxStudents() + ")");
        }

        // Save
        classStudentRepository.saveAll(newStudents);
        log.info("Đã chuyển {} học sinh từ {} sang {}", newStudents.size(), oldClass.getName(), newClass.getName());
    }

    @Override
    public List<ClassStudentDto> getStudentsByClass(String classId, String statusStr) {
        // Convert String sang Enum (Xử lý Null an toàn)
        ClassStudent.StudentStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = ClassStudent.StudentStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                throw new OperationNotPermittedException("Trạng thái học sinh không hợp lệ: " + statusStr);
            }
        }

        // Gọi Repo với tham số status (có thể là null hoặc value)
        return classStudentRepository.findByPhysicalClassIdAndStatusOption(classId, status).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public void removeStudentFromClass(String id) {
        classStudentRepository.deleteById(id);
    }

    @Override
    public void updateStatus(String id, String statusStr) {
        ClassStudent cs = classStudentRepository.findById(id).orElseThrow();
        try {
            ClassStudent.StudentStatus status = ClassStudent.StudentStatus.valueOf(statusStr);
            cs.setStatus(status);
            classStudentRepository.save(cs);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
    }

    // Helper Mapper
    private ClassStudentDto mapToDto(ClassStudent entity) {
        return ClassStudentDto.builder()
                .id(entity.getId())
                .physicalClassId(entity.getPhysicalClass().getId())
                .physicalClassName(entity.getPhysicalClass().getName())
                .studentId(entity.getStudent().getId())
                .studentName(entity.getStudent().getFullName())
                .studentCode(entity.getStudent().getStudentCode()) // Cần getter trong Student Entity
                .studentNumber(entity.getStudentNumber())
                .enrollmentDate(entity.getEnrollmentDate())
                .status(entity.getStatus())
                .build();
    }
}