package com.lms.education.module.academic.dto;

import lombok.Data;
import java.util.List;

@Data
public class AutoDistributeRequest {
    private List<String> studentIds; // Danh sách 500 học sinh mới tuyển
    private List<String> classIds;   // Danh sách 10 lớp trống (10A1 -> 10A10)
    private String schoolYearId;     // Năm học áp dụng
}
