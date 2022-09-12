package api.kafkamilestone.exception;

import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.*;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

	//404
	CLUSTER_NOT_FOUND(NOT_FOUND, "Cluster Not Found"),
	TOPIC_NAME_NOT_FOUND(NOT_FOUND, "No matching topic name"),
	
	//406
	SERVERS_URL_INVALID(NOT_ACCEPTABLE, "Registered url is invalid. Modify current url"),
	
	//409
	CLUSTER_DUPLICATE(CONFLICT,"Cluster already exists"),
	CLUSTER_EXECUTION(CONFLICT, "Cluster is interrupted or unexpected execution error occured"),
	NOT_EMPTY_ARGS(CONFLICT, "This Arguments is not blanked"),
	
	TOPIC_NAME_DUPLICATE(CONFLICT, "Duplicated topic name"),
	
	//500
	UNEXPECTED_SERVER_ERROR(INTERNAL_SERVER_ERROR, "unexpected error occured"),
	
	//retry
	REFRESH_PAGE(CONFLICT, "Refresh page");
	
	private final HttpStatus httpStatus;
	private final String message;
}
