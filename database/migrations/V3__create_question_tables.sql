-- MySQL 8.4+ / 9.6; execute once, in V1..V7 order, on an explicitly selected EMPTY database.
-- No CREATE DATABASE, USE, DROP, or automatic migration runner. DDL auto-commits.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- Topic: topics
CREATE TABLE topics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    parent_topic_id BIGINT NULL,
    topic_code VARCHAR(30) NOT NULL,
    topic_name VARCHAR(150) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_topics_1 UNIQUE (subject_id, topic_code),
    CONSTRAINT fk_topics_1 FOREIGN KEY (subject_id)
        REFERENCES subjects (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_topics_1 (parent_topic_id),
    CONSTRAINT fk_topics_2 FOREIGN KEY (parent_topic_id)
        REFERENCES topics (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_topics_1 CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- QuestionBank: question_banks
CREATE TABLE question_banks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    bank_name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_question_banks_1 (subject_id),
    CONSTRAINT fk_question_banks_1 FOREIGN KEY (subject_id)
        REFERENCES subjects (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_question_banks_2 (owner_id),
    CONSTRAINT fk_question_banks_2 FOREIGN KEY (owner_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_question_banks_1 CHECK (visibility IN ('PRIVATE','SHARED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Question: questions
CREATE TABLE questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bank_id BIGINT NOT NULL,
    topic_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_questions_1 (bank_id, status, difficulty),
    CONSTRAINT fk_questions_1 FOREIGN KEY (bank_id)
        REFERENCES question_banks (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_questions_2 (topic_id),
    CONSTRAINT fk_questions_2 FOREIGN KEY (topic_id)
        REFERENCES topics (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_questions_3 (author_id),
    CONSTRAINT fk_questions_3 FOREIGN KEY (author_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_questions_1 CHECK (question_type IN ('SINGLE_CHOICE','MULTIPLE_CHOICE')),
    CONSTRAINT ck_questions_2 CHECK (difficulty IN ('EASY','MEDIUM','HARD')),
    CONSTRAINT ck_questions_3 CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- QuestionVersion: question_versions
CREATE TABLE question_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    content LONGTEXT NOT NULL,
    explanation LONGTEXT NULL,
    default_points DECIMAL(7,2) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_question_versions_1 UNIQUE (question_id, version_no),
    CONSTRAINT fk_question_versions_1 FOREIGN KEY (question_id)
        REFERENCES questions (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_question_versions_1 (created_by),
    CONSTRAINT fk_question_versions_2 FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_question_versions_1 CHECK (version_no >= 1),
    CONSTRAINT ck_question_versions_2 CHECK (default_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- QuestionVersionOption: question_version_options
CREATE TABLE question_version_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_version_id BIGINT NOT NULL,
    option_content LONGTEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order SMALLINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_question_version_options_1 UNIQUE (question_version_id, display_order),
    CONSTRAINT uq_question_version_options_2 UNIQUE (id, question_version_id),
    CONSTRAINT fk_question_version_options_1 FOREIGN KEY (question_version_id)
        REFERENCES question_versions (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_question_version_options_1 CHECK (is_correct IN (0, 1)),
    CONSTRAINT ck_question_version_options_2 CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- QuestionAttachment: question_attachments
CREATE TABLE question_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_version_id BIGINT NOT NULL,
    option_id BIGINT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_question_attachments_1 (question_version_id),
    CONSTRAINT fk_question_attachments_1 FOREIGN KEY (question_version_id)
        REFERENCES question_versions (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_question_attachments_2 (option_id, question_version_id),
    CONSTRAINT fk_question_attachments_2 FOREIGN KEY (option_id, question_version_id)
        REFERENCES question_version_options (id, question_version_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_question_attachments_1 CHECK (size_bytes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
