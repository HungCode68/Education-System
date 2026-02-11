
CREATE TABLE teaching_substitutions (
                                        id VARCHAR(36) PRIMARY KEY,


                                        assignment_id VARCHAR(36) NOT NULL,


                                        sub_teacher_id VARCHAR(36) NOT NULL,

                                        start_date DATE NOT NULL,
                                        end_date DATE NOT NULL,

                                        reason TEXT COMMENT 'Lý do dạy thay (VD: Cô A nghỉ thai sản)',


                                        status VARCHAR(20) DEFAULT 'approved',

                                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_sub_assignment FOREIGN KEY (assignment_id) REFERENCES teaching_assignments(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_sub_teacher FOREIGN KEY (sub_teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
);



CREATE TABLE teaching_assignment_history (
                                             id VARCHAR(36) PRIMARY KEY,


                                             assignment_id VARCHAR(36) NOT NULL,


                                             old_teacher_id VARCHAR(36),


                                             new_teacher_id VARCHAR(36),


                                             action_type VARCHAR(50) NOT NULL,

                                             reason TEXT COMMENT 'Lý do thay đổi',


                                             changed_by VARCHAR(36),

                                             changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT fk_hist_assignment FOREIGN KEY (assignment_id) REFERENCES teaching_assignments(id) ON DELETE CASCADE,
                                             CONSTRAINT fk_hist_old_teacher FOREIGN KEY (old_teacher_id) REFERENCES teachers(id) ON DELETE SET NULL,
                                             CONSTRAINT fk_hist_new_teacher FOREIGN KEY (new_teacher_id) REFERENCES teachers(id) ON DELETE SET NULL

);