# ĐÁNH GIÁ HIỆU NĂNG HỆ THỐNG
*(Tập trung chuyên sâu vào 3 kỹ thuật kiểm thử)*

Quá trình kiểm thử hiệu năng phần mềm được thực hiện nhằm đánh giá giới hạn chịu tải, độ trễ và tính ổn định của Hệ thống Quản lý Trung tâm Tiếng Anh thông qua 3 kỹ thuật cốt lõi: Load Testing, Spike Testing và Stress Testing.

---

## 1. Kiểm thử Tải (Load Testing)

**Lý thuyết:** 
Kiểm thử tải là phương pháp đánh giá hệ thống bằng cách tạo ra một lượng truy cập mô phỏng mức độ sử dụng thông thường trong điều kiện thực tế. Mục đích cốt lõi là để xác nhận hệ thống có khả năng duy trì hoạt động ổn định và đáp ứng các tiêu chuẩn hiệu năng trong một thời gian dài.

**Kết quả đánh giá:** 
- **Kịch bản:** Giả lập duy trì liên tục **20 người dùng đồng thời** thao tác trên hệ thống trong thời gian **5 phút**.
- **Số liệu đạt được:** Hệ thống tiếp nhận và xử lý mượt mà gần **2.800 HTTP Requests**. Thời gian phản hồi trung bình (Avg Response Time) cực kỳ ấn tượng, đạt **135.94 ms**. Quan trọng nhất, tỷ lệ lỗi (Error Rate) duy trì ở mức tuyệt đối **0.00%**.
- **Đánh giá:** Mã nguồn tối ưu hóa cơ sở dữ liệu rất tốt, không xảy ra hiện tượng rò rỉ bộ nhớ (Memory Leak) khi duy trì tải trọng. Mức chịu tải này hoàn toàn đáp ứng được nhu cầu vận hành của một trung tâm tiếng Anh quy mô vừa và nhỏ.

---

## 2. Kiểm thử Đột biến (Spike Testing)

**Lý thuyết:** 
Kiểm thử đột biến là kỹ thuật đánh giá sức chịu đựng của phần mềm bằng cách gia tăng số lượng truy cập lên mức cực đại chỉ trong một khoảng thời gian cực kỳ ngắn. Mục đích nhằm quan sát xem hệ thống có bị sập hay có khả năng phục hồi nhanh chóng sau một cơn bão truy cập (ví dụ: thời điểm học viên ồ ạt truy cập vào làm bài thi) hay không.

**Kết quả đánh giá:**
- **Kịch bản:** Tăng lưu lượng truy cập đột biến từ 0 lên **100 người dùng đồng thời chỉ trong vỏn vẹn 10 giây**.
- **Số liệu đạt được:** Do tải trọng tăng quá nhanh, tỷ lệ lỗi (Error Rate) tăng vọt lên mức **~29.39%**. 
- **Đánh giá:** Hệ thống đã bộc lộ "nút thắt cổ chai" (Bottleneck) ở chức năng **Đăng nhập (Authentication)**. Các log ghi nhận 100% lỗi phát sinh do quá tải CPU khi phải chạy thuật toán băm mật khẩu `Bcrypt` cho hàng trăm request cùng một lúc, dẫn đến hiện tượng từ chối dịch vụ cục bộ (Timeout). Tuy nhiên, ưu điểm là các luồng nghiệp vụ khác (nộp bài tập) vẫn an toàn và không bị mất mát dữ liệu.

---

## 3. Kiểm thử Căng thẳng (Stress Testing)

**Lý thuyết:** 
Kiểm thử căng thẳng là phương pháp gia tăng áp lực tải trọng một cách liên tục, vượt quá khả năng thiết kế của hệ thống cho đến khi hệ thống bắt đầu phát sinh lỗi. Mục đích chính của kỹ thuật này là để xác định "Điểm gãy" (Breakpoint) - ngưỡng tối đa phần cứng có thể chịu đựng trước khi ngừng hoạt động.

**Kết quả đánh giá:**
- **Kịch bản:** Tăng đều lượng người dùng từ 0 lên 100 truy cập đồng thời, duy trì bóp nghẹt tài nguyên máy chủ liên tục trong vòng **10 phút**.
- **Số liệu đạt được:** Thời gian phản hồi trung bình tăng cao. Tỷ lệ lỗi tổng thể ghi nhận cuối bài test ở mức **~24.34%**.
- **Đánh giá:** Theo dõi trên biểu đồ thực tế, hệ thống vẫn phục vụ ổn định và không phát sinh lỗi cho đến khi chạm mốc **~60 người dùng tương tác đồng thời**. Ngay khi vượt qua mốc này, tài nguyên CPU (1 Core) bị vắt kiệt 100% do thuật toán mã hóa, kéo theo hàng loạt các lỗi rớt kết nối. Do đó, có thể kết luận chính xác "Điểm gãy" của máy chủ cấu hình hiện tại được xác định ở mức **60 CCU**.
