package api.kafkamilestone.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvalidUrlException extends RuntimeException{

	/**
	 * 카프카 서버 생성 시, 유효하지 않은 브로커 주소일 경우 발생하는 예외
	 */
	private static final long serialVersionUID = -843831002457337796L;
	private final ErrorCode errorCode;
	private final String servers;
	
}
