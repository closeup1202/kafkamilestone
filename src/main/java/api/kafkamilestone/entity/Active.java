package api.kafkamilestone.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 생성할 cluster에 대한 사용 환경
 * 
 * 	DEV     : 개발 서버
 * 	QA      : 테스팅
 * 	LOCAL   : 로컬 개발
 * 	STAGING : 운영 환경으로 이관 전 비기능적인 부분 (검증, 성능, 장애 등)을 체크
 */
public enum Active {
	
	@JsonProperty("dev")
	DEV,
	
	@JsonProperty("qa")
	QA,
	
	@JsonProperty("local")
	LOCAL,
	
	@JsonProperty("staging")
	STAGING;
	
}
