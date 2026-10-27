package co.edu.unipamplona.logicode.forum.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumCategory;
import co.edu.unipamplona.logicode.forum.domain.ForumPost;
import co.edu.unipamplona.logicode.forum.domain.ForumThread;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-26: cualquiera, con o sin sesión, lee el foro. Lo eliminado por moderación no se muestra. */
@Service
public class BrowseForum {

    static final int MAX_PAGE_SIZE = 50;

    private final ForumThreadRepository threads;
    private final ForumPostRepository posts;
    private final UserDirectory directory;

    public BrowseForum(ForumThreadRepository threads, ForumPostRepository posts, UserDirectory directory) {
        this.threads = threads;
        this.posts = posts;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public PageResult<ForumViews.ThreadSummary> threads(ForumCategory category, String text, int page, int size) {
        String query = text == null || text.isBlank() ? null : text.strip();
        PageResult<ForumThread> found = threads.search(category, query, Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE));
        Map<UUID, String> names = directory.fullNamesOf(found.items().stream().map(ForumThread::getAuthorId).toList());
        return found.map(t -> new ForumViews.ThreadSummary(t.getId(), t.getCategory(), t.getTitle(),
                names.get(t.getAuthorId()), t.isPinned(), t.isClosed(), t.getReplyCount(), t.getCreatedAt(),
                t.getLastActivityAt()));
    }

    @Transactional(readOnly = true)
    public ForumViews.ThreadDetail thread(Actor actor, UUID threadId) {
        ForumThread thread = requireVisible(threads, threadId);
        List<ForumPost> replies = posts.findVisibleByThread(threadId);
        List<UUID> authors = new ArrayList<>(replies.stream().map(ForumPost::getAuthorId).toList());
        authors.add(thread.getAuthorId());
        Map<UUID, String> names = directory.fullNamesOf(authors);
        boolean canModerate = actor != null && actor.can(Permission.MODERATE_FORUM);
        boolean canReply = actor != null && actor.can(Permission.PARTICIPATE_FORUM) && !thread.isClosed();
        return new ForumViews.ThreadDetail(thread.getId(), thread.getCategory(), thread.getTitle(), thread.getBody(),
                names.get(thread.getAuthorId()), thread.isPinned(), thread.isClosed(), thread.getCreatedAt(),
                replies.stream().map(p -> new ForumViews.PostView(p.getId(), names.get(p.getAuthorId()), p.getBody(),
                        p.getCreatedAt())).toList(),
                canReply, canModerate);
    }

    static ForumThread requireVisible(ForumThreadRepository threads, UUID threadId) {
        return threads.findById(threadId).filter(t -> !t.isDeleted())
                .orElseThrow(() -> ApplicationException.notFound("El hilo no existe."));
    }
}
