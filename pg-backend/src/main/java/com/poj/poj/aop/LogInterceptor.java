package com.poj.poj.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class LogInterceptor {
    @Around("execution(* com.poj.poj.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String path = attributes == null ? point.getSignature().getName() : attributes.getRequest().getRequestURI();
        String id = java.util.UUID.randomUUID().toString();
        if (attributes != null) attributes.getResponse().setHeader("X-Request-ID", id);
        long start = System.nanoTime();
        try { return point.proceed(); }
        finally { log.info("request id={} path={} durationMs={}", id, path, (System.nanoTime() - start) / 1000000); }
    }
}

