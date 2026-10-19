package co.edu.unipamplona.logicode.shared.application;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados con el formato de paginación de la API (ver docs/api/README.md).
 */
public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems, totalPages);
    }
}
