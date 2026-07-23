package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.RoomDto;
import com.lms.education.module.academic.entity.Room;
import com.lms.education.module.academic.repository.RoomRepository;
import com.lms.education.module.academic.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public RoomDto create(RoomDto dto) {
        String formattedName = dto.getName().trim().toUpperCase();

        if (roomRepository.existsByName(formattedName)) {
            throw new DuplicateResourceException("Tên phòng học '" + formattedName + "' đã tồn tại trên hệ thống!");
        }

        Room room = Room.builder()
                .name(formattedName)
                .roomType(dto.getRoomType() != null ? dto.getRoomType().toUpperCase() : "PHYSICAL")
                .capacity(dto.getCapacity() != null ? dto.getCapacity() : 30) // Giá trị mặc định 30 chỗ
                .build();

        Room savedRoom = roomRepository.save(room);
        log.info("Đã tạo mới phòng học: {} (Sức chứa: {})", savedRoom.getName(), savedRoom.getCapacity());

        return mapToDto(savedRoom);
    }

    @Override
    @Transactional
    public RoomDto update(Long id, RoomDto dto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + id));

        String newFormattedName = dto.getName().trim().toUpperCase();

        // Kiểm tra xem có đổi tên phòng không và tên mới có bị trùng không
        if (!room.getName().equals(newFormattedName) && roomRepository.existsByName(newFormattedName)) {
            throw new DuplicateResourceException("Tên phòng học '" + newFormattedName + "' đã được sử dụng!");
        }

        room.setName(newFormattedName);

        if (dto.getRoomType() != null && !dto.getRoomType().trim().isEmpty()) {
            room.setRoomType(dto.getRoomType().toUpperCase());
        }

        if (dto.getCapacity() != null) {
            room.setCapacity(dto.getCapacity());
        }

        Room updatedRoom = roomRepository.save(room);
        log.info("Đã cập nhật thông tin phòng học ID: {}", id);

        return mapToDto(updatedRoom);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + id));

        /* * LƯU Ý MỞ RỘNG TƯƠNG LAI:
         * Khi có bảng Classes hoặc Schedules, bạn sẽ cần check xem phòng này có
         * đang được xếp lịch cho lớp học nào không. Nếu có thì chặn xóa.
         */

        roomRepository.delete(room);
        log.info("Đã xóa hoàn toàn phòng học ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDto getById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + id));
        return mapToDto(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomDto> getAllRooms(String keyword, Pageable pageable) {
        Page<Room> rooms;
        if (keyword != null && !keyword.trim().isEmpty()) {
            rooms = roomRepository.searchRooms(keyword.trim(), pageable);
        } else {
            rooms = roomRepository.findAll(pageable);
        }
        return rooms.map(this::mapToDto);
    }

    // --- Helper Method ---
    private RoomDto mapToDto(Room room) {
        return RoomDto.builder()
                .id(room.getId())
                .name(room.getName())
                .roomType(room.getRoomType())
                .capacity(room.getCapacity())
                .createdAt(room.getCreatedAt())
                .build();
    }
}