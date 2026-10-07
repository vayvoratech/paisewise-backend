DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'learn' AND table_name = 'quiz_questions' AND column_name = 'id' AND data_type = 'uuid'
    ) THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN id TYPE VARCHAR(255) USING id::text;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'learn' AND table_name = 'quiz_questions' AND column_name = 'lesson_id' AND data_type = 'uuid'
    ) THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN lesson_id TYPE VARCHAR(255) USING lesson_id::text;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'learn' AND table_name = 'quiz_questions' AND column_name = 'correct_option_id' AND data_type = 'uuid'
    ) THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN correct_option_id TYPE VARCHAR(255) USING correct_option_id::text;
    END IF;
END $$;
