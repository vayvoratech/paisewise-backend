DO $$ 
DECLARE
    col RECORD;
BEGIN
    FOR col IN 
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_schema = 'learn' 
          AND table_name = 'quiz_questions' 
          AND column_name != 'id'
          AND is_nullable = 'NO'
    LOOP
        EXECUTE format('ALTER TABLE learn.quiz_questions ALTER COLUMN %I DROP NOT NULL', col.column_name);
    END LOOP;
END $$;
