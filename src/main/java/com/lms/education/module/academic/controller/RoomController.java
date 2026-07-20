package com.lms.education.module.academic.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.room.dto.RoomDto;
import com.lms.education.module.academic.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROOM_CREATE')")
    @LogActivity(module = "ROOM", action = "CREATE", targetType = "room", description = "Tạo mới phòng học")
    public ResponseEntity<Map<String, Object>> createRoom(@Valid @RequestBody RoomDto dto) {
        RoomDto createdRoom = roomService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo phòng học thành công!");
        response.put("data", createdRoom);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_UPDATE')")
    @LogActivity(module = "ROOM", action = "UPDATE", targetType = "room", description = "Cập nhật thông tin phòng học")
    public ResponseEntity<Map<String, Object>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomDto dto) {

        RoomDto updatedRoom = roomService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin phòng học thành công!");
        response.put("data", updatedRoom);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_DELETE')")
    @LogActivity(module = "ROOM", action = "DELETE", targetType = "room", description = "Xóa phòng học")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable Long id) {
        roomService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa phòng học thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_VIEW')")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROOM_VIEW')")
    public ResponseEntity<Page<RoomDto>> getAllRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(roomService.getAllRooms(keyword, pageable));
    }
}