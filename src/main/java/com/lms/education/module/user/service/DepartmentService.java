package com.lms.education.module.user.service;
//package com.lms.education.module.department.service; không có vì DepartmentService nằm trong module user, không phải module department
import com.lms.education.module.user.dto.DepartmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {

    // Tạo mới một phòng ban/khoa
    DepartmentDto create(DepartmentDto dto);

    // Cập nhật thông tin phòng ban/khoa
    DepartmentDto update(Long id, DepartmentDto dto);

    // Xóa phòng ban/khoa theo ID
    void delete(Long id);

    // Lấy chi tiết phòng ban/khoa theo ID
    DepartmentDto getById(Long id);

    // Lấy chi tiết phòng ban/khoa theo mã Code
    DepartmentDto getByCode(String code);

    // Lấy danh sách phòng ban có hỗ trợ tìm kiếm từ khóa và phân trang
    Page<DepartmentDto> getAllDepartments(String keyword, Pageable pageable);
}