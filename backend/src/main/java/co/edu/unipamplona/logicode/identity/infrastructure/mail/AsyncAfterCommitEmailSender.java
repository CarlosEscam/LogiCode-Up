package co.edu.unipamplona.logicode.identity.infrastructure.mail;

import co.edu.unipamplona.logicode.identity.application.EmailMessage;
import co.edu.unipamplona.logicode.identity.application.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Envía el correo solo si la transacción se confirma, y en segundo plano para no hacer esperar al usuario
 * (RNF-08). Los errores se registran sin el contenido del mensaje, que puede incluir un enlace con token.
 */
class AsyncAfterCommitEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(AsyncAfterCommitEmailSender.class);

    private final EmailProvider provider;
    private final TaskExecutor executor;

    AsyncAfterCommitEmailSender(EmailProvider provider, TaskExecutor executor) {
        this.provider = provider;
        this.executor = executor;
    }

    @Override
    public void send(EmailMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(message);
                }
            });
        } else {
            dispatch(message);
        }
    }

    private void dispatch(EmailMessage message) {
        executor.execute(() -> {
            try {
                provider.deliver(message);
            } catch (Exception e) {
                log.error("No se pudo enviar el correo \"{}\": {}", message.subject(), e.getClass().getSimpleName());
            }
        });
    }
}
