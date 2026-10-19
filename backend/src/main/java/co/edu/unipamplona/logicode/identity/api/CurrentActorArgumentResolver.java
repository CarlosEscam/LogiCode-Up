package co.edu.unipamplona.logicode.identity.api;

import java.util.List;

import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Resuelve {@link CurrentActor}: lee el actor que el filtro de seguridad cargó desde la base de datos para esta
 * solicitud (atributo {@code Actor.class.getName()}).
 */
@Configuration(proxyBeanMethods = false)
public class CurrentActorArgumentResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    public static final String ATTRIBUTE = Actor.class.getName();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentActor.class) && parameter.getParameterType() == Actor.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object actor = webRequest.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (actor instanceof Actor resolved) {
            return resolved;
        }
        throw ApplicationException.unauthenticated("Debes iniciar sesión.");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }
}
