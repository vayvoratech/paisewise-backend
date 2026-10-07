DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='learn' AND table_name='quiz_questions' AND column_name='question_en') THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN question_en DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='learn' AND table_name='quiz_questions' AND column_name='explanation_en') THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN explanation_en DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='learn' AND table_name='quiz_questions' AND column_name='options_en') THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN options_en DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='learn' AND table_name='quiz_questions' AND column_name='topic') THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN topic DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='learn' AND table_name='quiz_questions' AND column_name='difficulty') THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN difficulty DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='learn' AND table_name='quiz_questions' AND column_name='question_type') THEN
        ALTER TABLE learn.quiz_questions ALTER COLUMN question_type DROP NOT NULL;
    END IF;
END $$;
