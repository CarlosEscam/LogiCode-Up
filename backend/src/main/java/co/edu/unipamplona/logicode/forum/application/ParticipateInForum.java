package co.edu.unipamplona.logicode.forum.application;

import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumCategory;
import co.edu.unipamplona.logicode.forum.domain.ForumThread;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-26: escribir en el foro exige sesión de una cuenta activa (estudiante, docente o Administrador). */
@Service
public class ParticipateInForum {

    private final ForumThreadRepository threads;
    private final ForumPostRepository posts;
    private final BrowseForum browse;
    private final Clock clock;

    public ParticipateInForum(ForumThreadRepository threads, ForumPostRepository posts, BrowseForum browse,
            Clock clock) {
        this.threads = threads;
        this.posts = posts;
        this.browse = browse;
        this.clock = clock;
    }

    @Transactional
    public ForumViews.ThreadDetail startThread(Actor actor, ForumCategory category, String title, String body) {
        actor.require(Permission.PARTICIPATE_FORUM);
        ForumThread thread = threads.save(new ForumThread(category, requireText(title, "el título"),
                requireText(body, "el mensaje"), actor.userId(), clock.instant()));
        return browse.thread(actor, thread.getId());
    }

    @Transactional
    public ForumViews.ThreadDetail reply(Actor actor, UUID threadId, String body) {
        actor.require(Permission.PARTICIPATE_FORUM);
        ForumThread thread = BrowseForum.requireVisible(threads, threadId);
        if (thread.isClosed()) {
            throw ApplicationException.conflict("El hilo está cerrado y no recibe más respuestas.");
        }
        posts.save(thread.reply(requireText(body, "el mensaje"), actor.userId(), clock.instant()));
        threads.save(thread);
        return browse.thread(actor, threadId);
    }

    private static String requireText(String text, String what) {
        if (text == null || text.isBlank()) {
            throw ApplicationException.businessRule("Escribe " + what + ".");
        }
        return text.strip();
    }
}
