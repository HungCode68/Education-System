-- Thêm cột job_title vào bảng staffs
ALTER TABLE staffs ADD COLUMN job_title NVARCHAR(150);

-- Cập nhật data cũ (nếu staff_type là những text linh tinh thì chuyển vào job_title, còn staff_type set lại thành TEACHER hoặc STAFF)
-- Lưu ý: Câu này tùy thuộc vào dữ liệu, ta tạm set job_title mặc định từ staff_type hiện tại.
UPDATE staffs SET job_title = staff_type WHERE job_title IS NULL;

-- Chuẩn hóa lại staff_type (Ai là giáo viên thì set TEACHER, ngược lại set STAFF)
UPDATE staffs 
SET staff_type = 'TEACHER' 
WHERE UPPER(staff_type) LIKE '%TEACHER%' 
   OR UPPER(staff_type) LIKE '%GIẢNG%' 
   OR UPPER(staff_type) LIKE '%GIÁO%';

UPDATE staffs 
SET staff_type = 'STAFF' 
WHERE staff_type != 'TEACHER' AND staff_type != 'ADMIN';
