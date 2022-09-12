package api.kafkamilestone.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Aspect
public class RetryAspect {
	
    @Around("@annotation(retry)") 
    public Object doRetry(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable{
        log.info("[retry] {} | [args] = {}", joinPoint.getSignature(), retry);

        int maxRetry = retry.value();
        Exception exceptionHolder = new Exception();

        for(int retryCount = 1; retryCount <= maxRetry; retryCount++){

            try {
                log.info("[retry] try count= {}/{}", retryCount, maxRetry);
                return joinPoint.proceed();
            } catch (Exception e){
                exceptionHolder = e;
            }
        }

        throw exceptionHolder;
    }
}
