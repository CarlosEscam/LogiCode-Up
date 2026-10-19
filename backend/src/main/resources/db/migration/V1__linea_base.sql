-- LogiCode · V1 · Línea base del esquema
--
-- Primera migración versionada. Establece el punto de partida reproducible del esquema y no crea
-- tablas de negocio: las tablas de identidad, cursos, actividades, etc. se agregan en las etapas
-- correspondientes, cada una con su propia migración (V2, V3, ...).
--
-- Convenciones (ver docs/modelo-de-datos.md):
--   * Nombres de tablas y columnas en inglés, snake_case y singular (p. ej. user_account).
--   * Claves primarias UUID generadas por la aplicación o con gen_random_uuid().
--   * Fechas como TIMESTAMPTZ en UTC; la zona horaria del negocio (America/Bogota) se aplica en la aplicación.
--   * Notas como NUMERIC(2,1) con restricción 0.0 a 5.0.
--   * Nunca se edita una migración ya aplicada; los cambios van en una migración nueva.

COMMENT ON SCHEMA public IS 'LogiCode: esquema principal de la aplicación';
