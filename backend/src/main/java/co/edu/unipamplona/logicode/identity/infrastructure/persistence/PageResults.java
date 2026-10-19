package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.data.domain.Page;

final class PageResults {

    private PageResults() {
    }

    static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
