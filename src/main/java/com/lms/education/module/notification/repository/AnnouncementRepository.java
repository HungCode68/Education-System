package com.lms.education.module.notification.repository;

import com.lms.education.module.notification.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, String> {

    // Lấy danh sách thông báo cho lớp Offline (Sắp xếp theo thời gian giảm dần)
    Page<Announcement> findByPhysicalClassIdOrderByPublishedAtDesc(String physicalClassId, Pageable pageable);

    // Lấy danh sách thông báo cho lớp Online (Sắp xếp theo thời gian giảm dần)
    Page<Announcement> findByOnlineClassIdOrderByPublishedAtDesc(String onlineClassId, Pageable pageable);

}
