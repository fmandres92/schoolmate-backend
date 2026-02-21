DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'malla_curricular'
          AND column_name = 'horas_semanales'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'malla_curricular'
          AND column_name = 'horas_pedagogicas'
    ) THEN
        ALTER TABLE malla_curricular
            RENAME COLUMN horas_semanales TO horas_pedagogicas;
    END IF;
END $$;
