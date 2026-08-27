package com.lms.education.integration;

import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.entity.Room;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.academic.repository.RoomRepository;
import com.lms.education.module.teaching.dto.ScheduleAssignmentDto;
import com.lms.education.module.teaching.entity.ScheduleAssignment;
import com.lms.education.module.teaching.entity.TeachingAssignment;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ScheduleAssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ScheduleAssignmentRepository scheduleAssignmentRepository;

    @Autowired
    private TeachingAssignmentRepository teachingAssignmentRepository;

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

    private ScheduleAssignment testAssignment;
    private Staff testStaff;
    private ClassSchedule testSchedule;
    private Classes testClass;

    @BeforeEach
    void setUp() {
        // Staff
        List<Staff> staffs = staffRepository.findAll();
        if (staffs.isEmpty()) {
            Staff staff = new Staff();
            staff.setStaffCode("STF_SA");
            staff.setStaffType("TEACHER");
            staff.setFullName("Schedule Teacher");
            staff.setStatus("ACTIVE");
            testStaff = staffRepository.save(staff);
        } else {
            testStaff = staffs.get(0);
        }

        // Room
        List<Room> rooms = roomRepository.findAll();
        Room testRoom;
        if (rooms.isEmpty()) {
            Room room = new Room();
            room.setName("Room SA");
            room.setCapacity(40);
            testRoom = roomRepository.save(room);
        } else {
            testRoom = rooms.get(0);
        }

        // Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_SA");
            course.setName("Schedule Assignment Course");
            course.setBasePrice(new java.math.BigDecimal("2000000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Classes
        List<Classes> classList = classesRepository.findAll();
        if (classList.isEmpty()) {
            Classes clazz = new Classes();
            clazz.setCode("CLASS_SA");
            clazz.setName("Schedule Assignment Class");
            clazz.setCourse(testCourse);
            clazz.setMaxStudents(30);
            clazz.setStatus("OPENING");
            testClass = classesRepository.save(clazz);
        } else {
            testClass = classList.get(0);
        }

        // ClassSchedule
        List<ClassSchedule> schedules = classScheduleRepository.findAll();
        if (schedules.isEmpty()) {
            ClassSchedule schedule = new ClassSchedule();
            schedule.setClasses(testClass);
            schedule.setRoom(testRoom);
            schedule.setDayOfWeek(3); // Tuesday
            schedule.setStartTime(LocalTime.of(8, 0));
            schedule.setEndTime(LocalTime.of(10, 0));
            testSchedule = classScheduleRepository.save(schedule);
        } else {
            testSchedule = schedules.get(0);
        }

        // Schedule Assignment
        List<ScheduleAssignment> assignments = scheduleAssignmentRepository.findAll();
        if (assignments.isEmpty()) {
            ScheduleAssignment assignment = new ScheduleAssignment();
            assignment.setSchedule(testSchedule);
            assignment.setTeacher(testStaff);
            assignment.setRole("MAIN_TEACHER");
            testAssignment = scheduleAssignmentRepository.save(assignment);
        } else {
            testAssignment = assignments.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_VIEW"})
    void testGetAssignmentById() throws Exception {
        mockMvc.perform(get("/api/v1/schedule-assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAssignment.getId()));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_CREATE"})
    void testCreateAssignment() throws Exception {
        Staff newStaff = new Staff();
        newStaff.setStaffCode("STF_SA_NEW");
        newStaff.setStaffType("TEACHER");
        newStaff.setFullName("New Schedule Teacher");
        newStaff.setStatus("ACTIVE");
        newStaff = staffRepository.save(newStaff);

        TeachingAssignment ta = new TeachingAssignment();
        ta.setClasses(testClass);
        ta.setTeacher(newStaff);
        ta.setRole("ASSISTANT");
        ta.setStatus("ACTIVE");
        ta.setAssignedDate(java.time.LocalDate.now());
        teachingAssignmentRepository.save(ta);

        ScheduleAssignmentDto dto = new ScheduleAssignmentDto();
        dto.setScheduleId(testSchedule.getId());
        dto.setStaffId(newStaff.getId());
        dto.setRole("ASSISTANT");

        mockMvc.perform(post("/api/v1/schedule-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Phân công giáo viên vào ca học thành công!"))
                .andExpect(jsonPath("$.data.role").value("ASSISTANT"));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_UPDATE"})
    void testUpdateAssignment() throws Exception {
        Staff updateStaff = new Staff();
        updateStaff.setStaffCode("STF_SA_UPD");
        updateStaff.setStaffType("TEACHER");
        updateStaff.setFullName("Update Schedule Teacher");
        updateStaff.setStatus("ACTIVE");
        updateStaff = staffRepository.save(updateStaff);

        TeachingAssignment ta = new TeachingAssignment();
        ta.setClasses(testClass);
        ta.setTeacher(updateStaff);
        ta.setRole("NATIVE_TEACHER");
        ta.setStatus("ACTIVE");
        ta.setAssignedDate(java.time.LocalDate.now());
        teachingAssignmentRepository.save(ta);

        ScheduleAssignmentDto updateDto = new ScheduleAssignmentDto();
        updateDto.setScheduleId(testSchedule.getId());
        updateDto.setStaffId(updateStaff.getId());
        updateDto.setRole("NATIVE_TEACHER");

        mockMvc.perform(put("/api/v1/schedule-assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật phân công ca học thành công!"))
                .andExpect(jsonPath("$.data.role").value("NATIVE_TEACHER"));
    }

    @Test
    @WithMockUser(authorities = {"ASSIGNMENT_DELETE"})
    void testDeleteAssignment() throws Exception {
        mockMvc.perform(delete("/api/v1/schedule-assignments/" + testAssignment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hủy phân công ca học thành công!"));
    }
}
