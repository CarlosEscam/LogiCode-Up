package co.edu.unipamplona.logicode.identity.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un controlador el {@link Actor} de la sesión. Vive junto a {@link Actor} para que los controladores de
 * todos los módulos lo usen sin depender de la capa API de identity.
 * <p>
 * Con {@code required = true} (por defecto), si no hay sesión activa la solicitud termina con 401. Con
 * {@code required = false} el parámetro llega en {@code null} para un visitante.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentActor {

    boolean required() default true;
}
