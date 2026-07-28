package com.ecotrack.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Measures and logs execution time for methods annotated with {@link TrackExecutionTime}.
 */
@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    @Around("@annotation(com.ecotrack.aspect.TrackExecutionTime)")
    public Object trackExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        log.info("⏱️  {}.{}() executed in {}ms", className, methodName, duration);
        if (duration > 1000) {
            log.warn("⚠️  SLOW METHOD: {}.{}() took {}ms (threshold: 1000ms)",
                    className, methodName, duration);
        }
        return result;
    }
}
