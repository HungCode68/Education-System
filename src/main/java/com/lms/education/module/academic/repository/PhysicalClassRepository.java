package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.PhysicalClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalClassRepository extends JpaRepository<PhysicalClass, String> {
}
