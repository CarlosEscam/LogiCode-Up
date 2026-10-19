package co.edu.unipamplona.logicode.identity.application;

/**
 * Puerto de envío de correo transaccional (Brevo en producción local, buzón de archivos en desarrollo).
 * El envío ocurre después de confirmar la transacción y en segundo plano: el caso de uso no espera.
 */
public interface EmailSender {

    void send(EmailMessage message);
}
