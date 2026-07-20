package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Classes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassesRepository extends JpaRepository<Classes, Long> {

    boolean existsByCode(String code);

    Optional<Classes> findByCode(String code);

    @Query("SELECT c FROM Classes c WHERE " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Classes> searchClasses(@Param("keyword") String keyword, Pageable pageable);
}
