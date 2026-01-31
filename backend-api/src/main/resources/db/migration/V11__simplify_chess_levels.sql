-- Migration to convert old 6-level system (PION, CAVALIER, FOU, TOUR, DAME, ROI)
-- to new 4-level system (A, B, C, D)
-- This migration is safe to run on fresh databases (tables may not exist yet)

DO $$
BEGIN
    -- =====================================================
    -- UPDATE CHECK CONSTRAINTS FIRST (before data inserts)
    -- =====================================================

    -- Update quiz_questions constraint
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'quiz_questions') THEN
        ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS quiz_questions_level_check;
        ALTER TABLE quiz_questions ADD CONSTRAINT quiz_questions_level_check
            CHECK (level IN ('A', 'B', 'C', 'D'));
    END IF;

    -- Update quiz_results constraint
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'quiz_results') THEN
        ALTER TABLE quiz_results DROP CONSTRAINT IF EXISTS quiz_results_determined_level_check;
        ALTER TABLE quiz_results ADD CONSTRAINT quiz_results_determined_level_check
            CHECK (determined_level IN ('A', 'B', 'C', 'D'));
    END IF;

    -- Update exercises constraint
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'exercises') THEN
        ALTER TABLE exercises DROP CONSTRAINT IF EXISTS exercises_chess_level_check;
        ALTER TABLE exercises ADD CONSTRAINT exercises_chess_level_check
            CHECK (chess_level IN ('A', 'B', 'C', 'D'));
    END IF;

    -- Update progress_tracking constraint (note: different table name from progress)
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'progress_tracking') THEN
        ALTER TABLE progress_tracking DROP CONSTRAINT IF EXISTS progress_tracking_current_level_check;
        ALTER TABLE progress_tracking ADD CONSTRAINT progress_tracking_current_level_check
            CHECK (current_level IN ('A', 'B', 'C', 'D'));
    END IF;

    -- Update courses constraint if exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'courses') THEN
        ALTER TABLE courses DROP CONSTRAINT IF EXISTS courses_grade_check;
        ALTER TABLE courses ADD CONSTRAINT courses_grade_check
            CHECK (grade IN ('A', 'B', 'C', 'D'));
    END IF;

    -- =====================================================
    -- UPDATE EXISTING DATA (for existing databases only)
    -- =====================================================

    -- Update progress table if it exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'progress') THEN
        UPDATE progress SET current_level = 'A' WHERE current_level IN ('PION', 'CAVALIER');
        UPDATE progress SET current_level = 'B' WHERE current_level = 'FOU';
        UPDATE progress SET current_level = 'C' WHERE current_level = 'TOUR';
        UPDATE progress SET current_level = 'D' WHERE current_level IN ('DAME', 'ROI');
    END IF;

    -- Update progress_tracking table if it exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'progress_tracking') THEN
        UPDATE progress_tracking SET current_level = 'A' WHERE current_level IN ('PION', 'CAVALIER');
        UPDATE progress_tracking SET current_level = 'B' WHERE current_level = 'FOU';
        UPDATE progress_tracking SET current_level = 'C' WHERE current_level = 'TOUR';
        UPDATE progress_tracking SET current_level = 'D' WHERE current_level IN ('DAME', 'ROI');
    END IF;

    -- Update courses table if it exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'courses') THEN
        UPDATE courses SET grade = 'A' WHERE grade IN ('PION', 'CAVALIER');
        UPDATE courses SET grade = 'B' WHERE grade = 'FOU';
        UPDATE courses SET grade = 'C' WHERE grade = 'TOUR';
        UPDATE courses SET grade = 'D' WHERE grade IN ('DAME', 'ROI');

        -- Reorder courses within level A (merged from PION and CAVALIER)
        WITH ranked_courses AS (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY grade ORDER BY order_in_grade) as new_order
            FROM courses
            WHERE grade = 'A'
        )
        UPDATE courses c
        SET order_in_grade = rc.new_order
        FROM ranked_courses rc
        WHERE c.id = rc.id;

        -- Reorder courses within level D (merged from DAME and ROI)
        WITH ranked_courses AS (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY grade ORDER BY order_in_grade) as new_order
            FROM courses
            WHERE grade = 'D'
        )
        UPDATE courses c
        SET order_in_grade = rc.new_order
        FROM ranked_courses rc
        WHERE c.id = rc.id;
    END IF;

    -- Update quiz_questions table if it exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'quiz_questions') THEN
        UPDATE quiz_questions SET level = 'A' WHERE level IN ('PION', 'CAVALIER');
        UPDATE quiz_questions SET level = 'B' WHERE level = 'FOU';
        UPDATE quiz_questions SET level = 'C' WHERE level = 'TOUR';
        UPDATE quiz_questions SET level = 'D' WHERE level IN ('DAME', 'ROI');

        -- Reorder quiz questions within level A
        WITH ranked_questions AS (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY level ORDER BY order_in_level) as new_order
            FROM quiz_questions
            WHERE level = 'A'
        )
        UPDATE quiz_questions q
        SET order_in_level = rq.new_order
        FROM ranked_questions rq
        WHERE q.id = rq.id;

        -- Reorder quiz questions within level D
        WITH ranked_questions AS (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY level ORDER BY order_in_level) as new_order
            FROM quiz_questions
            WHERE level = 'D'
        )
        UPDATE quiz_questions q
        SET order_in_level = rq.new_order
        FROM ranked_questions rq
        WHERE q.id = rq.id;
    END IF;

    -- Update exercises table if it exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'exercises') THEN
        UPDATE exercises SET chess_level = 'A' WHERE chess_level IN ('PION', 'CAVALIER');
        UPDATE exercises SET chess_level = 'B' WHERE chess_level = 'FOU';
        UPDATE exercises SET chess_level = 'C' WHERE chess_level = 'TOUR';
        UPDATE exercises SET chess_level = 'D' WHERE chess_level IN ('DAME', 'ROI');
    END IF;

    -- Update quiz_results table if it exists - rename columns and merge scores
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'quiz_results') THEN
        -- Check if old columns exist (migration not yet applied)
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'quiz_results' AND column_name = 'pion_score') THEN
            -- Add new columns
            ALTER TABLE quiz_results ADD COLUMN IF NOT EXISTS level_a_score INTEGER;
            ALTER TABLE quiz_results ADD COLUMN IF NOT EXISTS level_b_score INTEGER;
            ALTER TABLE quiz_results ADD COLUMN IF NOT EXISTS level_c_score INTEGER;
            ALTER TABLE quiz_results ADD COLUMN IF NOT EXISTS level_d_score INTEGER;

            -- Merge old scores into new columns
            -- A = PION + CAVALIER (average or sum of non-null values)
            UPDATE quiz_results SET level_a_score = COALESCE(pion_score, 0) + COALESCE(cavalier_score, 0);
            -- B = FOU
            UPDATE quiz_results SET level_b_score = fou_score;
            -- C = TOUR
            UPDATE quiz_results SET level_c_score = tour_score;
            -- D = DAME + ROI (average or sum of non-null values)
            UPDATE quiz_results SET level_d_score = COALESCE(dame_score, 0) + COALESCE(roi_score, 0);

            -- Drop old columns
            ALTER TABLE quiz_results DROP COLUMN IF EXISTS pion_score;
            ALTER TABLE quiz_results DROP COLUMN IF EXISTS cavalier_score;
            ALTER TABLE quiz_results DROP COLUMN IF EXISTS fou_score;
            ALTER TABLE quiz_results DROP COLUMN IF EXISTS tour_score;
            ALTER TABLE quiz_results DROP COLUMN IF EXISTS dame_score;
            ALTER TABLE quiz_results DROP COLUMN IF EXISTS roi_score;
        END IF;

        -- Update the determined_level values
        UPDATE quiz_results SET determined_level = 'A' WHERE determined_level IN ('PION', 'CAVALIER');
        UPDATE quiz_results SET determined_level = 'B' WHERE determined_level = 'FOU';
        UPDATE quiz_results SET determined_level = 'C' WHERE determined_level = 'TOUR';
        UPDATE quiz_results SET determined_level = 'D' WHERE determined_level IN ('DAME', 'ROI');
    END IF;
END $$;
