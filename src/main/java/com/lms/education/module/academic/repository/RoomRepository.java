package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Kiểm tra trùng lặp tên phòng
    boolean existsByName(String name);

    // Tìm kiếm phòng học theo tên
    Optional<Room> findByName(String name);

    // Hỗ trợ ô Search: Tìm theo Tên phòng học
    @Query("SELECT r FROM Room r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Room> searchRooms(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.id NOT IN (" +
           "SELECT cs.room.id FROM ClassSchedule cs JOIN cs.classes c WHERE cs.room IS NOT NULL " +
           "AND cs.dayOfWeek = :dayOfWeek AND cs.startTime < :endTime AND cs.endTime > :startTime " +
           "AND (:startDate IS NULL OR c.endDate IS NULL OR c.endDate >= :startDate) " +
           "AND (:endDate IS NULL OR c.startDate IS NULL OR c.startDate <= :endDate) " +
           "AND (:excludeId IS NULL OR cs.id != :excludeId))")
    List<Room> findAvailableRooms(
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );
}