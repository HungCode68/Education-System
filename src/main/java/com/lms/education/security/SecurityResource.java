package com.lms.education.security;

public interface SecurityResource {
    /**
     * Extracts the ID of the class this resource belongs to.
     * @return classId or null if not associated with a specific class
     */
    default Long extractClassId() {
        return null;
    }
    
    /**
     * Extracts the ID of the course this resource belongs to.
     * @return courseId or null if not associated with a specific course
     */
    default Long extractCourseId() {
        return null;
    }
}
