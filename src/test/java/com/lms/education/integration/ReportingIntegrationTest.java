package com.lms.education.integration;

import com.lms.education.module.reporting.entity.ReportCenterStatistics;
import com.lms.education.module.reporting.repository.ReportCenterStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReportingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReportCenterStatisticsRepository repository;

    private ReportCenterStatistics testReport;

    @BeforeEach
    void setUp() {
        List<ReportCenterStatistics> reports = repository.findAll();
        if (reports.isEmpty()) {
            ReportCenterStatistics report = new ReportCenterStatistics();
            report.setReportDate(LocalDate.now());
            report.setTotalActiveStudents(100);
            report.setTotalTeachers(10);
            report.setTotalActiveClasses(5);
            testReport = repository.save(report);
        } else {
            testReport = reports.get(0);
        }
    }

    @Test
    @WithMockUser(authorities = {"REPORT_VIEW"})
    void testGetReportByDate() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/statistics/" + testReport.getReportDate())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveStudents").value(testReport.getTotalActiveStudents()));
    }

    @Test
    @WithMockUser(authorities = {"REPORT_CREATE"})
    void testSyncDailyReport() throws Exception {
        mockMvc.perform(post("/api/v1/reporting/statistics/sync")
                .param("date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đồng bộ/Chốt số liệu thống kê trung tâm thành công!"));
    }

    @Test
    @WithMockUser(authorities = {"TRAINING_VIEW"})
    void testGetTrainingDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/statistics/training-dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.centerOverview").exists());
    }
}
