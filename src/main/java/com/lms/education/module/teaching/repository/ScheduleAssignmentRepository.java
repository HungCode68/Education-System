package com.lms.education.module.teaching.repository;

import com.lms.education.module.teaching.entity.ScheduleAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignment, Long> {

    boolean existsByScheduleIdAndTeacherId(Long scheduleId, Long teacherId);

    List<ScheduleAssignment> findByScheduleId(Long scheduleId);

    List<ScheduleAssignment> findByTeacherId(Long teacherId);

    List<ScheduleAssignment> findByScheduleClassesId(Long classId);

    @Query("SELECT COUNT(sa) > 0 FROM ScheduleAssignment sa WHERE " +
           "sa.schedule.classes.id = :classId AND " +
           "(:userId IS NOT NULL AND sa.teacher.user.id = :userId OR :staffId IS NOT NULL AND sa.teacher.id = :staffId)")
    boolean isTeacherAssignedToClass(@Param("classId") Long classId, @Param("userId") Long userId, @Param("staffId") Long staffId);

    @Query("SELECT COUNT(sa) > 0 FROM ScheduleAssignment sa WHERE " +
           "sa.teacher.id = :teacherId AND " +
           "sa.schedule.dayOfWeek = :dayOfWeek AND " +
           "sa.schedule.startTime < :endTime AND " +
           "sa.schedule.endTime > :startTime AND " +
           "(:excludeAssignmentId IS NULL OR sa.id != :excludeAssignmentId)")
    boolean existsTeacherConflict(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("excludeAssignmentId") Long excludeAssignmentId
    );

    @Query("SELECT sa FROM ScheduleAssignment sa WHERE " +
           "LOWER(sa.teacher.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(sa.teacher.staffCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(sa.schedule.classes.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(sa.schedule.classes.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ScheduleAssignment> searchAssignments(@Param("keyword") String keyword, Pageable pageable);
}
