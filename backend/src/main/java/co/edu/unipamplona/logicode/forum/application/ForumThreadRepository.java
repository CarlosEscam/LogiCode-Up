package co.edu.unipamplona.logicode.forum.application;

import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumCategory;
import co.edu.unipamplona.logicode.forum.domain.ForumThread;
import co.edu.unipamplona.logicode.shared.application.PageResult;

public interface ForumThreadRepository {

    ForumThread save(ForumThread thread);

    Optional<ForumThread> findById(UUID id);

    /**
     * Hilos no eliminados, fijados primero y luego por última actividad. {@code category} y {@code text} (título o
     * mensaje inicial) son opcionales.
     */
    PageResult<ForumThread> search(ForumCategory category, String text, int page, int size);
}
