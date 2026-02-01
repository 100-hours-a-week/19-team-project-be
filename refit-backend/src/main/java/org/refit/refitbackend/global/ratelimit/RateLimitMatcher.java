package org.refit.refitbackend.global.ratelimit;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class RateLimitMatcher {

    private final AntPathMatcher matcher = new AntPathMatcher();

    public boolean matches(RateLimitRule rule, String method, String path) {
        if (rule.method() != HttpMethod.valueOf(method)) {
            return false;
        }
        return matcher.match(rule.pattern(), path);
    }
}
