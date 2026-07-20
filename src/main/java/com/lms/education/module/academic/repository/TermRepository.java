package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Term;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

    boolean existsByCode(String code);

    Optional<Term> findByCode(String code);

    @Query("SELECT t FROM Term t WHERE " +
           "LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Term> searchTerms(@Param("keyword") String keyword, Pageable pageable);
}
