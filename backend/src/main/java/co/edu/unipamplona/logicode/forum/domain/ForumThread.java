package co.edu.unipamplona.logicode.forum.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Hilo del foro con su mensaje inicial. Los moderadores lo fijan arriba, lo cierran a nuevas respuestas o lo eliminan
 * (RF-27, RN-15). Eliminar lo oculta y deja quién y cuándo, para poder revisarlo después.
 */
@Entity
@Table(name = "forum_thread")
public class ForumThread {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForumCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    private boolean pinned;

    private boolean closed;

    @Column(name = "reply_count", nullable = false)
    private int replyCount;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "moderated_by")
    private UUID moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ForumThread() {
    }

    public ForumThread(ForumCategory category, String title, String body, UUID authorId, Instant now) {
        this.id = UUID.randomUUID();
        this.category = category;
        this.title = title;
        this.body = body;
        this.authorId = authorId;
        this.lastActivityAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Registra una respuesta. Un hilo cerrado no recibe más. */
    public ForumPost reply(String text, UUID author, Instant now) {
        if (closed) {
            throw new IllegalStateException("Hilo cerrado");
        }
        replyCount++;
        lastActivityAt = now;
        updatedAt = now;
        return new ForumPost(id, author, text, now);
    }

    public void replyRemoved(Instant now) {
        replyCount = Math.max(0, replyCount - 1);
        updatedAt = now;
    }

    public void moderate(Boolean pin, Boolean close, UUID moderator, Instant now) {
        if (pin != null) {
            pinned = pin;
        }
        if (close != null) {
            closed = close;
        }
        moderatedBy = moderator;
        moderatedAt = now;
        updatedAt = now;
    }

    public void delete(UUID moderator, Instant now) {
        deletedAt = now;
        moderatedBy = moderator;
        moderatedAt = now;
        updatedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public ForumCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isClosed() {
        return closed;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
