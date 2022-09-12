package api.kafkamilestone.dto.consumer;

import java.util.OptionalLong;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumerViewDTO {
	
	private String partition;
	private Long consumerOffset;
	private OptionalLong lag;
	private String consumerInstanceOwner;
	
}
