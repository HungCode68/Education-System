package com.lms.education.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;      // Danh sách dữ liệu của trang hiện tại
    private int pageNo;           // Số trang hiện tại
    private int pageSize;         // Kích thước trang
    private long totalElements;   // Tổng số bản ghi trong DB
    private int totalPages;       // Tổng số trang
    private boolean last;         // Có phải trang cuối không?
}
