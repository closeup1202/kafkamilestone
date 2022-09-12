package api.kafkamilestone.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MilestoneException extends RuntimeException{
	
	/**
	 * 전역에서 사용할 커스텀 이벤트 
	 * RuntimeException을 받아서 체크되지 않은 예외들을 처리
	 * 필드로 받아온 Enum 클래스를 통해 디테일한 예외 처리
	 */
	
	private static final long serialVersionUID = 4072659556394546154L;
	private final ErrorCode errorCode;
}
