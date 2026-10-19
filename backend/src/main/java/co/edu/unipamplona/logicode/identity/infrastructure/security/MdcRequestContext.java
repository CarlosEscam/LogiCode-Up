package co.edu.unipamplona.logicode.identity.infrastructure.security;

import co.edu.unipamplona.logicode.identity.application.RequestContext;
import co.edu.unipamplona.logicode.shared.infrastructure.web.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
class MdcRequestContext implements RequestContext {

    @Override
    public String requestId() {
        return MDC.get(RequestIdFilter.MDC_KEY);
    }
}
