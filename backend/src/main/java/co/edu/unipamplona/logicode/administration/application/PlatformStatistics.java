package co.edu.unipamplona.logicode.administration.application;

/**
 * Consulta de solo lectura sobre las tablas de todos los módulos para el panel (T-62). Nunca escribe: cada módulo
 * sigue siendo el único que cambia sus datos.
 */
public interface PlatformStatistics {

    PlatformOverview overview();
}
