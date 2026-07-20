package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}