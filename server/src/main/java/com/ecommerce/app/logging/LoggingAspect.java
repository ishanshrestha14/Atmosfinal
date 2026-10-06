package com.ecommerce.app.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Audit trail of controller and service calls: which operation ran, how long it took and whether
 * it failed. Arguments and results are deliberately not logged, since they carry personal data
 * (emails, addresses) and serialising them on every call is expensive.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@within(com.ecommerce.app.logging.LoggingController) || @annotation(com.ecommerce.app.logging.LoggingController)")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        return audit(joinPoint, "Controller");
    }

    @Around("@within(com.ecommerce.app.logging.LoggingService) || @annotation(com.ecommerce.app.logging.LoggingService)")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        return audit(joinPoint, "Service");
    }

    private Object audit(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String operation = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            logger.info("[{}] {} completed in {} ms", layer, operation, elapsedMillis(start));
            return result;
        } catch (Throwable failure) {
            logger.warn("[{}] {} failed after {} ms: {}", layer, operation, elapsedMillis(start), failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
