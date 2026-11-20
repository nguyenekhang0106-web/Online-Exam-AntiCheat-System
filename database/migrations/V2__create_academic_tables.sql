-- MySQL 8.4+ / 9.6; execute once, in V1..V7 order, on an explicitly selected EMPTY database.
-- No CREATE DATABASE, USE, DROP, or automatic migration runner. DDL auto-commits.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- Subject: subjects
CREATE TABLE subjects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_code VARCHAR(30) NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_subjects_1 UNIQUE (subject_code),
    CONSTRAINT ck_subjects_1 CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- SchoolClass: classes
CREATE TABLE classes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_code VARCHAR(30) NOT NULL,
    class_name VARCHAR(150) NOT NULL,
    academic_year VARCHAR(9) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_classes_1 UNIQUE (class_code),
    CONSTRAINT ck_classes_1 CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ClassStudent: class_students
CREATE TABLE class_students (
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME(3) NOT NULL,
    left_at DATETIME(3) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (class_id, student_id),
    CONSTRAINT fk_class_students_1 FOREIGN KEY (class_id)
        REFERENCES classes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_class_students_1 (student_id),
    CONSTRAINT fk_class_students_2 FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_class_students_1 CHECK (status IN ('ACTIVE','LEFT')),
    CONSTRAINT ck_class_students_2 CHECK (left_at IS NULL OR left_at >= joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- CourseOffering: course_offerings
CREATE TABLE course_offerings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    academic_year VARCHAR(9) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_course_offerings_1 (teacher_id, academic_year, semester),
    INDEX ix_course_offerings_2 (subject_id),
    CONSTRAINT fk_course_offerings_1 FOREIGN KEY (subject_id)
        REFERENCES subjects (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_course_offerings_3 (class_id),
    CONSTRAINT fk_course_offerings_2 FOREIGN KEY (class_id)
        REFERENCES classes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_course_offerings_3 FOREIGN KEY (teacher_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_course_offerings_1 CHECK (status IN ('ACTIVE','CLOSED','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- CourseEnrollment: course_enrollments
CREATE TABLE course_enrollments (
    offering_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enrolled_at DATETIME(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    PRIMARY KEY (offering_id, student_id),
    CONSTRAINT fk_course_enrollments_1 FOREIGN KEY (offering_id)
        REFERENCES course_offerings (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_course_enrollments_1 (student_id),
    CONSTRAINT fk_course_enrollments_2 FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_course_enrollments_1 CHECK (status IN ('ENROLLED','WITHDRAWN','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
