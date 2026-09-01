-- Migration: widen signature columns to TEXT
-- Only runs if columns are still VARCHAR(255)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='solicitudes_hr_recipients' AND column_name='signature_url' AND character_maximum_length=255) THEN
        ALTER TABLE solicitudes_hr_recipients ALTER COLUMN signature_url TYPE TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='solicitudes_hr_recipients' AND column_name='signed_document_url') THEN
        ALTER TABLE solicitudes_hr_recipients ADD COLUMN signed_document_url TEXT;
    END IF;
END $$;
