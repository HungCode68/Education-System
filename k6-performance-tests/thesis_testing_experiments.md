# CHƯƠNG Y: DỮ LIỆU THỰC NGHIỆM VÀ KIỂM THỬ HỆ THỐNG
*(Mẫu viết luận văn Tốt nghiệp - Hãy điều chỉnh số Chương cho phù hợp)*

## 1. Dữ liệu thực nghiệm và Môi trường (Experimental Data & Environment)
**1.1. Môi trường triển khai kiểm thử**
- **Backend:** Spring Boot (Java 21), sử dụng cơ sở dữ liệu H2 (In-memory) dành riêng cho môi trường Test để đảm bảo tính độc lập và tốc độ thực thi, không làm ảnh hưởng đến dữ liệu thật.
- **Frontend:** Angular 17.
- **Công cụ kiểm thử tự động:** JUnit 5, Mockito (Backend), Cypress (Frontend E2E).

**1.2. Dữ liệu giả lập (Mock Data)**
Để thực hiện kiểm thử, đồ án đã khởi tạo một tập dữ liệu giả lập bao gồm đầy đủ các thực thể lõi của hệ thống giáo dục:
- **Tài khoản người dùng:** 4 phân quyền chính (Admin, HR, Teacher, Student).
- **Học thuật (Academic):** Khởi tạo sẵn các Lớp học, Lịch học, Khóa học và Bài tập (Assignment).
- **Dữ liệu bài tập:** Dữ liệu nộp bài (Submission) đa dạng (đúng hạn, trễ hạn, vượt quá số lần cho phép) để kiểm thử các bộ lọc và logic nghiệp vụ.

---

## 2. Kiểm thử Đơn vị và Tích hợp (Unit Test & Integration Test)

Đồ án đã thiết kế một chiến lược kiểm thử toàn diện cho phía Backend (Máy chủ) với độ phủ sóng mã (Code Coverage) cao trải dài trên 10 phân hệ lõi (`academic`, `attendance`, `enrollment`, `lms`, `notification`, `reporting`, `teaching`, v.v.).

### 2.1. Kiểm thử Đơn vị (Unit Test)
- **Phương pháp:** Sử dụng **JUnit 5** kết hợp thư viện **Mockito** để cô lập các lớp nghiệp vụ (Service Layer).
- **Thực nghiệm:** Tiến hành cô lập các Dependencies (như Repository gọi xuống CSDL) bằng kỹ thuật Mocking. Qua đó, kiểm tra độc lập tính chính xác của các thuật toán tính điểm, logic kiểm tra giới hạn nộp bài (`max_attempts`), và bộ máy chấm điểm tự động. Điều này đảm bảo mỗi hàm tính toán đều cho ra output chính xác với input dự kiến.

### 2.2. Kiểm thử Tích hợp (Integration Test)
- **Phương pháp:** Xây dựng 21 kịch bản kiểm thử tích hợp chuyên sâu (Ví dụ: `AuthIntegrationTest`, `SubmissionIntegrationTest`, `ClassScheduleIntegrationTest`).
- **Thực nghiệm:** Không sử dụng Mock, hệ thống Test sẽ gọi trực tiếp các Controller qua API (`MockMvc`), đi xuyên qua tầng Service, chạm xuống cơ sở dữ liệu (Database) thực tế và trả về kết quả. 
- **Kết quả:** Đảm bảo dữ liệu luân chuyển trơn tru giữa các lớp kiến trúc. Điển hình như `SubmissionIntegrationTest` đã chứng minh được khi Học viên nộp bài, cơ sở dữ liệu cập nhật trạng thái chính xác và đồng bộ hoàn hảo với điểm số.

---

## 3. Kiểm thử Bảo mật (Security Test)
- **Phương pháp:** Sử dụng bộ khung Security Test của Spring (`@WithMockUser`).
- **Thực nghiệm:** Trọng tâm của kiểm thử bảo mật được đặt tại lớp `LmsPermissionEvaluatorTest`. Kịch bản kiểm thử tập trung vào phân quyền kiểm soát truy cập dựa trên vai trò (RBAC - Role Based Access Control).
- **Kết quả:** Đảm bảo tính đóng gói dữ liệu nghiêm ngặt: Học viên A không thể xem hoặc can thiệp vào bài nộp của Học viên B; Giáo viên chỉ có quyền chấm điểm cho các lớp học được phân công giảng dạy. Việc đánh chặn các nỗ lực leo thang đặc quyền (Privilege Escalation) từ API đều trả về mã lỗi bảo mật (403 Forbidden) đúng thiết kế.

---

## 4. Kiểm thử Luồng nghiệp vụ Giao diện (End-to-End Test)
- **Phương pháp:** Sử dụng nền tảng **Cypress** để tự động hóa kiểm thử trên giao diện người dùng (Frontend).
- **Thực nghiệm:** Đồ án thiết kế 6 tập kịch bản E2E cốt lõi mô phỏng chính xác hành vi của người dùng thật (`admin.cy.ts`, `auth.cy.ts`, `hr.cy.ts`, `student.cy.ts`, `teacher.cy.ts`, `academic.cy.ts`). Công cụ Cypress sẽ tự động điều khiển trình duyệt: Tự động nhập tài khoản, click nút Đăng nhập, điều hướng đến màn hình Bài tập, thao tác chọn câu trả lời và nhấn nút Nộp bài.
- **Kết quả:** Toàn bộ chu trình từ lúc người dùng thao tác chuột trên giao diện Angular cho đến khi Backend xử lý và hiển thị lại kết quả lên màn hình đều hoạt động thông suốt. Các thành phần giao diện (UI Components) hiển thị đúng logic, các form nhập liệu đều kích hoạt thông báo lỗi (Validation) chính xác khi người dùng cố tình nhập sai định dạng.
