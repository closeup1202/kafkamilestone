package api.kafkamilestone.dto.broker;

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
public class BrokersDTO {
	
	private String id;
	private String host;
	private int port;
	private double bytesIn;
	private double bytesOut;

}
