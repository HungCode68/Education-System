package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.ScheduleCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleCancellationRepository extends JpaRepository<ScheduleCancellation, Long> {

    @Query("SELECT sc FROM ScheduleCancellation sc WHERE " +
           "(sc.classes.id = :classId OR sc.classes IS NULL) AND " +
           "sc.startDate <= :endDate AND sc.endDate >= :startDate")
    List<ScheduleCancellation> findActiveCancellationsForClass(
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT sc FROM ScheduleCancellation sc WHERE " +
           "(sc.classes.id = :classId OR sc.classes IS NULL)")
    List<ScheduleCancellation> findByClassIdOrCenterWide(@Param("classId") Long classId);
    
    @Query("SELECT sc FROM ScheduleCancellation sc WHERE sc.classes IS NULL")
    List<ScheduleCancellation> findCenterWideCancellations();
}
