package co.edu.unipamplona.logicode.identity.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un controlador el {@link co.edu.unipamplona.logicode.identity.application.Actor} de la sesión.
 * Si no hay sesión activa, la solicitud termina con 401.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentActor {
}
