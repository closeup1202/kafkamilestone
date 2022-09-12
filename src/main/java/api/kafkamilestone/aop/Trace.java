package api.kafkamilestone.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * global log trace AOP
 * 메소드 단위로 설정 (다른 AOP 프레임 워크와 달리 Spring에서는 메소드 JoinPoint만 제공) {@link https://jojoldu.tistory.com/71}
 * 주로 프론트단이랑 데이터를 주고 받는 컨트롤 단에 적용	
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trace {

}
