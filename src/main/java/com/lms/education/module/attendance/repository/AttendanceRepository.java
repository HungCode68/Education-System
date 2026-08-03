package com.lms.education.module.attendance.repository;

import com.lms.education.module.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByScheduleIdAndAttendanceDate(Long scheduleId, LocalDate attendanceDate);

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findByStudentIdAndAttendanceDateBetween(Long studentId, LocalDate startDate, LocalDate endDate);

    Optional<Attendance> findByScheduleIdAndStudentIdAndAttendanceDate(Long scheduleId, Long studentId, LocalDate attendanceDate);

    boolean existsByScheduleIdAndStudentIdAndAttendanceDate(Long scheduleId, Long studentId, LocalDate attendanceDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.schedule.classes.id = :classId")
    long countTotalAttendanceByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.schedule.classes.id = :classId AND UPPER(a.status) IN ('PRESENT', 'LATE', 'EXCUSED')")
    long countPresentAttendanceByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.schedule.classes.id = :classId AND a.attendanceDate BETWEEN :startDate AND :endDate")
    long countTotalAttendanceByClassIdInRange(@Param("classId") Long classId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.schedule.classes.id = :classId AND a.attendanceDate BETWEEN :startDate AND :endDate AND UPPER(a.status) IN ('PRESENT', 'LATE', 'EXCUSED')")
    long countPresentAttendanceByClassIdInRange(@Param("classId") Long classId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
