package com.lms.education.integration;

import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.entity.Room;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.academic.repository.RoomRepository;
import com.lms.education.module.teaching.dto.TeachingSubstitutionDto;
import com.lms.education.module.teaching.entity.TeachingSubstitution;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TeachingSubstitutionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TeachingSubstitutionRepository teachingSubstitutionRepository;

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    private TeachingSubstitution testSubstitution;
    private ClassSchedule testSchedule;
    private Staff testAbsentStaff;
    private Staff testSubstituteStaff;

    @BeforeEach
    void setUp() {
        // Staffs
        List<Staff> staffs = staffRepository.findAll();
        if (staffs.size() < 2) {
            Staff staff1 = new Staff();
            staff1.setStaffCode("STF_ABSENT");
            staff1.setStaffType("TEACHER");
            staff1.setFullName("Absent Teacher");
            staff1.setStatus("ACTIVE");
            testAbsentStaff = staffRepository.save(staff1);

            Staff staff2 = new Staff();
            staff2.setStaffCode("STF_SUB");
            staff2.setStaffType("TEACHER");
            staff2.setFullName("Substitute Teacher");
            staff2.setStatus("ACTIVE");
            testSubstituteStaff = staffRepository.save(staff2);
        } else {
            testAbsentStaff = staffs.get(0);
            testSubstituteStaff = staffs.get(1);
        }

        // Room
        List<Room> rooms = roomRepository.findAll();
        Room testRoom;
        if (rooms.isEmpty()) {
            Room room = new Room();
            room.setName("Room TS");
            room.setCapacity(30);
            testRoom = roomRepository.save(room);
        } else {
            testRoom = rooms.get(0);
        }

        // Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_TS");
            course.setName("Course TS");
            course.setBasePrice(new java.math.BigDecimal("1000000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Classes
        List<Classes> classList = classesRepository.findAll();
        Classes testClass;
        if (classList.isEmpty()) {
            Classes clazz = new Classes();
            clazz.setCode("CLASS_TS");
            clazz.setName("Class TS");
            clazz.setCourse(testCourse);
            clazz.setMaxStudents(30);
            clazz.setStatus("OPENING");
            testClass = classesRepository.save(clazz);
        } else {
            testClass = classList.get(0);
        }

        // Schedule
        List<ClassSchedule> schedules = classScheduleRepository.findAll();
        if (schedules.isEmpty()) {
            ClassSchedule schedule = new ClassSchedule();
            schedule.setClasses(testClass);
            schedule.setRoom(testRoom);
            schedule.setDayOfWeek(5); // Thursday
            schedule.setStartTime(LocalTime.of(14, 0));
            schedule.setEndTime(LocalTime.of(16, 0));
            testSchedule = classScheduleRepository.save(schedule);
        } else {
            testSchedule = schedules.get(0);
        }

        // Substitution
        List<TeachingSubstitution> substitutions = teachingSubstitutionRepository.findAll();
        if (substitutions.isEmpty()) {
            TeachingSubstitution substitution = new TeachingSubstitution();
            substitution.setSchedule(testSchedule);
            substitution.setAbsentStaff(testAbsentStaff);
            substitution.setSubstituteStaff(testSubstituteStaff);
            substitution.setStartDate(LocalDate.now());
            substitution.setEndDate(LocalDate.now().plusDays(7));
            substitution.setReason("Sick leave");
            substitution.setStatus("APPROVED");
            testSubstitution = teachingSubstitutionRepository.save(substitution);
        } else {
            testSubstitution = substitutions.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_VIEW"})
    void testGetSubstitutionById() throws Exception {
        mockMvc.perform(get("/api/v1/teaching-substitutions/" + testSubstitution.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSubstitution.getId()));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_CREATE"})
    void testCreateSubstitution() throws Exception {
        TeachingSubstitutionDto newDto = new TeachingSubstitutionDto();
        newDto.setScheduleId(testSchedule.getId());
        newDto.setAbsentStaffId(testAbsentStaff.getId());
        newDto.setSubstituteStaffId(testSubstituteStaff.getId());
        newDto.setStartDate(LocalDate.now().plusDays(10));
        newDto.setEndDate(LocalDate.now().plusDays(15));
        newDto.setReason("Personal matter");
        newDto.setStatus("APPROVED");

        mockMvc.perform(post("/api/v1/teaching-substitutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Phân công dạy thay thành công!"))
                .andExpect(jsonPath("$.data.reason").value("Personal matter"));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_UPDATE"})
    void testUpdateSubstitution() throws Exception {
        TeachingSubstitutionDto updateDto = new TeachingSubstitutionDto();
        updateDto.setScheduleId(testSchedule.getId());
        updateDto.setAbsentStaffId(testAbsentStaff.getId());
        updateDto.setSubstituteStaffId(testSubstituteStaff.getId());
        updateDto.setStartDate(LocalDate.now());
        updateDto.setEndDate(LocalDate.now().plusDays(7));
        updateDto.setReason("Sick leave - Updated");
        updateDto.setStatus("APPROVED");

        mockMvc.perform(put("/api/v1/teaching-substitutions/" + testSubstitution.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật phân công dạy thay thành công!"))
                .andExpect(jsonPath("$.data.reason").value("Sick leave - Updated"));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_DELETE"})
    void testDeleteSubstitution() throws Exception {
        mockMvc.perform(delete("/api/v1/teaching-substitutions/" + testSubstitution.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa phân công dạy thay thành công!"));
    }
}
