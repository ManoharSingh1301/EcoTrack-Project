package com.ecotrack.communication.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.ecotrack.communication.service..*(..))")
    public void serviceLayer() {}

    @Pointcut("execution(* com.ecotrack.communication.controller..*(..))")
    public void controllerLayer() {}

    @Around("serviceLayer()")
    public Object logServiceMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("➡️  {}.{}() called with args: {}", className, methodName, Arrays.toString(args));
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ {}.{}() completed in {}ms", className, methodName, duration);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ {}.{}() failed after {}ms with exception: {}",
                    className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    @Before("controllerLayer()")
    public void logControllerEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("🌐 API Request: {}", methodName);
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logServiceException(JoinPoint joinPoint, Exception ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.error("🔥 Exception in {}.{}(): {} - {}",
                className, methodName, ex.getClass().getSimpleName(), ex.getMessage());
    }
}
