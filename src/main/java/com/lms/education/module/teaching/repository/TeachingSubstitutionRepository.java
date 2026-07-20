package com.lms.education.module.teaching.repository;

import com.lms.education.module.teaching.entity.TeachingSubstitution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingSubstitutionRepository extends JpaRepository<TeachingSubstitution, Long> {

    List<TeachingSubstitution> findByScheduleId(Long scheduleId);

    List<TeachingSubstitution> findBySubstituteStaffId(Long staffId);

    List<TeachingSubstitution> findByAbsentStaffId(Long staffId);

    List<TeachingSubstitution> findByScheduleClassesId(Long classId);

    @Query("SELECT COUNT(ts) > 0 FROM TeachingSubstitution ts WHERE " +
           "ts.schedule.classes.id = :classId AND " +
           "(:userId IS NOT NULL AND ts.substituteStaff.user.id = :userId OR :staffId IS NOT NULL AND ts.substituteStaff.id = :staffId) AND " +
           "ts.status = 'APPROVED'")
    boolean isTeacherSubstitutingForClass(@Param("classId") Long classId, @Param("userId") Long userId, @Param("staffId") Long staffId);

    @Query("SELECT COUNT(ts) > 0 FROM TeachingSubstitution ts WHERE " +
           "ts.substituteStaff.id = :staffId AND " +
           "ts.schedule.dayOfWeek = :dayOfWeek AND " +
           "ts.schedule.startTime < :endTime AND " +
           "ts.schedule.endTime > :startTime AND " +
           "ts.startDate <= :endDate AND " +
           "ts.endDate >= :startDate AND " +
           "ts.status = 'APPROVED' AND " +
           "(:excludeSubstitutionId IS NULL OR ts.id != :excludeSubstitutionId)")
    boolean existsSubstituteConflict(
            @Param("staffId") Long staffId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("excludeSubstitutionId") Long excludeSubstitutionId
    );

    @Query("SELECT ts FROM TeachingSubstitution ts WHERE " +
           "LOWER(ts.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ts.absentStaff.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ts.substituteStaff.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ts.schedule.classes.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ts.schedule.classes.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<TeachingSubstitution> searchSubstitutions(@Param("keyword") String keyword, Pageable pageable);
}
