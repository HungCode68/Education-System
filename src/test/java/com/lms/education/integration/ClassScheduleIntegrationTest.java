package com.lms.education.integration;

import com.lms.education.module.academic.dto.ClassScheduleDto;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.entity.Room;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.academic.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ClassScheduleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    private ClassSchedule testSchedule;
    private Classes testClass;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        // Prepare Room
        List<Room> rooms = roomRepository.findAll();
        if (rooms.isEmpty()) {
            Room room = new Room();
            room.setName("Room 101");
            room.setCapacity(30);
            testRoom = roomRepository.save(room);
        } else {
            testRoom = rooms.get(0);
        }

        // Prepare Course
        List<Course> courses = courseRepository.findAll();
        Course testCourse;
        if (courses.isEmpty()) {
            Course course = new Course();
            course.setCode("COURSE_SCHED");
            course.setName("Schedule Test Course");
            course.setBasePrice(new java.math.BigDecimal("1000000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Prepare Classes
        List<Classes> classesList = classesRepository.findAll();
        if (classesList.isEmpty()) {
            Classes newClass = new Classes();
            newClass.setCode("CLASS_SCHED");
            newClass.setName("Schedule Test Class");
            newClass.setCourse(testCourse);
            newClass.setMaxStudents(30);
            newClass.setStatus("OPENING");
            testClass = classesRepository.save(newClass);
        } else {
            testClass = classesList.get(0);
        }

        // Prepare ClassSchedule
        List<ClassSchedule> schedules = classScheduleRepository.findAll();
        if (schedules.isEmpty()) {
            ClassSchedule schedule = new ClassSchedule();
            schedule.setClasses(testClass);
            schedule.setRoom(testRoom);
            schedule.setDayOfWeek(2); // Monday
            schedule.setStartTime(LocalTime.of(8, 0));
            schedule.setEndTime(LocalTime.of(10, 0));
            testSchedule = classScheduleRepository.save(schedule);
        } else {
            testSchedule = schedules.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"SCHEDULE_VIEW"})
    void testGetScheduleById() throws Exception {
        mockMvc.perform(get("/api/v1/class-schedules/" + testSchedule.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSchedule.getId()));
    }

    @Test
    @WithMockUser(authorities = {"SCHEDULE_CREATE"})
    void testCreateSchedule() throws Exception {
        ClassScheduleDto newSchedule = new ClassScheduleDto();
        newSchedule.setClassId(testClass.getId());
        newSchedule.setRoomId(testRoom.getId());
        newSchedule.setDayOfWeek(3); // Tuesday
        newSchedule.setStartTime(LocalTime.of(13, 0));
        newSchedule.setEndTime(LocalTime.of(15, 0));

        mockMvc.perform(post("/api/v1/class-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSchedule)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo lịch học thành công!"))
                .andExpect(jsonPath("$.data.dayOfWeek").value(3));
    }

    @Test
    @WithMockUser(authorities = {"SCHEDULE_UPDATE"})
    void testUpdateSchedule() throws Exception {
        ClassScheduleDto updateDto = new ClassScheduleDto();
        updateDto.setClassId(testClass.getId());
        updateDto.setRoomId(testRoom.getId());
        updateDto.setDayOfWeek(4); // Wednesday
        updateDto.setStartTime(LocalTime.of(8, 0));
        updateDto.setEndTime(LocalTime.of(10, 0));

        mockMvc.perform(put("/api/v1/class-schedules/" + testSchedule.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin lịch học thành công!"))
                .andExpect(jsonPath("$.data.dayOfWeek").value(4));
    }

    @Test
    @WithMockUser(authorities = {"SCHEDULE_DELETE"})
    void testDeleteSchedule() throws Exception {
        mockMvc.perform(delete("/api/v1/class-schedules/" + testSchedule.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa lịch học thành công!"));
    }
}
