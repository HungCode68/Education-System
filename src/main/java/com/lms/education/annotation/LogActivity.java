package com.lms.education.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // Chỉ được dán lên các hàm (Method)
@Retention(RetentionPolicy.RUNTIME) // Tồn tại trong lúc ứng dụng chạy
public @interface LogActivity {

    String module(); // Ví dụ: "STUDENT", "TEACHER", "ASSIGNMENT"

    String action(); // Ví dụ: "CREATE", "UPDATE", "DELETE"

    String targetType() default ""; // Ví dụ: "students", "teachers"

    String description() default ""; // Mô tả thêm nếu cần
}
