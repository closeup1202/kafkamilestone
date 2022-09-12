package api.kafkamilestone.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter 
@Setter 
@ToString
@Builder
public class ResponseDTO<T> {
	private T data;
	private List<T> datas;
}
