package co.edu.unipamplona.logicode.forum.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumPost;

public interface ForumPostRepository {

    ForumPost save(ForumPost post);

    Optional<ForumPost> findById(UUID id);

    /** Respuestas no eliminadas del hilo, de la más antigua a la más reciente. */
    List<ForumPost> findVisibleByThread(UUID threadId);
}
