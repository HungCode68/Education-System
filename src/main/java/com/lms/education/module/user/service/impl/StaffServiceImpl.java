package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.AccountProvisionDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.repository.DepartmentRepository;
import com.lms.education.module.user.dto.StaffDto;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.service.StaffService;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private StaffService self;

    @Override
    @Transactional
    public StaffDto create(StaffDto dto) {

        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản User với ID: " + dto.getUserId()));

            if (staffRepository.existsByUserId(user.getId())) {
                throw new DuplicateResourceException("Tài khoản này đã được liên kết với một hồ sơ nhân sự khác!");
            }
        }

        //  Kiểm tra tính hợp lệ của Department (Nếu có truyền lên)
        Department department = null;
        String prefix = "NV";
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban với ID: " + dto.getDepartmentId()));
            prefix = department.getCode().trim().toUpperCase();
        }

        // Lấy 2 số cuối của năm hiện tại (Ví dụ: 2026 -> "26")
        String currentYearLastTwoDigits = String.valueOf(java.time.LocalDate.now().getYear() % 100);

        //  Đếm số lượng nhân viên cùng phòng ban đã vào trong năm để sinh số thứ tự tiếp theo
        long currentCount = staffRepository.countByStaffCodePattern(prefix, currentYearLastTwoDigits);
        long nextSequence = currentCount + 1; // Số thứ tự tiếp theo

        // Định dạng số thứ tự thành chuỗi 4 chữ số (Ví dụ: 1 -> "0001", 123 -> "0123")
        String formattedSequence = String.format("%04d", nextSequence);

        // Ráp thành mã nhân sự hoàn chỉnh (Ví dụ: GV + 26 + 0001 = GV260001)
        String generatedStaffCode = prefix + currentYearLastTwoDigits + formattedSequence;

        // Xây dựng và lưu dữ liệu
        Staff staff = Staff.builder()
                .user(user)
                .department(department)
                .staffCode(generatedStaffCode)
                .staffType(dto.getStaffType().toUpperCase())
                .jobTitle(dto.getJobTitle() != null ? dto.getJobTitle().trim() : null)
                .fullName(dto.getFullName().trim())
                .phone(dto.getPhone())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .address(dto.getAddress())
                .nationality(dto.getNationality() != null ? dto.getNationality() : "Vietnam")
                .identityNumber(dto.getIdentityNumber())
                .hireDate(dto.getHireDate())
                .contractType(dto.getContractType() != null ? dto.getContractType().toUpperCase() : null)
                .baseSalary(dto.getBaseSalary())
                .status(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "ACTIVE")
                .build();

        Staff savedStaff = staffRepository.save(staff);
        log.info("Đã tạo mới hồ sơ nhân sự: {} - {}", savedStaff.getStaffCode(), savedStaff.getFullName());

        return mapToDto(savedStaff);
    }

    @Override
    @Transactional
    public StaffDto update(Long id, StaffDto dto) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + id));

        //  Kiểm tra đổi User (Nếu có truyền userId lên và nó khác với userId hiện tại)
        if (dto.getUserId() != null && (staff.getUser() == null || !staff.getUser().getId().equals(dto.getUserId()))) {
            if (staffRepository.existsByUserId(dto.getUserId())) {
                throw new DuplicateResourceException("Tài khoản User mới này đã thuộc về người khác!");
            }
            User newUser = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản User với ID: " + dto.getUserId()));
            staff.setUser(newUser);
        }

        // Kiểm tra đổi phòng ban
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban với ID: " + dto.getDepartmentId()));
            staff.setDepartment(department);
        } else {
            staff.setDepartment(null); // Cho phép tháo nhân sự ra khỏi phòng ban
        }

        // Cập nhật các trường thông tin cơ bản
        staff.setStaffType(dto.getStaffType().toUpperCase());
        staff.setJobTitle(dto.getJobTitle() != null ? dto.getJobTitle().trim() : null);
        staff.setFullName(dto.getFullName().trim());
        staff.setPhone(dto.getPhone());
        staff.setDateOfBirth(dto.getDateOfBirth());
        staff.setGender(dto.getGender());
        staff.setAddress(dto.getAddress());
        staff.setNationality(dto.getNationality() != null ? dto.getNationality() : "Vietnam");
        staff.setIdentityNumber(dto.getIdentityNumber());
        staff.setHireDate(dto.getHireDate());
        staff.setContractType(dto.getContractType() != null ? dto.getContractType().toUpperCase() : null);
        staff.setBaseSalary(dto.getBaseSalary());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            staff.setStatus(dto.getStatus().toUpperCase());
        }

        Staff updatedStaff = staffRepository.save(staff);
        log.info("Đã cập nhật thông tin nhân sự ID: {}", id);

        return mapToDto(updatedStaff);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + id));
        
        User user = staff.getUser();
        // Ngắt liên kết để tránh lỗi JPA Cascade (Dù CSDL có ON DELETE CASCADE, nhưng báo trước cho Hibernate)
        staff.setUser(null);
        staffRepository.delete(staff);
        
        if (user != null) {
            userRepository.delete(user);
        }
        
        log.info("Đã xóa hoàn toàn hồ sơ nhân sự ID: {} và tài khoản User liên quan", id);
    }

    @Override
    public Map<String, Object> deleteMultiple(List<Long> ids) {
        int successCount = 0;
        int skipCount = 0;

        for (Long id : ids) {
            try {
                self.delete(id);
                successCount++;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Bỏ qua nhân sự ID {} vì vướng dữ liệu liên quan (Foreign Key)", id);
                skipCount++;
            } catch (Exception e) {
                log.error("Lỗi khi xóa nhân sự ID {}: {}", id, e.getMessage());
                skipCount++;
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("success", successCount);
        report.put("skipped", skipCount);
        report.put("totalProcessed", ids.size());
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDto getById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + id));
        return mapToDto(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDto getByStaffCode(String staffCode) {
        String formattedCode = staffCode.trim().toUpperCase();
        Staff staff = staffRepository.findByStaffCode(formattedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân sự với mã: " + formattedCode));
        return mapToDto(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffDto> getAllStaffs(String keyword, Pageable pageable) {
        Page<Staff> staffs;

        if (keyword != null && !keyword.trim().isEmpty()) {
            staffs = staffRepository.searchStaffs(keyword.trim(), pageable);
        } else {
            staffs = staffRepository.findAll(pageable);
        }

        return staffs.map(this::mapToDto);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<StaffDto> getStaffsByDepartmentId(Long departmentId, Pageable pageable) {
        // Kiểm tra xem phòng ban có tồn tại không
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Không tìm thấy phòng ban/khoa với ID: " + departmentId);
        }

        // Truy vấn danh sách nhân sự và chuyển đổi sang DTO
        Page<Staff> staffs = staffRepository.findByDepartmentId(departmentId, pageable);
        return staffs.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffDto> getTeachers() {
        List<Staff> teachers = staffRepository.findTeachers();
        return teachers.stream().map(this::mapToDto).collect(java.util.stream.Collectors.toList());
    }


    // THÊM HÀM XỬ LÝ CẤP TÀI KHOẢN
    @Override
    @Transactional
    public Map<String, Object> provisionAccounts(AccountProvisionDto dto) {
        // Lấy danh sách Role
        List<Role> roles = roleRepository.findAllById(dto.getRoleIds());
        if (roles.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy vai trò hợp lệ nào để gán cho nhân sự!");
        }

        // Lấy danh sách hồ sơ nhân sự
        List<Staff> staffs = staffRepository.findAllById(dto.getStaffIds());

        int successCount = 0;
        int skipCount = 0;

        for (Staff staff : staffs) {
            // Chỉ cấp tài khoản cho những ai CHƯA CÓ user_id
            if (staff.getUser() == null) {

                // Sinh Email tự động (Ví dụ: nv001@school.edu.vn)
                String generatedEmail = staff.getStaffCode().toLowerCase() + "@school.edu.vn";

                // Sinh Password là mã nhân viên viết hoa
                String rawPassword = staff.getStaffCode().toUpperCase();

                // Kiểm tra tránh lỗi crash nếu Email này vô tình bị ai đó tạo tay trước đó rồi
                if (userRepository.existsByEmail(generatedEmail)) {
                    skipCount++;
                    continue;
                }

                // Tạo tài khoản User mới
                User newUser = User.builder()
                        .email(generatedEmail)
                        .password(passwordEncoder.encode(rawPassword))
                        .fullName(staff.getFullName()) // Lấy tên thật của nhân viên sang
                        .status("ACTIVE")
                        .roles(new HashSet<>(roles)) // Gán Role
                        .build();

                User savedUser = userRepository.save(newUser);

                //  Móc ngược user_id vào hồ sơ Staff hiện tại
                staff.setUser(savedUser);
                successCount++;

                log.info("Đã cấp tài khoản {} cho nhân sự {}", generatedEmail, staff.getStaffCode());
            } else {
                // Nếu đã có tài khoản rồi thì bỏ qua để không bị lỗi
                skipCount++;
            }
        }

        // Nhờ cơ chế Transactional của Spring, các thay đổi trên object 'staff' sẽ tự động UPDATE xuống DB

        // Trả về báo cáo thống kê
        Map<String, Object> report = new HashMap<>();
        report.put("success", successCount);
        report.put("skipped", skipCount);
        report.put("totalProcessed", staffs.size());

        return report;
    }

    // --- Helper Method: Chuyển đổi Entity sang DTO & Trích xuất dữ liệu mồi cho Frontend ---
    private StaffDto mapToDto(Staff staff) {
        return StaffDto.builder()
                .id(staff.getId())
                .userId(staff.getUser() != null ? staff.getUser().getId() : null)
                .userEmail(staff.getUser() != null ? staff.getUser().getEmail() : null)
                .departmentId(staff.getDepartment() != null ? staff.getDepartment().getId() : null)
                .departmentName(staff.getDepartment() != null ? staff.getDepartment().getName() : null) // Lấy Tên Khoa
                .staffCode(staff.getStaffCode())
                .staffType(staff.getStaffType())
                .jobTitle(staff.getJobTitle())
                .fullName(staff.getFullName())
                .phone(staff.getPhone())
                .dateOfBirth(staff.getDateOfBirth())
                .gender(staff.getGender())
                .address(staff.getAddress())
                .nationality(staff.getNationality())
                .identityNumber(staff.getIdentityNumber())
                .hireDate(staff.getHireDate())
                .contractType(staff.getContractType())
                .baseSalary(staff.getBaseSalary())
                .status(staff.getStatus())
                .createdAt(staff.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDto getMyProfile(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản của bạn chưa được liên kết với hồ sơ nhân sự nào."));
        return mapToDto(staff);
    }
}