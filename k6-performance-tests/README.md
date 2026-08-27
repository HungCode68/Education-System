# K6 Performance Testing

Bộ kịch bản kiểm thử hiệu năng cho hệ thống LMS. Được thiết kế với 2 luồng công việc chính:
1. **Luồng Quản lý:** Đăng nhập và xem Báo cáo trung tâm.
2. **Luồng Học viên:** Đăng nhập, bắt đầu làm bài và nộp bài.

Kịch bản hỗ trợ sẵn 3 kỹ thuật kiểm thử thông qua tham số môi trường: `load`, `stress`, và `spike`.

## Yêu Cầu
- Cài đặt `k6` trên máy của bạn: [Tải k6](https://k6.io/docs/get-started/installation/)
- Đảm bảo môi trường chạy (Server Linux 1 Core/4GB RAM) đang mở và có thể truy cập được.

## Chuẩn bị Dữ Liệu
Trước khi chạy test, đảm bảo Database của bạn có sẵn:
- 1 user admin (`admin@example.com` / `123456`)
- Các user học viên theo định dạng `student{ID}@example.com` (ví dụ `student1@example.com` đến `student100@example.com`) với mật khẩu `123456`. (Biến `__VU` trong k6 sẽ tăng dần từ 1 tùy thuộc vào số lượng Virtual Users bạn cấp).
- Có ít nhất 1 assignment với `ID = 1` đang ở trạng thái cho phép nộp bài.

## Hướng Dẫn Chạy Kịch Bản

Mở Terminal (Command Prompt hoặc PowerShell) tại thư mục `k6-performance-tests` và chạy các lệnh tương ứng.

> **Lưu ý:** Thay đổi `http://your-server-ip:8080/api/v1` thành IP và Port thực tế của server Linux.

### 1. Kiểm Thử Tải (Load Testing - Mặc định)
Giả lập một ngày làm việc bình thường của hệ thống.
```bash
k6 run -e BASE_URL="http://your-server-ip:8080/api/v1" -e TEST_TYPE=load main-test.js
```

### 2. Kiểm Thử Căng Thẳng (Stress Testing)
Ép tải hệ thống bằng cách tăng dần số lượng truy cập đến ngưỡng 100 người dùng đồng thời.
```bash
k6 run -e BASE_URL="http://your-server-ip:8080/api/v1" -e TEST_TYPE=stress main-test.js
```

### 3. Kiểm Thử Đột Biến (Spike Testing)
Kiểm tra khả năng chịu đựng của hệ thống khi có tới 100 người dùng ồ ạt thao tác cùng lúc trong vòng 10 giây.
```bash
k6 run -e BASE_URL="http://your-server-ip:8080/api/v1" -e TEST_TYPE=spike main-test.js
```

## Xem Báo Cáo
Sau khi mỗi lệnh chạy kết thúc, k6 sẽ tự động sinh ra một file HTML (ví dụ: `report-load-1698765432100.html`) ngay trong thư mục này. 
Hãy nhấp đúp vào file HTML đó để xem biểu đồ và kết quả chi tiết trên trình duyệt.
