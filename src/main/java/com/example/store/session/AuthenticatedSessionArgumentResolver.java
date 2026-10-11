package com.example.store.session;

import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AuthenticatedSessionArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String SESSION_HEADER = "X-Session-Id";

    private final SessionService sessionService;

    public AuthenticatedSessionArgumentResolver(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedSession.class)
                && UserSession.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer modelAndViewContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String sessionId = webRequest.getHeader(SESSION_HEADER);
        if (!StringUtils.hasText(sessionId)) {
            throw InvalidSessionException.missingHeader(SESSION_HEADER);
        }
        return sessionService.requireActiveSession(sessionId);
    }
}
