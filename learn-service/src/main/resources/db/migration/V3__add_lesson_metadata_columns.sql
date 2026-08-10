-- V3__add_lesson_metadata_columns.sql
ALTER TABLE learn.lessons
    ADD COLUMN IF NOT EXISTS difficulty VARCHAR(50),
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS duration_minutes INTEGER;