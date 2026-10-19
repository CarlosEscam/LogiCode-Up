package co.edu.unipamplona.logicode.identity.infrastructure.mail;

import co.edu.unipamplona.logicode.identity.application.EmailMessage;

/** Proveedor concreto que entrega el correo (Brevo o buzón local de desarrollo). */
interface EmailProvider {

    void deliver(EmailMessage message) throws Exception;
}
