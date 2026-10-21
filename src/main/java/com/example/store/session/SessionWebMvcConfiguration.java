package com.example.store.session;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SessionWebMvcConfiguration implements WebMvcConfigurer {

    private final SessionService sessionService;

    public SessionWebMvcConfiguration(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticatedSessionArgumentResolver(sessionService));
    }
}
