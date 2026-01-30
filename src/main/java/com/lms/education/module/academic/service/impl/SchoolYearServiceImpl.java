package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.SchoolYearDto;
import com.lms.education.module.academic.entity.SchoolYear;
import com.lms.education.module.academic.repository.SchoolYearRepository;
import com.lms.education.module.academic.service.SchoolYearService;
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolYearServiceImpl implements SchoolYearService {

    private final SchoolYearRepository schoolYearRepository;

    @Override
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu khi ghi
    public SchoolYearDto create(SchoolYearDto dto) {
        log.info("Bắt đầu tạo năm học mới: {}", dto.getName());
        // Validate: Kiểm tra trùng tên
        if (schoolYearRepository.existsByName(dto.getName())) {
            log.warn("Tạo thất bại - Trùng tên: {}", dto.getName());
            throw new DuplicateResourceException("Tên năm học '" + dto.getName() + "' đã tồn tại!");
        }

        // Validate: Kiểm tra thời gian chồng chéo
        if (schoolYearRepository.existsOverlappingYear(dto.getStartDate(), dto.getEndDate(), null)) {
            log.warn("Tạo thất bại - Trùng thời gian: {} - {}", dto.getStartDate(), dto.getEndDate());
            throw new OperationNotPermittedException("Khoảng thời gian này bị trùng lặp với một năm học khác!");
        }

        //  Map DTO -> Entity
        SchoolYear entity = SchoolYear.builder()
                .name(dto.getName())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                // Status có thể null, nếu null thì Entity dùng default (active)
                .status(dto.getStatus() != null ? dto.getStatus() : SchoolYear.SchoolYearStatus.active)
                .build();

        SchoolYear savedEntity = schoolYearRepository.save(entity);

        //  Save & Return
        log.info("Tạo năm học thành công. ID: {}", savedEntity.getId());
        return mapToDto(savedEntity);
    }

    @Override
    @Transactional
    public SchoolYearDto update(String id, SchoolYearDto dto) {
        log.info("Cập nhật năm học ID: {}", id);
        //  Tìm bản ghi cũ
        SchoolYear existingYear = schoolYearRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Không tìm thấy năm học ID: {}", id); // Log Error
                    return new ResourceNotFoundException("Không tìm thấy năm học với ID: " + id);
                });

        //  Validate: Trùng tên (Chỉ check nếu tên thay đổi)
        if (!existingYear.getName().equals(dto.getName()) && schoolYearRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Tên năm học '" + dto.getName() + "' đã được sử dụng!");
        }

        //  Validate: Thời gian chồng chéo
        if (schoolYearRepository.existsOverlappingYear(dto.getStartDate(), dto.getEndDate(), id)) {
            throw new OperationNotPermittedException("Thời gian cập nhật bị trùng lặp với năm học khác!");
        }

        //  Cập nhật dữ liệu
        existingYear.setName(dto.getName());
        existingYear.setStartDate(dto.getStartDate());
        existingYear.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) {
            existingYear.setStatus(dto.getStatus());
        }
        log.info("Cập nhật thành công năm học: {}", existingYear.getName());
        //  Save (JPA tự hiểu là Update vì entity đã có ID)
        return mapToDto(schoolYearRepository.save(existingYear));
    }

    @Override
    @Transactional
    public void delete(List<String> ids) {
        log.info("Yêu cầu xóa {} năm học. IDs: {}", ids.size(), ids);
        //  Kiểm tra đầu vào: Phải có ít nhất 1 ID
        if (CollectionUtils.isEmpty(ids)) {
            throw new OperationNotPermittedException("Danh sách ID cần xóa không được để trống!");
        }

        //  Validate ràng buộc dữ liệu
        // Duyệt qua từng ID để đảm bảo an toàn tuyệt đối
        for (String id : ids) {
            // Check tồn tại
            if (!schoolYearRepository.existsById(id)) {
                log.error("Lỗi xóa - ID không tồn tại: {}", id);
                throw new ResourceNotFoundException("Không tìm thấy năm học với ID: " + id);
            }

            // Check ràng buộc: Năm học này đã có lớp chưa?
//            if (physicalClassRepository.existsBySchoolYearId(id)) {
//                log.warn("Chặn xóa - Năm học ID {} đang có dữ liệu lớp học", id);
//                throw new OperationNotPermittedException("Không thể xóa năm học (ID: " + id + ") vì đã có lớp học. Vui lòng Archive thay vì xóa!");
//            }
        }

        //  Xóa sạch sau khi đã kiểm tra kỹ
        schoolYearRepository.deleteAllById(ids);
        log.info("Đã xóa hoàn tất {} năm học", ids.size());
    }

    @Override
    public SchoolYearDto getById(String id) {
        log.debug("Fetching school year detail: {}", id);
        SchoolYear entity = schoolYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy năm học với ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public PageResponse<SchoolYearDto> getAll(String keyword, SchoolYear.SchoolYearStatus status, int page, int size) {
        log.info("Tìm kiếm năm học - Keyword: '{}', Status: {}, Page: {}, Size: {}", keyword, status, page, size);
        // Tạo Pageable
        // Sort mặc định theo startDate giảm dần (mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("startDate").descending());

        // Gọi Repository trả về Page Entity
        Page<SchoolYear> pageResult = schoolYearRepository.searchAndFilter(keyword, status, pageable);

        log.info("Tìm thấy {} kết quả", pageResult.getTotalElements());

        // Map từ Page<Entity> sang List<Dto>
        List<SchoolYearDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        // Đóng gói vào PageResponse
        return PageResponse.<SchoolYearDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public SchoolYearDto getCurrentSchoolYear() {
        log.debug("Lấy thông tin năm học hiện tại theo ngày: {}", LocalDate.now());
        return schoolYearRepository.findCurrentSchoolYearByDate(LocalDate.now())
                .map(this::mapToDto)
                .orElse(null); // Hoặc throw Exception nếu bắt buộc phải có năm học
    }

    @Override
    @Transactional
    public void archive(String id) {
        log.info("Yêu cầu kết thúc (lưu trữ) năm học ID: {}", id);

        // Tìm năm học
        SchoolYear year = schoolYearRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Lỗi Archive - Không tìm thấy ID: {}", id);
                    return new ResourceNotFoundException("Không tìm thấy năm học với ID: " + id);
                });

        //  Kiểm tra (Optional): Nếu đã archive rồi thì thôi hoặc báo lỗi
        if (year.getStatus() == SchoolYear.SchoolYearStatus.archived) {
            log.warn("Năm học {} đã được lưu trữ trước đó rồi.", year.getName());
            return; // Hoặc throw Exception tùy nghiệp vụ của bạn
        }

        // Cập nhật trạng thái
        year.setStatus(SchoolYear.SchoolYearStatus.archived);

        // Lưu lại
        schoolYearRepository.save(year);
        log.info("Đã kết thúc năm học: {}", year.getName());
    }

    // --- Helper Method: Map Entity -> DTO ---
    // (Nếu dùng ModelMapper/MapStruct thì có thể bỏ qua hàm này)
    private SchoolYearDto mapToDto(SchoolYear entity) {
        return SchoolYearDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
