package api.kafkamilestone.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 카프카 cluster (AdminClient create) 구성 오류 시 재시도하는 AOP
 * 메소드 단위로 설정
 * {@value} 재시도 횟수 기본값 : 3
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
	int value() default 3;
}
