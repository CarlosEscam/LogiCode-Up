package co.edu.unipamplona.logicode.forum.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Respuesta en un hilo. Eliminarla la oculta y guarda quién lo hizo (RF-27). */
@Entity
@Table(name = "forum_post")
public class ForumPost {

    @Id
    private UUID id;

    @Column(name = "thread_id", nullable = false, updatable = false)
    private UUID threadId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(nullable = false, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected ForumPost() {
    }

    ForumPost(UUID threadId, UUID authorId, String body, Instant now) {
        this.id = UUID.randomUUID();
        this.threadId = threadId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = now;
    }

    public void delete(UUID moderator, Instant now) {
        deletedBy = moderator;
        deletedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
