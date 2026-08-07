package com.lms.education.module.ai.repository;

import com.lms.education.module.ai.entity.AiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiDocumentRepository extends JpaRepository<AiDocument, Long> {
}
