package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.RoomDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomService {

    RoomDto create(RoomDto dto);

    RoomDto update(Long id, RoomDto dto);

    void delete(Long id);

    RoomDto getById(Long id);

    Page<RoomDto> getAllRooms(String keyword, Pageable pageable);

    java.util.List<RoomDto> getAvailableRooms(Long classId, Integer dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime, Long excludeScheduleId);
}