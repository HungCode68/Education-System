package com.lms.education.integration;

import com.lms.education.module.user.dto.StaffDto;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StaffIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StaffRepository staffRepository;

    private Staff testStaff;

    @BeforeEach
    void setUp() {
        List<Staff> staffs = staffRepository.findAll();
        if (staffs.isEmpty()) {
            Staff staff = new Staff();
            staff.setStaffCode("STF_TEST_01");
            staff.setStaffType("TEACHER");
            staff.setFullName("Test Staff");
            staff.setPhone("0987654321");
            staff.setBaseSalary(new BigDecimal("15000000"));
            staff.setStatus("ACTIVE");
            testStaff = staffRepository.save(staff);
        } else {
            testStaff = staffs.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"STAFF_VIEW"})
    void testGetStaffById() throws Exception {
        mockMvc.perform(get("/api/v1/staffs/" + testStaff.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testStaff.getId()));
    }

    @Test
    @WithMockUser(authorities = {"STAFF_CREATE"})
    void testCreateStaff() throws Exception {
        StaffDto dto = new StaffDto();
        dto.setStaffType("TEACHING_ASSISTANT");
        dto.setFullName("New Assistant");
        dto.setPhone("0912345678");
        dto.setBaseSalary(new BigDecimal("10000000"));
        dto.setHireDate(LocalDate.now());

        mockMvc.perform(post("/api/v1/staffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo hồ sơ nhân sự thành công!"))
                .andExpect(jsonPath("$.data.fullName").value("New Assistant"));
    }

    @Test
    @WithMockUser(authorities = {"STAFF_UPDATE"})
    void testUpdateStaff() throws Exception {
        StaffDto dto = new StaffDto();
        dto.setStaffType("TEACHER");
        dto.setFullName("Updated Staff Name");
        dto.setPhone("0987654321");
        dto.setBaseSalary(new BigDecimal("20000000"));

        mockMvc.perform(put("/api/v1/staffs/" + testStaff.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin nhân sự thành công!"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Staff Name"));
    }

    @Test
    @WithMockUser(authorities = {"STAFF_DELETE"})
    void testDeleteStaff() throws Exception {
        mockMvc.perform(delete("/api/v1/staffs/" + testStaff.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa hồ sơ nhân sự thành công!"));
    }
}
