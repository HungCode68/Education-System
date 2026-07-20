-- Cập nhật bảng staffs
-- Thêm cột status với giá trị mặc định là ACTIVE
ALTER TABLE staffs
    ADD COLUMN status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL;

-- Gỡ bỏ ràng buộc NOT NULL cho user_id (Phục vụ luồng tạo hồ sơ trước, cấp tài khoản sau)
ALTER TABLE staffs
    MODIFY user_id BIGINT UNIQUE NULL;


-- Cập nhật bảng students
-- Thêm cột status với giá trị mặc định là STUDYING (Đang học)
ALTER TABLE students
    ADD COLUMN status VARCHAR(50) DEFAULT 'STUDYING' NOT NULL;

-- Gỡ bỏ ràng buộc NOT NULL cho user_id (Để sau này Học viên cũng có thể dùng luồng tự động sinh tài khoản)
ALTER TABLE students
    MODIFY user_id BIGINT UNIQUE NULL;