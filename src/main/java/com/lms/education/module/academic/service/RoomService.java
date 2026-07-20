package com.lms.education.module.academic.service;

import com.lms.education.module.room.dto.RoomDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomService {

    RoomDto create(RoomDto dto);

    RoomDto update(Long id, RoomDto dto);

    void delete(Long id);

    RoomDto getById(Long id);

    Page<RoomDto> getAllRooms(String keyword, Pageable pageable);
}