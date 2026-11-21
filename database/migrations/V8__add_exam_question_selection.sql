ALTER TABLE exams
    ADD COLUMN selection_mode VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    ADD COLUMN question_count INT NULL,
    ADD CONSTRAINT ck_exam_selection CHECK (
        (selection_mode = 'FIXED' AND question_count IS NULL)
        OR (selection_mode = 'RANDOM_PER_ATTEMPT' AND question_count IS NOT NULL AND question_count > 0)
    );
