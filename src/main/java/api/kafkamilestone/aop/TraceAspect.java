package api.kafkamilestone.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Aspect
public class TraceAspect {
	
    @Before("@annotation(api.kafkamilestone.aop.Trace)")
    public void doTrace(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs();
        log.info("[method trace] {} | [args] = {}", joinPoint.getSignature(), args);
    }
    
    @AfterReturning(pointcut = "@annotation(api.kafkamilestone.aop.Trace)", returning = "results")
    public void doTraceResult(JoinPoint joinPoint, ResponseEntity<?> results) {
    	log.info("[method trace] {} | [result] = {}", joinPoint.getSignature(), results);
    }

}
