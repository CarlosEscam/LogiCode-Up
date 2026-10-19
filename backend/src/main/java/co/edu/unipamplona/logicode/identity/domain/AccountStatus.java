package co.edu.unipamplona.logicode.identity.domain;

public enum AccountStatus {
    /** Cuenta docente solicitada, sin permisos hasta que un administrador la apruebe (RN-02). */
    PENDING_APPROVAL,
    ACTIVE,
    DISABLED
}
