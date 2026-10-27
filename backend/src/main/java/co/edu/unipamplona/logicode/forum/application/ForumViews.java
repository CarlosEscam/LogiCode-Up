package co.edu.unipamplona.logicode.forum.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumCategory;

/** Lo que la API muestra del foro. No incluye cédulas ni correos: solo el nombre de quien escribe. */
public final class ForumViews {

    private ForumViews() {
    }

    public record ThreadSummary(UUID id, ForumCategory category, String title, String authorName, boolean pinned,
            boolean closed, int replyCount, Instant createdAt, Instant lastActivityAt) {
    }

    public record PostView(UUID id, String authorName, String body, Instant createdAt) {
    }

    /** {@code canReply} y {@code canModerate} dicen qué puede hacer quien consulta; el backend lo vuelve a validar. */
    public record ThreadDetail(UUID id, ForumCategory category, String title, String body, String authorName,
            boolean pinned, boolean closed, Instant createdAt, List<PostView> posts, boolean canReply,
            boolean canModerate) {
    }
}
