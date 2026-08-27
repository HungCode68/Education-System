package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.RoomDto;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Room;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ClassesRepository classesRepository;

    @Mock
    private ClassScheduleRepository classScheduleRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room mockRoom;
    private RoomDto mockRoomDto;

    @BeforeEach
    void setUp() {
        mockRoom = new Room();
        mockRoom.setId(1L);
        mockRoom.setName("ROOM A1");
        mockRoom.setCapacity(40);

        mockRoomDto = RoomDto.builder()
                .name("room a1 ")
                .capacity(40)
                .build();
    }

    @Test
    void create_Success() {
        when(roomRepository.existsByName("ROOM A1")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(mockRoom);

        RoomDto result = roomService.create(mockRoomDto);

        assertNotNull(result);
        assertEquals("ROOM A1", result.getName());
        verify(roomRepository).save(argThat(r -> r.getCapacity() == 40));
    }

    @Test
    void create_Success_DefaultCapacity() {
        mockRoomDto.setCapacity(null);
        when(roomRepository.existsByName("ROOM A1")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(mockRoom);

        roomService.create(mockRoomDto);

        verify(roomRepository).save(argThat(r -> r.getCapacity() == 30));
    }

    @Test
    void create_DuplicateName_ThrowsException() {
        when(roomRepository.existsByName("ROOM A1")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> roomService.create(mockRoomDto));
    }

    @Test
    void update_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(mockRoom));
        mockRoomDto.setName("ROOM A2");
        when(roomRepository.existsByName("ROOM A2")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(mockRoom);

        RoomDto result = roomService.update(1L, mockRoomDto);

        assertNotNull(result);
        verify(roomRepository).save(mockRoom);
    }

    @Test
    void update_DuplicateName_ThrowsException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(mockRoom));
        mockRoomDto.setName("ROOM A2");
        when(roomRepository.existsByName("ROOM A2")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> roomService.update(1L, mockRoomDto));
    }

    @Test
    void update_RoomNotFound_ThrowsException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> roomService.update(1L, mockRoomDto));
    }

    @Test
    void delete_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsByRoomId(1L)).thenReturn(false);

        roomService.delete(1L);

        verify(roomRepository).delete(mockRoom);
    }

    @Test
    void delete_RoomInUse_ThrowsException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(mockRoom));
        when(classScheduleRepository.existsByRoomId(1L)).thenReturn(true);

        assertThrows(OperationNotPermittedException.class, () -> roomService.delete(1L));
    }

    @Test
    void delete_RoomNotFound_ThrowsException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> roomService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(mockRoom));
        RoomDto result = roomService.getById(1L);
        assertEquals("ROOM A1", result.getName());
    }

    @Test
    void getById_RoomNotFound_ThrowsException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> roomService.getById(1L));
    }

    @Test
    void getAllRooms_WithKeyword() {
        Page<Room> page = new PageImpl<>(List.of(mockRoom));
        Pageable pageable = PageRequest.of(0, 10);
        when(roomRepository.searchRooms("ROOM", pageable)).thenReturn(page);

        Page<RoomDto> result = roomService.getAllRooms("ROOM", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllRooms_WithoutKeyword() {
        Page<Room> page = new PageImpl<>(List.of(mockRoom));
        Pageable pageable = PageRequest.of(0, 10);
        when(roomRepository.findAll(pageable)).thenReturn(page);

        Page<RoomDto> result = roomService.getAllRooms(null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAvailableRooms_Success() {
        Classes mockClass = new Classes();
        mockClass.setId(10L);
        mockClass.setStartDate(LocalDate.of(2024, 1, 1));
        mockClass.setEndDate(LocalDate.of(2024, 6, 1));

        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(roomRepository.findAvailableRooms(
                eq(2), any(LocalTime.class), any(LocalTime.class),
                eq(mockClass.getStartDate()), eq(mockClass.getEndDate()), eq(100L)
        )).thenReturn(List.of(mockRoom));

        List<RoomDto> result = roomService.getAvailableRooms(
                10L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0), 100L
        );

        assertEquals(1, result.size());
        assertEquals("ROOM A1", result.get(0).getName());
    }

    @Test
    void getAvailableRooms_ClassNotFound_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.getAvailableRooms(
                10L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0), 100L
        ));
    }
}
