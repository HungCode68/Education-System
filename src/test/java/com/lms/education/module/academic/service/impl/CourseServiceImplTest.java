package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.CourseDto;
import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.academic.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course mockCourse;
    private CourseDto mockCourseDto;

    @BeforeEach
    void setUp() {
        mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setCode("ENG101");
        mockCourse.setName("Basic English");
        mockCourse.setStatus("ACTIVE");
        mockCourse.setDurationHours(20);
        mockCourse.setTotalSessions(10);
        mockCourse.setSessionsPerWeek(2);

        mockCourseDto = CourseDto.builder()
                .code("eng101")
                .name("Basic English")
                .status("ACTIVE")
                .durationHours(20)
                .totalSessions(10)
                .sessionsPerWeek(2)
                .build();
    }

    @Test
    void create_Success_WithValues() {
        when(courseRepository.existsByCode("ENG101")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

        CourseDto result = courseService.create(mockCourseDto);

        assertNotNull(result);
        assertEquals("ENG101", result.getCode());
    }

    @Test
    void create_Success_WithNullDefaults() {
        mockCourseDto.setDurationHours(null);
        mockCourseDto.setTotalSessions(null);
        mockCourseDto.setSessionsPerWeek(null);
        mockCourseDto.setStatus(null);

        when(courseRepository.existsByCode("ENG101")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

        CourseDto result = courseService.create(mockCourseDto);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(courseRepository).save(argThat(course -> 
            course.getDurationHours() == 0 &&
            course.getTotalSessions() == 0 &&
            course.getSessionsPerWeek() == 0
        ));
    }

    @Test
    void create_DuplicateCode_ThrowsException() {
        when(courseRepository.existsByCode("ENG101")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> courseService.create(mockCourseDto));
    }

    @Test
    void update_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(mockCourse));
        mockCourseDto.setCode("ENG102");
        when(courseRepository.existsByCode("ENG102")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

        CourseDto result = courseService.update(1L, mockCourseDto);

        assertNotNull(result);
        verify(courseRepository).save(mockCourse);
    }

    @Test
    void update_Success_WithNullDefaults() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(mockCourse));
        mockCourseDto.setDurationHours(null);
        mockCourseDto.setTotalSessions(null);
        mockCourseDto.setSessionsPerWeek(null);
        
        when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

        courseService.update(1L, mockCourseDto);

        verify(courseRepository).save(argThat(course -> 
            course.getDurationHours() == 0 &&
            course.getTotalSessions() == 0 &&
            course.getSessionsPerWeek() == 0
        ));
    }

    @Test
    void update_DuplicateCode_ThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(mockCourse));
        mockCourseDto.setCode("ENG102");
        when(courseRepository.existsByCode("ENG102")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> courseService.update(1L, mockCourseDto));
    }

    @Test
    void update_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> courseService.update(1L, mockCourseDto));
    }

    @Test
    void delete_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(mockCourse));
        courseService.delete(1L);
        verify(courseRepository).delete(mockCourse);
    }

    @Test
    void delete_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> courseService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(mockCourse));
        CourseDto result = courseService.getById(1L);
        assertEquals("ENG101", result.getCode());
    }

    @Test
    void getById_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> courseService.getById(1L));
    }

    @Test
    void getByCode_Success() {
        when(courseRepository.findByCode("ENG101")).thenReturn(Optional.of(mockCourse));
        CourseDto result = courseService.getByCode("eng101 ");
        assertEquals(1L, result.getId());
    }

    @Test
    void getByCode_CourseNotFound_ThrowsException() {
        when(courseRepository.findByCode("ENG101")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> courseService.getByCode("ENG101"));
    }

    @Test
    void getAllCourses_WithKeyword() {
        Page<Course> page = new PageImpl<>(List.of(mockCourse));
        Pageable pageable = PageRequest.of(0, 10);
        when(courseRepository.searchCourses("ENG", pageable)).thenReturn(page);

        Page<CourseDto> result = courseService.getAllCourses("ENG", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllCourses_WithoutKeyword() {
        Page<Course> page = new PageImpl<>(List.of(mockCourse));
        Pageable pageable = PageRequest.of(0, 10);
        when(courseRepository.findAll(pageable)).thenReturn(page);

        Page<CourseDto> result = courseService.getAllCourses(null, pageable);

        assertEquals(1, result.getTotalElements());
    }
}
