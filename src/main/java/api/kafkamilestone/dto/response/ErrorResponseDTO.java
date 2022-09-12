package api.kafkamilestone.dto.response;

import org.springframework.http.ResponseEntity;

import api.kafkamilestone.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponseDTO {
	
	private final String error;
	private final String message;
	private final String servers;
	
	public static ResponseEntity<ErrorResponseDTO> toErrorResponse(ErrorCode errorCode){
		return ResponseEntity
				.status(errorCode.getHttpStatus())
				.body(ErrorResponseDTO.builder()
						.error(errorCode.getHttpStatus().name())
						.message(errorCode.getMessage())
						.build()
				);
	}
	
	public static ResponseEntity<ErrorResponseDTO> toErrorWithServerResponse(ErrorCode errorCode, String servers){
		return ResponseEntity
				.status(errorCode.getHttpStatus())
				.body(ErrorResponseDTO.builder()
						.error(errorCode.getHttpStatus().name())
						.message(errorCode.getMessage())
						.servers(servers)
						.build()
				);
	}
}
