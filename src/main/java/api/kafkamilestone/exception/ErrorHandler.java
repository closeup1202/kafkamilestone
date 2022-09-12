package api.kafkamilestone.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import api.kafkamilestone.dto.response.ErrorResponseDTO;

@RestControllerAdvice
public class ErrorHandler {

	@ExceptionHandler(value = {MilestoneException.class})
	public ResponseEntity<ErrorResponseDTO> errorHandler(MilestoneException e) {
		return ErrorResponseDTO.toErrorResponse(e.getErrorCode());
	}
	
	@ExceptionHandler(value = {InvalidUrlException.class})
	public ResponseEntity<ErrorResponseDTO> errorHandler(InvalidUrlException e) {
		return ErrorResponseDTO.toErrorWithServerResponse(e.getErrorCode(), e.getServers());
	}
}
