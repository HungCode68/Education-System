package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findByClassesId(Long classId);
    
    boolean existsByRoomId(Long roomId);

    // Kiểm tra trùng lịch của phòng học
    @Query("SELECT COUNT(cs) > 0 FROM ClassSchedule cs JOIN cs.classes c WHERE " +
           "cs.room.id = :roomId AND " +
           "cs.dayOfWeek = :dayOfWeek AND " +
           "cs.startTime < :endTime AND " +
           "cs.endTime > :startTime AND " +
           "(:startDate IS NULL OR c.endDate IS NULL OR c.endDate >= :startDate) AND " +
           "(:endDate IS NULL OR c.startDate IS NULL OR c.startDate <= :endDate) AND " +
           "(:excludeId IS NULL OR cs.id != :excludeId)")
    boolean existsRoomConflict(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );

    // Kiểm tra trùng lịch của chính lớp học đó
    @Query("SELECT COUNT(cs) > 0 FROM ClassSchedule cs JOIN cs.classes c WHERE " +
           "cs.classes.id = :classId AND " +
           "cs.dayOfWeek = :dayOfWeek AND " +
           "cs.startTime < :endTime AND " +
           "cs.endTime > :startTime AND " +
           "(:startDate IS NULL OR c.endDate IS NULL OR c.endDate >= :startDate) AND " +
           "(:endDate IS NULL OR c.startDate IS NULL OR c.startDate <= :endDate) AND " +
           "(:excludeId IS NULL OR cs.id != :excludeId)")
    boolean existsClassConflict(
            @Param("classId") Long classId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );

    @Query("SELECT cs FROM ClassSchedule cs JOIN Enrollment e ON cs.classes.id = e.classes.id " +
           "WHERE e.student.id = :studentId AND e.status = 'ACTIVE'")
    List<ClassSchedule> findSchedulesByStudentId(@Param("studentId") Long studentId);
}
