package com.lms.education.module.notification.repository;

import com.lms.education.module.notification.entity.ClassAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassAnnouncementRepository extends JpaRepository<ClassAnnouncement, Long> {

    List<ClassAnnouncement> findByClassesIdOrderByIsPinnedDescCreatedAtDesc(Long classId);

    Page<ClassAnnouncement> findByClassesIdOrderByIsPinnedDescCreatedAtDesc(Long classId, Pageable pageable);

    List<ClassAnnouncement> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    long countByClassesId(Long classId);
}
