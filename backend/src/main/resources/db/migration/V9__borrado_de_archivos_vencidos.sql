-- LogiCode · V9 · Borrado automático de archivos vencidos (RN-21, RNF-12)
-- Cuando pasa retain_until y ningún módulo usa el archivo, se borra su contenido y se anota cuándo.
-- El registro se conserva porque materiales y entregas lo siguen referenciando.

ALTER TABLE stored_file ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX stored_file_expiring_ix ON stored_file (retain_until) WHERE deleted_at IS NULL;
