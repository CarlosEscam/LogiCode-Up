package co.edu.unipamplona.logicode.forum.application;

import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumPost;
import co.edu.unipamplona.logicode.forum.domain.ForumThread;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-27, RN-15: el docente y las cuentas Administrador (ordinarias o con privilegio completo) fijan, cierran y
 * eliminan hilos y mensajes. Eliminar oculta el contenido y registra quién lo hizo.
 */
@Service
public class ModerateForum {

    private final ForumThreadRepository threads;
    private final ForumPostRepository posts;
    private final BrowseForum browse;
    private final Clock clock;

    public ModerateForum(ForumThreadRepository threads, ForumPostRepository posts, BrowseForum browse, Clock clock) {
        this.threads = threads;
        this.posts = posts;
        this.browse = browse;
        this.clock = clock;
    }

    /** Fija o suelta y cierra o reabre el hilo; un valor nulo no cambia. */
    @Transactional
    public ForumViews.ThreadDetail update(Actor actor, UUID threadId, Boolean pinned, Boolean closed) {
        actor.require(Permission.MODERATE_FORUM);
        ForumThread thread = BrowseForum.requireVisible(threads, threadId);
        thread.moderate(pinned, closed, actor.userId(), clock.instant());
        threads.save(thread);
        return browse.thread(actor, threadId);
    }

    @Transactional
    public void deleteThread(Actor actor, UUID threadId) {
        actor.require(Permission.MODERATE_FORUM);
        ForumThread thread = BrowseForum.requireVisible(threads, threadId);
        thread.delete(actor.userId(), clock.instant());
        threads.save(thread);
    }

    @Transactional
    public void deletePost(Actor actor, UUID postId) {
        actor.require(Permission.MODERATE_FORUM);
        ForumPost post = posts.findById(postId).filter(p -> !p.isDeleted())
                .orElseThrow(() -> ApplicationException.notFound("El mensaje no existe."));
        ForumThread thread = BrowseForum.requireVisible(threads, post.getThreadId());
        post.delete(actor.userId(), clock.instant());
        posts.save(post);
        thread.replyRemoved(clock.instant());
        threads.save(thread);
    }
}
