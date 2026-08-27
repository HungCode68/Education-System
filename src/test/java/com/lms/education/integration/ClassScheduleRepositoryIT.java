package com.lms.education.integration;

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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassScheduleRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Room testRoom;
    private Classes testClass;

    @BeforeEach
    void setUp() {
        // Room
        List<Room> rooms = roomRepository.findAll();
        if (rooms.isEmpty()) {
            Room room = new Room();
            room.setName("IT Room 1");
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
            course.setCode("IT_COURSE");
            course.setName("IT Course");
            course.setBasePrice(new java.math.BigDecimal("2000000"));
            testCourse = courseRepository.save(course);
        } else {
            testCourse = courses.get(0);
        }

        // Classes
        List<Classes> classList = classesRepository.findAll();
        if (classList.isEmpty()) {
            Classes clazz = new Classes();
            clazz.setCode("IT_CLASS");
            clazz.setName("IT Class 01");
            clazz.setCourse(testCourse);
            clazz.setMaxStudents(30);
            clazz.setStartDate(LocalDate.now().minusDays(10));
            clazz.setEndDate(LocalDate.now().plusDays(30));
            testClass = classesRepository.save(clazz);
        } else {
            testClass = classList.get(0);
        }
    }

    @Test
    void testExistsRoomConflict_ShouldReturnTrue_WhenConflict() {
        // Setup existing schedule
        ClassSchedule schedule = new ClassSchedule();
        schedule.setClasses(testClass);
        schedule.setRoom(testRoom);
        schedule.setDayOfWeek(2); // Monday
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(10, 0));
        classScheduleRepository.save(schedule);

        // Test conflict: Same room, same day, overlapping time
        boolean hasConflict = classScheduleRepository.existsRoomConflict(
                testRoom.getId(),
                2,
                LocalTime.of(9, 0), // Overlaps
                LocalTime.of(11, 0),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                null
        );

        assertThat(hasConflict).isTrue();
    }

    @Test
    void testExistsRoomConflict_ShouldReturnFalse_WhenNoConflict() {
        // Setup existing schedule
        ClassSchedule schedule = new ClassSchedule();
        schedule.setClasses(testClass);
        schedule.setRoom(testRoom);
        schedule.setDayOfWeek(2); // Monday
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(10, 0));
        classScheduleRepository.save(schedule);

        // Test no conflict: Different day
        boolean hasConflictDifferentDay = classScheduleRepository.existsRoomConflict(
                testRoom.getId(),
                3, // Tuesday
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                null
        );

        assertThat(hasConflictDifferentDay).isFalse();

        // Test no conflict: Same day but different time
        boolean hasConflictDifferentTime = classScheduleRepository.existsRoomConflict(
                testRoom.getId(),
                2,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                null
        );

        assertThat(hasConflictDifferentTime).isFalse();
    }
}
