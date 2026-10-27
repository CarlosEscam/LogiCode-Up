-- RF-12: reto de bloques que configura el docente en la rúbrica de una actividad (tablero, orientación inicial,
-- máximo de bloques, bloques permitidos y programa inicial para depurar), en JSON. Va con la versión de la rúbrica:
-- cada entrega se califica con el reto vigente cuando se envió (RN-20).
ALTER TABLE rubric_version ADD COLUMN block_challenge TEXT;
