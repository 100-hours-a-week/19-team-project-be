package org.refit.refitbackend.global.metrics;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.metrics.aop", name = "enabled", havingValue = "true")
public class RuntimeTimingAspect {

    private final long slowThresholdMs;

    public RuntimeTimingAspect(@Value("${app.metrics.aop.slow-threshold-ms:300}") long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    @Around(
            "("
                    + "execution(* org.refit.refitbackend..service..*(..))"
                    + " || execution(* org.refit.refitbackend..controller..*(..))"
                    + ")"
                    + " && !execution(* org.refit.refitbackend.domain.chat.kafka.ChatMessagePersistenceListener.*(..))"
                    + " && !within(org.refit.refitbackend.global..*)"
    )
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNs = System.nanoTime();
        String method = joinPoint.getSignature().toShortString();
        int argsCount = joinPoint.getArgs() == null ? 0 : joinPoint.getArgs().length;

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            if (elapsedMs >= slowThresholdMs) {
                log.warn("[AOP][SLOW] method={}, elapsedMs={}, argsCount={}, thread={}",
                        method, elapsedMs, argsCount, Thread.currentThread().getName());
            } else {
                log.debug("[AOP] method={}, elapsedMs={}, argsCount={}, thread={}",
                        method, elapsedMs, argsCount, Thread.currentThread().getName());
            }
            return result;
        } catch (Throwable t) {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.error("[AOP][ERROR] method={}, elapsedMs={}, argsCount={}, thread={}, error={}",
                    method, elapsedMs, argsCount, Thread.currentThread().getName(), t.getClass().getSimpleName());
            throw t;
        }
    }
}
