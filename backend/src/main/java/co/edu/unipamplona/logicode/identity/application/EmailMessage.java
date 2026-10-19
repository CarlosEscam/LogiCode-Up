package co.edu.unipamplona.logicode.identity.application;

/**
 * Correo transaccional. El cuerpo puede contener un enlace con token: nunca debe escribirse en logs.
 */
public record EmailMessage(String toAddress, String toName, String subject, String textBody, String htmlBody) {

    @Override
    public String toString() {
        return "EmailMessage[to=" + toAddress + ", subject=" + subject + "]";
    }
}
